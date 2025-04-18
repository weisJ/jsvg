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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.util.ColorUtil;

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
 * You should have received a copy of the GNU General Public License version 2 along with this work
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com
 * if you need additional information or have any questions.
 */

/**
 * This is the superclass for all PaintContexts which use a multiple color
 * gradient to fill in their raster.  It provides the actual color
 * interpolation functionality.  Subclasses only have to deal with using
 * the gradient to fill pixels in a raster.
 *
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 */
abstract class SVGMultipleGradientPaintContext implements PaintContext {

    private static final float MIN_INTERVAL_LENGTH = 0.001f;

    /**
     * The PaintContext's ColorModel.  This is ARGB if colors are not all
     * opaque, otherwise it is RGB.
     */
    protected ColorModel model;

    /**
     * Color model used if gradient colors are all opaque.
     */
    private static final ColorModel XRGB_MODEL =
            new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);

    /**
     * The cached ColorModel.
     */
    protected static ColorModel cachedModel;

    /**
     * The cached raster, which is reusable among instances.
     */
    protected static WeakReference<Raster> cached;

    /**
     * Raster is reused whenever possible.
     */
    protected Raster saved;

    /**
     * The method to use when painting out of the gradient bounds.
     */
    protected MultipleGradientPaint.CycleMethod cycleMethod;

    /**
     * The ColorSpace in which to perform the interpolation
     */
    protected MultipleGradientPaint.ColorSpaceType colorSpace;

    /**
     * Elements of the inverse transform matrix.
     */
    protected float a00;
    protected float a01;
    protected float a10;
    protected float a11;
    protected float a02;
    protected float a12;

    /**
     * This boolean specifies whether we are in simple lookup mode, where an
     * input value between 0 and 1 may be used to directly index into a single
     * array of gradient colors.  If this boolean value is false, then we have
     * to use a 2-step process where we have to determine which gradient array
     * we fall into, then determine the index into that array.
     */
    protected boolean isSimpleLookup;

    /**
     * Size of gradients array for scaling the 0-1 index when looking up
     * colors the fast way.
     */
    protected int fastGradientArraySize;

    /**
     * Array which contains the interpolated color values for each interval,
     * used by calculateSingleArrayGradient().  It is protected for possible
     * direct access by subclasses.
     */
    protected int[] gradient;

    /**
     * Array of gradient arrays, one array for each interval.  Used by
     * calculateMultipleArrayGradient().
     */
    private int[][] gradients;

    /**
     * Normalized intervals array.
     */
    private float[] normalizedIntervals;

    /**
     * Fractions array.
     */
    private final float[] fractions;

    /**
     * Used to determine if gradient colors are all opaque.
     */
    private int transparencyTest;

    /**
     * Constant number of max colors between any 2 arbitrary colors.
     * Used for creating and indexing gradients arrays.
     */
    protected static final int GRADIENT_SIZE = 256;
    protected static final int GRADIENT_SIZE_INDEX = GRADIENT_SIZE - 1;

    /**
     * Maximum length of the fast single-array.  If the estimated array size
     * is greater than this, switch over to the slow lookup method.
     * No particular reason for choosing this number, but it seems to provide
     * satisfactory performance for the common case (fast lookup).
     */
    private static final int MAX_GRADIENT_ARRAY_SIZE = 5000;

    /**
     * Constructor for MultipleGradientPaintContext superclass.
     */
    protected SVGMultipleGradientPaintContext(@NotNull SVGMultipleGradientPaint mgp,
            @NotNull AffineTransform t, float @NotNull [] fractions, @NotNull Color @NotNull [] colors,
            MultipleGradientPaint.CycleMethod cycleMethod, MultipleGradientPaint.ColorSpaceType colorSpace) {
        // The inverse transform is needed to go from device to user space.
        // Get all the components of the inverse transform matrix.
        AffineTransform tInv;
        try {
            // the following assumes that the caller has copied the incoming
            // transform and is not concerned about it being modified
            t.invert();
            tInv = t;
        } catch (NoninvertibleTransformException e) {
            // just use identity transform in this case; better to show
            // (incorrect) results than to throw an exception and/or no-op
            tInv = new AffineTransform();
        }
        double[] m = new double[6];
        tInv.getMatrix(m);
        a00 = (float) m[0];
        a10 = (float) m[1];
        a01 = (float) m[2];
        a11 = (float) m[3];
        a02 = (float) m[4];
        a12 = (float) m[5];

        // copy some flags
        this.cycleMethod = cycleMethod;
        this.colorSpace = colorSpace;

        // we can avoid copying this array since we do not modify its values
        this.fractions = fractions;

        // note that only one of these values can ever be non-null (we either
        // store the fast gradient array or the slow one, but never both
        // at the same time)
        int[] grad =
                (mgp.gradient != null) ? mgp.gradient.get() : null;
        int[][] grads =
                (mgp.gradients != null) ? mgp.gradients.get() : null;

        if (grad == null && grads == null) {
            // we need to (re)create the appropriate values
            calculateLookupData(colors);

            // now cache the calculated values in the
            // MultipleGradientPaint instance for future use
            mgp.model = this.model;
            mgp.normalizedIntervals = this.normalizedIntervals;
            mgp.isSimpleLookup = this.isSimpleLookup;
            if (isSimpleLookup) {
                // only cache the fast array
                mgp.fastGradientArraySize = this.fastGradientArraySize;
                mgp.gradient = new SoftReference<>(this.gradient);
            } else {
                // only cache the slow array
                mgp.gradients = new SoftReference<>(this.gradients);
            }
        } else {
            // use the values cached in the MultipleGradientPaint instance
            this.model = mgp.model;
            this.normalizedIntervals = mgp.normalizedIntervals;
            this.isSimpleLookup = mgp.isSimpleLookup;
            this.gradient = grad;
            this.fastGradientArraySize = mgp.fastGradientArraySize;
            this.gradients = grads;
        }
    }

    /**
     * This function is the meat of this class.  It calculates an array of
     * gradient colors based on an array of fractions and color values at
     * those fractions.
     */
    private void calculateLookupData(Color[] colors) {
        Color[] normalizedColors;
        if (colorSpace == MultipleGradientPaint.ColorSpaceType.LINEAR_RGB) {
            // create a new colors array
            normalizedColors = new Color[colors.length];
            // convert the colors using the lookup table
            for (int i = 0; i < colors.length; i++) {
                int argb = colors[i].getRGB();
                normalizedColors[i] = new Color(ColorUtil.sRGBtoLinearRGB(argb), true);
            }
        } else {
            // we can just use this array by reference since we do not
            // modify its values in the case of SRGB
            normalizedColors = colors;
        }

        // this will store the intervals (distances) between gradient stops
        normalizedIntervals = new float[fractions.length - 1];

        // convert from fractions into intervals
        for (int i = 0; i < normalizedIntervals.length; i++) {
            // interval distance is equal to the difference in positions
            normalizedIntervals[i] = this.fractions[i + 1] - this.fractions[i];
        }

        // initialize to be fully opaque for ANDing with colors
        transparencyTest = 0xff000000;

        // array of interpolation arrays
        gradients = new int[normalizedIntervals.length][];

        // find smallest interval
        float minInterval = 1;
        for (float interval : normalizedIntervals) {
            if (interval > MIN_INTERVAL_LENGTH) {
                minInterval = Math.min(minInterval, interval);
            }
        }

        // Estimate the size of the entire gradients array.
        // This is to prevent a tiny interval from causing the size of array
        // to explode. If the estimated size is too large, break to using
        // separate arrays for each interval, and using an indexing scheme at
        // look-up time.
        int estimatedSize = 0;
        for (float normalizedInterval : normalizedIntervals) {
            estimatedSize += (int) ((normalizedInterval / minInterval) * GRADIENT_SIZE);
        }

        if (estimatedSize > MAX_GRADIENT_ARRAY_SIZE) {
            // slow method
            calculateMultipleArrayGradient(normalizedColors);
        } else {
            // fast method
            calculateSingleArrayGradient(normalizedColors, minInterval);
        }

        // use the most "economical" model
        if ((transparencyTest >>> 24) == 0xff) {
            model = XRGB_MODEL;
        } else {
            model = ColorModel.getRGBdefault();
        }
    }

    /**
     * FAST LOOKUP METHOD
     * <p>
     * This method calculates the gradient color values and places them in a
     * single int array, gradient[].  It does this by allocating space for
     * each interval based on its size relative to the smallest interval in
     * the array.  The smallest interval is allocated 255 interpolated values
     * (the maximum number of unique in-between colors in a 24 bit color
     * system), and all other intervals are allocated
     * size = (255 * the ratio of their size to the smallest interval).
     * <p>
     * This scheme expedites a speedy retrieval because the colors are
     * distributed along the array according to their user-specified
     * distribution.  All that is needed is a relative index from 0 to 1.
     * <p>
     * The only problem with this method is that the possibility exists for
     * the array size to balloon in the case where there is a
     * disproportionately small gradient interval.  In this case the other
     * intervals will be allocated huge space, but much of that data is
     * redundant.  We thus need to use the space conserving scheme below.
     *
     * @param minInterval the size of the smallest interval
     */
    private void calculateSingleArrayGradient(Color[] colors, float minInterval) {
        // set the flag, so we know later it is a simple (fast) lookup
        isSimpleLookup = true;

        // 2 colors to interpolate
        int rgb1;
        int rgb2;

        // the eventual size of the single array
        int gradientsTot = 1;

        // for every interval (transition between 2 colors)
        for (int i = 0; i < gradients.length; i++) {
            // create an array whose size is based on the ratio to the
            // smallest interval
            int nGradients = (int) ((normalizedIntervals[i] / minInterval) * 255f);
            gradientsTot += nGradients;
            gradients[i] = new int[nGradients];

            // the 2 colors (keyframes) to interpolate between
            rgb1 = colors[i].getRGB();
            rgb2 = colors[i + 1].getRGB();

            // fill this array with the colors in between rgb1 and rgb2
            interpolate(rgb1, rgb2, gradients[i]);

            // if the colors are opaque, transparency should still
            // be 0xff000000
            transparencyTest &= rgb1;
            transparencyTest &= rgb2;
        }

        // put all gradients in a single array
        gradient = new int[gradientsTot];
        int curOffset = 0;
        for (int[] ints : gradients) {
            System.arraycopy(ints, 0, gradient, curOffset, ints.length);
            curOffset += ints.length;
        }
        gradient[gradient.length - 1] = colors[colors.length - 1].getRGB();

        // if interpolation occurred in Linear RGB space, convert the
        // gradients back to sRGB using the lookup table
        if (colorSpace == MultipleGradientPaint.ColorSpaceType.LINEAR_RGB) {
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] = ColorUtil.linearRGBtoSRGB(gradient[i]);
            }
        }

        fastGradientArraySize = gradient.length - 1;
    }

    /**
     * SLOW LOOKUP METHOD
     * <p>
     * This method calculates the gradient color values for each interval and
     * places each into its own 255 size array.  The arrays are stored in
     * gradients[][].  (255 is used because this is the maximum number of
     * unique colors between 2 arbitrary colors in a 24 bit color system.)
     * <p>
     * This method uses the minimum amount of space (only 255 * number of
     * intervals), but it aggravates the lookup procedure, because now we
     * have to find out which interval to select, then calculate the index
     * within that interval.  This causes a significant performance hit,
     * because it requires this calculation be done for every point in
     * the rendering loop.
     * <p>
     * For those of you who are interested, this is a classic example of the
     * time-space tradeoff.
     */
    private void calculateMultipleArrayGradient(Color[] colors) {
        // set the flag, so we know later it is a non-simple lookup
        isSimpleLookup = false;

        // 2 colors to interpolate
        int rgb1;
        int rgb2;

        // for every interval (transition between 2 colors)
        for (int i = 0; i < gradients.length; i++) {
            // create an array of the maximum theoretical size for
            // each interval
            gradients[i] = new int[GRADIENT_SIZE];

            // get the 2 colors
            rgb1 = colors[i].getRGB();
            rgb2 = colors[i + 1].getRGB();

            // fill this array with the colors in between rgb1 and rgb2
            interpolate(rgb1, rgb2, gradients[i]);

            // if the colors are opaque, transparency should still
            // be 0xff000000
            transparencyTest &= rgb1;
            transparencyTest &= rgb2;
        }

        // if interpolation occurred in Linear RGB space, convert the
        // gradients back to SRGB using the lookup table
        if (colorSpace == MultipleGradientPaint.ColorSpaceType.LINEAR_RGB) {
            for (int j = 0; j < gradients.length; j++) {
                for (int i = 0; i < gradients[j].length; i++) {
                    gradients[j][i] = ColorUtil.linearRGBtoSRGB(gradients[j][i]);
                }
            }
        }
    }

    /**
     * Yet another helper function.  This one linearly interpolates between
     * 2 colors, filling up the output array.
     *
     * @param rgb1   the start color
     * @param rgb2   the end color
     * @param output the output array of colors; must not be null
     */
    private void interpolate(int rgb1, int rgb2, int[] output) {
        // step between interpolated values
        float stepSize = 1.0f / output.length;

        // extract color components from packed integer
        int a1 = (rgb1 >> 24) & 0xff;
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        // calculate the total change in alpha, red, green, blue
        int da = ((rgb2 >> 24) & 0xff) - a1;
        int dr = ((rgb2 >> 16) & 0xff) - r1;
        int dg = ((rgb2 >> 8) & 0xff) - g1;
        int db = (rgb2 & 0xff) - b1;

        // for each step in the interval calculate the in-between color by
        // multiplying the normalized current position by the total color
        // change (0.5 is added to prevent truncation round-off error)
        for (int i = 0; i < output.length; i++) {
            output[i] = ((int) ((a1 + i * da * stepSize) + 0.5) << 24) |
                    ((int) ((r1 + i * dr * stepSize) + 0.5) << 16) |
                    ((int) ((g1 + i * dg * stepSize) + 0.5) << 8) |
                    ((int) ((b1 + i * db * stepSize) + 0.5));
        }
    }

    private static float mod1(float x) {
        float result = x - (int) x;
        if (result < 0) {
            result += 1;
        }
        return result;
    }

    private static float mod1Reflect(float x) {
        float result = Math.abs(x);
        int part = (int) x;
        result = result - part;
        if (part % 2 == 1) {
            // integer part is odd, get reflected color instead
            result = 1 - result;
        }
        return result;
    }

    /**
     * Helper function to index into the gradients array.  This is necessary
     * because each interval has an array of colors with uniform size 255.
     * However, the color intervals are not necessarily of uniform length, so
     * a conversion is required.
     *
     * @param position the unmanipulated position, which will be mapped
     *                 into the range 0 to 1
     * @return integer color to display
     */
    protected final int indexIntoGradientsArrays(float position) {
        // first, manipulate position value depending on the cycle method
        if (cycleMethod == MultipleGradientPaint.CycleMethod.NO_CYCLE) {
            position = Math.min(1, Math.max(0, position));
        } else if (cycleMethod == MultipleGradientPaint.CycleMethod.REPEAT) {
            // get the fractional part
            position = mod1(position);
        } else { // cycleMethod == CycleMethod.REFLECT
            position = mod1Reflect(position);
        }

        // now, get the color based on this 0-1 position...

        if (isSimpleLookup) {
            // easy to compute: just scale index by array size
            return gradient[(int) (position * fastGradientArraySize)];
        } else {
            // more complicated computation, to save space

            // for all the gradient interval arrays
            for (int i = 0; i < gradients.length; i++) {
                if (position < fractions[i + 1]) {
                    // this is the array we want
                    float delta = position - fractions[i];

                    // this is the interval we want
                    int index = (int) ((delta / normalizedIntervals[i]) * GRADIENT_SIZE_INDEX);

                    return gradients[i][index];
                }
            }
        }

        return gradients[gradients.length - 1][GRADIENT_SIZE_INDEX];
    }

    @Override
    public final Raster getRaster(int x, int y, int w, int h) {
        // If working raster is big enough, reuse it. Otherwise,
        // build a large enough new one.
        Raster raster = saved;
        if (raster == null || raster.getWidth() < w || raster.getHeight() < h) {
            raster = getCachedRaster(model, w, h);
            saved = raster;
        }

        // Access raster internal int array. Because we use a DirectColorModel,
        // we know the DataBuffer is of type DataBufferInt and the SampleModel
        // is SinglePixelPackedSampleModel.
        // Adjust for initial offset in DataBuffer and also for the scanline
        // stride.
        // These calls make the DataBuffer non-acceleratable, but the
        // Raster is never Stable long enough to accelerate anyway...
        DataBufferInt rasterDB = (DataBufferInt) raster.getDataBuffer();
        int[] pixels = rasterDB.getData(0);
        int off = rasterDB.getOffset();
        int scanlineStride = ((SinglePixelPackedSampleModel) raster.getSampleModel()).getScanlineStride();
        int adjust = scanlineStride - w;

        fillRaster(pixels, off, adjust, x, y, w, h); // delegate to subclass

        return raster;
    }

    protected abstract void fillRaster(int[] pixels, int off, int adjust, int x, int y, int w, int h);


    /**
     * Took this cacheRaster code from GradientPaint. It appears to recycle
     * rasters for use by any other instance, as long as they are sufficiently
     * large.
     */
    private static synchronized Raster getCachedRaster(ColorModel cm, int w, int h) {
        if (Objects.equals(cm, cachedModel) && cached != null) {
            Raster ras = cached.get();
            if (ras != null && ras.getWidth() >= w && ras.getHeight() >= h) {
                cached = null;
                return ras;
            }
        }

        return cm.createCompatibleWritableRaster(w, h);
    }

    /**
     * Took this cacheRaster code from GradientPaint. It appears to recycle
     * rasters for use by any other instance, as long as they are sufficiently
     * large.
     */
    private static synchronized void putCachedRaster(ColorModel cm, Raster ras) {
        if (cached != null) {
            Raster cras = cached.get();
            if (cras != null) {
                int cw = cras.getWidth();
                int ch = cras.getHeight();
                int iw = ras.getWidth();
                int ih = ras.getHeight();
                if (cw >= iw && ch >= ih) {
                    return;
                }
                if (cw * ch >= iw * ih) {
                    return;
                }
            }
        }
        cachedModel = cm;
        cached = new WeakReference<>(ras);
    }

    @Override
    public final void dispose() {
        if (saved != null) {
            putCachedRaster(model, saved);
            saved = null;
        }
    }

    @Override
    public final ColorModel getColorModel() {
        return model;
    }
}
