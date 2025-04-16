/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.github.weisj.jsvg.paint.impl.jdk;

import java.awt.*;
import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR
 * REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License version 2 only, as published by the Free Software Foundation. Oracle
 * designates this particular file as subject to the "Classpath" exception as provided by Oracle in
 * the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com
 * if you need additional information or have any questions.
 */

/**
 * Provides the actual implementation for the RadialGradientPaint.
 * This is where the pixel processing is done.  A RadialGradientPaint
 * only supports circular gradients, but it should be possible to scale
 * the circle to look approximately elliptical, by means of a
 * gradient transform passed into the RadialGradientPaint constructor.
 *
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 */
final class SVGRadialGradientPaintContext extends SVGMultipleGradientPaintContext {

    /** True when (focus == center) and (focus radius == 0).  */
    private final boolean isSimpleFocus;

    /** True when (cycleMethod == NO_CYCLE). */
    private final boolean isNonCyclic;

    /** Variables representing center and focus points. */
    private final float centerX;
    private final float centerY;
    private float focusX;
    private float focusY;

    /** Radius of the gradient circle an focus circle squared. */
    private final float radiusSq;
    private final float focusRadius;
    private final float focusRadiusSq;

    /** Constant part of X, Y user space coordinates. */
    private final float constA;
    private final float constB;

    /** Constant second order delta for simple loop. */
    private final float gDeltaDelta;

    /**
     * This value represents the solution when focusX == X.  It is called
     * trivial because it is easier to calculate than the general case.
     */
    private final float trivial;

    /** Amount for offset when clamping focus. */
    private static final float FOCUS_CLAMP_DOWNSCALE = .99f;

    /**
     * Constructor for RadialGradientPaintContext.
     *
     * @param paint the {@code RadialGradientPaint} from which this context
     *              is created
     * @param t the {@code AffineTransform} from user
     *          space into device space (gradientTransform should be
     *          concatenated with this)
     * @param cx the center X coordinate in user space of the circle defining
     *           the gradient.  The last color of the gradient is mapped to
     *           the perimeter of this circle.
     * @param cy the center Y coordinate in user space of the circle defining
     *           the gradient.  The last color of the gradient is mapped to
     *           the perimeter of this circle.
     * @param r the radius of the circle defining the extents of the
     *          color gradient
     * @param fx the X coordinate in user space to which the first color
     *           is mapped
     * @param fy the Y coordinate in user space to which the first color
     *           is mapped
     * @param fractions the fractions specifying the gradient distribution
     * @param colors the gradient colors
     * @param cycleMethod either NO_CYCLE, REFLECT, or REPEAT
     * @param colorSpace which colorspace to use for interpolation,
     *                   either SRGB or LINEAR_RGB
     */
    SVGRadialGradientPaintContext(@NotNull SVGRadialGradientPaint paint, @NotNull AffineTransform t,
            float cx, float cy, float r,
            float fx, float fy, float fr,
            float @NotNull [] fractions, @NotNull Color @NotNull [] colors,
            MultipleGradientPaint.CycleMethod cycleMethod, MultipleGradientPaint.ColorSpaceType colorSpace) {
        super(paint, t, fractions, colors, cycleMethod, colorSpace);

        // copy some parameters
        centerX = cx;
        centerY = cy;
        focusX = fx;
        focusY = fy;
        /* Radius of the outermost circle defining the 100% gradient stop. */

        this.isSimpleFocus = (focusX == centerX) && (focusY == centerY) && (fr == 0);
        this.isNonCyclic = (cycleMethod == MultipleGradientPaint.CycleMethod.NO_CYCLE);

        // for use in the quadratic equation
        radiusSq = r * r;
        focusRadius = fr;
        focusRadiusSq = fr * fr;

        float dX = focusX - centerX;
        float dY = focusY - centerY;

        double distSq = (dX * dX) + (dY * dY);

        // test if distance from focus to center is greater than the radius
        if (distSq > radiusSq * FOCUS_CLAMP_DOWNSCALE) {
            // clamp focus to radius
            float scale = (float) Math.sqrt(radiusSq * FOCUS_CLAMP_DOWNSCALE / distSq);
            dX = dX * scale;
            dY = dY * scale;
            focusX = centerX + dX;
            focusY = centerY + dY;
        }

        // calculate the solution to be used in the case where X == focusX
        // in cyclicCircularGradientFillRaster()
        trivial = (float) Math.sqrt(radiusSq - (dX * dX));

        // constant parts of X, Y user space coordinates
        constA = a02 - centerX;
        constB = a12 - centerY;

        // constant second order delta for simple loop
        gDeltaDelta = 2 * (a00 * a00 + a10 * a10) / radiusSq;
    }

    /**
     * Return a Raster containing the colors generated for the graphics
     * operation.
     *
     * @param x,y,w,h the area in device space for which colors are
     * generated.
     */
    @Override
    protected void fillRaster(int[] pixels, int off, int adjust,
            int x, int y, int w, int h) {
        if (isSimpleFocus && isNonCyclic && isSimpleLookup) {
            simpleNonCyclicFillRaster(pixels, off, adjust, x, y, w, h);
        } else {
            cyclicCircularGradientFillRaster(pixels, off, adjust, x, y, w, h);
        }
    }

    /**
     * This code works in the simplest of cases, where the focus == center
     * point, the gradient is non-cyclic, and the gradient lookup method is
     * fast (single array index, no conversion necessary).
     */
    private void simpleNonCyclicFillRaster(int[] pixels, int off, int adjust, int x, int y, int w, int h) {
        /*
         * We calculate sqrt(X^2 + Y^2) relative to the radius size to get the fraction for the color to
         * use.
         *
         * Each step along the scanline adds (a00, a10) to (X, Y). If we precalculate: gRel = X^2+Y^2 for
         * the start of the row, then for each step we need to calculate: gRel' = (X+a00)^2 + (Y+a10)^2 =
         * X^2 + 2*X*a00 + a00^2 + Y^2 + 2*Y*a10 + a10^2 = (X^2+Y^2) + 2*(X*a00+Y*a10) + (a00^2+a10^2) =
         * gRel + 2*(X*a00+Y*a10) + (a00^2+a10^2) = gRel + 2*DP + SD (where DP = dot product between X,Y and
         * a00,a10 and SD = dot product square of the delta vector) For the step after that we get: gRel'' =
         * (X+2*a00)^2 + (Y+2*a10)^2 = X^2 + 4*X*a00 + 4*a00^2 + Y^2 + 4*Y*a10 + 4*a10^2 = (X^2+Y^2) +
         * 4*(X*a00+Y*a10) + 4*(a00^2+a10^2) = gRel + 4*DP + 4*SD = gRel' + 2*DP + 3*SD The increment
         * changed by: (gRel'' - gRel') - (gRel' - gRel) = (2*DP + 3*SD) - (2*DP + SD) = 2*SD Note that this
         * value depends only on the (inverse of the) transformation matrix and so is a constant for the
         * loop. To make this all relative to the unit circle, we need to divide all values as follows: [XY]
         * /= radius gRel /= radiusSq DP /= radiusSq SD /= radiusSq
         */
        // coordinates of UL corner in "user space" relative to center
        float rowX = (a00 * x) + (a01 * y) + constA;
        float rowY = (a10 * x) + (a11 * y) + constB;

        // second order delta calculated in constructor
        float deltaDelta = this.gDeltaDelta;

        // adjust is (scan-w) of pixels array, we need (scan)
        adjust += w;

        // rgb of the 1.0 color used when the distance exceeds gradient radius
        int rgbclip = gradient[fastGradientArraySize];

        for (int j = 0; j < h; j++) {
            // these values depend on the coordinates of the start of the row
            float gRel = (rowX * rowX + rowY * rowY) / radiusSq;
            float gDelta = (2 * (a00 * rowX + a10 * rowY) / radiusSq +
                    deltaDelta / 2);

            /*
             * Use optimized loops for any cases where gRel >= 1. We do not need to calculate sqrt(gRel) for
             * these values since sqrt(N>=1) == (M>=1). Note that gRel follows a parabola which can only be < 1
             * for a small region around the center on each scanline. In particular: gDeltaDelta is always
             * positive gDelta is <0 until it crosses the midpoint, then >0 To the left and right of that
             * region, it will always be >=1 out to infinity, so we can process the line in 3 regions: out to
             * the left - quick fill until gRel < 1, updating gRel in the heart - slow fraction=sqrt fill while
             * gRel < 1 out to the right - quick fill rest of scanline, ignore gRel
             */
            int i = 0;
            // Quick fill for "out to the left"
            while (i < w && gRel >= 1.0f) {
                pixels[off + i] = rgbclip;
                gRel += gDelta;
                gDelta += deltaDelta;
                i++;
            }
            // Slow fill for "in the heart"
            while (i < w && gRel < 1.0f) {
                int gIndex;

                if (gRel <= 0) {
                    gIndex = 0;
                } else {
                    float fIndex = gRel * SQRT_LUT_SIZE;
                    int iIndex = (int) fIndex;
                    float s0 = sqrtLookup[iIndex];
                    float s1 = sqrtLookup[iIndex + 1] - s0;
                    fIndex = s0 + (fIndex - iIndex) * s1;
                    gIndex = (int) (fIndex * fastGradientArraySize);
                }

                // store the color at this point
                pixels[off + i] = gradient[gIndex];

                // incremental calculation
                gRel += gDelta;
                gDelta += deltaDelta;
                i++;
            }
            // Quick fill to end of line for "out to the right"
            while (i < w) {
                pixels[off + i] = rgbclip;
                i++;
            }

            off += adjust;
            rowX += a01;
            rowY += a11;
        }
    }

    // SQRT_LUT_SIZE must be a power of 2 for the test above to work.
    private static final int SQRT_LUT_SIZE = (1 << 11);
    private static final float[] sqrtLookup = new float[SQRT_LUT_SIZE + 1];
    static {
        for (int i = 0; i < sqrtLookup.length; i++) {
            sqrtLookup[i] = (float) Math.sqrt(i / ((float) SQRT_LUT_SIZE));
        }
    }

    /**
     * Fill the raster, cycling the gradient colors when a point falls outside
     * the perimeter of the 100% stop circle.
     * <p>
     * This calculation first computes the intersection point of the line
     * from the focus through the current point in the raster, and the
     * perimeter of the gradient circle.
     * <p>
     * Then it determines the percentage distance of the current point along
     * that line (focus is 0%, perimeter is 100%).
     * <p>
     * Equation of a circle centered at (a,b) with radius r:
     *     (x-a)^2 + (y-b)^2 = r^2
     * Equation of a line with slope m and y-intercept b:
     *     y = mx + b
     * Replacing y in the circle equation and solving using the quadratic
     * formula produces the following set of equations.  Constant factors have
     * been extracted out of the inner loop.
     */
    private void cyclicCircularGradientFillRaster(int[] pixels, int off, int adjust, int x, int y, int w, int h) {
        // constant part of the C factor of the quadratic equation
        final double constC = -radiusSq + (centerX * centerX) + (centerY * centerY);

        // constant parts of X, Y coordinates
        final float constX = (a00 * x) + (a01 * y) + a02;
        final float constY = (a10 * x) + (a11 * y) + a12;

        // constants in inner loop quadratic formula
        final float precalc2 = 2 * centerY;
        final float precalc3 = -2 * centerX;

        // used to index pixels array
        int indexer = off;

        // incremental index change for pixels array
        final int pixInc = w + adjust;

        // for every row
        for (int j = 0; j < h; j++) {

            // user space point; these are constant from column to column
            float userX = (a01 * j) + constX;
            float userY = (a11 * j) + constY;

            // for every column (inner loop begins here)
            for (int i = 0; i < w; i++) {

                // intersection with circle X,Y coordinate
                double solutionX;
                double solutionY;
                if (userX == focusX) {
                    // special case to avoid divide by zero
                    solutionX = focusX;
                    solutionY = centerY;
                    solutionY += (userY > focusY) ? trivial : -trivial;
                } else {
                    // slope and y-intercept of the focus-perimeter line
                    float slope = (userY - focusY) / (userX - focusX);
                    float yIntercept = userY - (slope * userX);

                    // use the quadratic formula to calculate the
                    // intersection point
                    double a = (slope * slope) + 1;
                    double b = precalc3 + (-2 * slope * (centerY - yIntercept));
                    double c = constC + (yIntercept * (yIntercept - precalc2));

                    // determinant of quadratic formula (should always be > 0)
                    float det = (float) Math.sqrt((b * b) - (4 * a * c));
                    solutionX = -b;

                    // choose the positive or negative root depending
                    // on where the X coord lies with respect to the focus
                    solutionX += (userX < focusX) ? -det : det;
                    solutionX = solutionX / (2 * a); // divisor
                    solutionY = (slope * solutionX) + yIntercept;
                }

                int colorAtPoint = getColorAtPoint(userX, userY, (float) solutionX, (float) solutionY);

                // store the color at this point
                pixels[indexer + i] = colorAtPoint;

                // incremental change in X, Y
                userX += a00;
                userY += a10;
            } // end inner loop

            indexer += pixInc;
        } // end outer loop
    }

    private int getColorAtPoint(float userX, float userY, float solutionX, float solutionY) {
        float currentToFocusSq = getCurrentToFocusSq(userX, userY);
        if (currentToFocusSq <= focusRadiusSq) {
            // If we are inside the focus circle use the first color stop.
            return indexIntoGradientsArrays(0);
        } else {
            return getColorAtPointOutsideFocusCircle(solutionX, solutionY, currentToFocusSq);
        }
    }

    private float getCurrentToFocusSq(float x, float y) {
        float deltaXSq = x - focusX;
        deltaXSq = deltaXSq * deltaXSq;

        float deltaYSq = y - focusY;
        deltaYSq = deltaYSq * deltaYSq;

        // sq distance from the current point to focus
        return deltaXSq + deltaYSq;
    }

    private int getColorAtPointOutsideFocusCircle(float solutionX, float solutionY, float currentToFocusSq) {
        // Otherwise remap the interval (0-1) which usually span from the focus to the circle boundary
        // to the line from the outside of the focus circle to the outside.
        float intersectToFocusSq = getCurrentToFocusSq(solutionX, solutionY);

        float gradientPosition = computeGradientPosition(currentToFocusSq, intersectToFocusSq);
        return indexIntoGradientsArrays(gradientPosition);
    }

    private float computeGradientPosition(float currentToFocusSq, float intersectToFocusSq) {
        // get the percentage (0-1) of the current point along the focus-circumference line
        // Adjust for the focus radius
        float gradientPosition;
        if (focusRadius > 0) {
            float currentToFocus = (float) Math.sqrt(currentToFocusSq);
            float intersectToFocus = (float) Math.sqrt(intersectToFocusSq);
            gradientPosition = (currentToFocus - focusRadius) / (intersectToFocus - focusRadius);
        } else {
            gradientPosition = (float) Math.sqrt(currentToFocusSq / intersectToFocusSq);
        }
        return gradientPosition;
    }
}
