/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;

/** Common bounds handling for filters which sample a rectangular neighborhood around each pixel. */
public abstract class AbstractConvolveFilter extends AbstractFilterPrimitive {

    protected final void saveConvolutionLayout(@NotNull FilterLayoutContext context,
            @NotNull EdgeMode edgeMode, @NotNull FloatInsets samplingInsets,
            boolean canMakeTransparentPixelsOpaque) {
        LayoutBounds input = impl().layoutInput(context);
        Rectangle2D sourceBounds = input.sourceBounds();
        Rectangle2D outputSourceBounds = context.filterPrimitiveRegion(impl(), sourceBounds);
        Rectangle2D tightInputBounds = input.tightSourceBounds().createIntersection(sourceBounds);
        // If an output pixel reads to its right, an input pixel can affect output to its left (and
        // likewise on every other side). Sampling reach and affected-output growth are opposites.
        FloatInsets outputGrowthInsets = new FloatInsets(
                samplingInsets.bottom(), samplingInsets.right(),
                samplingInsets.top(), samplingInsets.left());
        LayoutBounds grownInput = input.withTightSourceBounds(tightInputBounds)
                .grow(outputGrowthInsets, context);

        LayoutBounds affectedBounds;
        switch (edgeMode) {
            case None:
                affectedBounds = grownInput;
                break;
            case Duplicate:
                Rectangle2D duplicatedBounds = duplicateAffectedBounds(
                        tightInputBounds, sourceBounds, outputSourceBounds,
                        outputGrowthInsets, samplingInsets);
                affectedBounds = grownInput.withTightSourceBounds(
                        GeometryUtil.union(grownInput.tightSourceBounds(), duplicatedBounds));
                break;
            case Wrap:
            case Mirror:
                boolean horizontal = samplingInsets.left() > 0 || samplingInsets.right() > 0;
                boolean vertical = samplingInsets.top() > 0 || samplingInsets.bottom() > 0;
                Rectangle2D repeatedBounds = repeatedAffectedBounds(
                        tightInputBounds, outputSourceBounds, horizontal, vertical);
                affectedBounds = grownInput.withTightSourceBounds(
                        GeometryUtil.union(grownInput.tightSourceBounds(), repeatedBounds));
                // A repeated sample can read the opposite side of the logical input. Make that span
                // available without treating its transparent padding as affected output.
                Rectangle2D requiredInput = repeatedInputBounds(
                        sourceBounds, context.clipBounds(), horizontal, vertical);
                affectedBounds = affectedBounds.ensuringRectIsAvailable(requiredInput, context);
                break;
            default:
                throw new IllegalStateException("Unknown edge mode: " + edgeMode);
        }
        if (canMakeTransparentPixelsOpaque) {
            affectedBounds = affectedBounds.withTightSourceBounds(
                    GeometryUtil.union(affectedBounds.tightSourceBounds(), outputSourceBounds));
        }
        // Samples outside the output subregion can contribute to pixels inside it. saveResult applies
        // the primitive-subregion clip after the convolution has run.
        impl().saveLayoutResult(affectedBounds.withSourceBounds(outputSourceBounds), context);
    }

    protected final @NotNull Rectangle2D constantInputRegion(@NotNull FilterContext context,
            @NotNull EdgeMode edgeMode, @NotNull FloatInsets deviceSpaceSamplingInsets) {
        Rectangle2D outputRegion = context.primitiveRegion(impl());
        if (edgeMode != EdgeMode.None) return outputRegion;

        AffineTransform inverse = GeometryUtil.createInverse(context.info().output().transform());
        return GeometryUtil.grow(outputRegion,
                userSpaceInsetsForDeviceSpaceInsets(inverse, deviceSpaceSamplingInsets));
    }

    /** Returns user-space bounds for an axis-aligned device-space sampling rectangle. */
    protected static @NotNull FloatInsets userSpaceInsetsForDeviceSpaceInsets(
            @NotNull AffineTransform inverseTransform, @NotNull FloatInsets deviceInsets) {
        Point2D.Double corner = new Point2D.Double();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double[] x = {-deviceInsets.left(), deviceInsets.right()};
        double[] y = {-deviceInsets.top(), deviceInsets.bottom()};
        for (double dx : x) {
            for (double dy : y) {
                corner.setLocation(dx, dy);
                inverseTransform.deltaTransform(corner, corner);
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
            }
        }
        return new FloatInsets((float) -minY, (float) -minX, (float) maxY, (float) maxX);
    }

    private static @NotNull Rectangle2D duplicateAffectedBounds(@NotNull Rectangle2D tightSourceBounds,
            @NotNull Rectangle2D sourceBounds, @NotNull Rectangle2D outputSourceBounds,
            @NotNull FloatInsets outputGrowthInsets, @NotNull FloatInsets samplingInsets) {
        if (tightSourceBounds.isEmpty() || sourceBounds.isEmpty()) return new Rectangle2D.Double();

        Rectangle2D grownBounds = GeometryUtil.grow(tightSourceBounds, outputGrowthInsets);
        double x = grownBounds.getX();
        double y = grownBounds.getY();
        double maxX = grownBounds.getMaxX();
        double maxY = grownBounds.getMaxY();
        boolean horizontal = samplingInsets.left() > 0 || samplingInsets.right() > 0;
        boolean vertical = samplingInsets.top() > 0 || samplingInsets.bottom() > 0;
        if (horizontal && tightSourceBounds.getMinX() <= sourceBounds.getMinX()) {
            x = Math.min(x, outputSourceBounds.getMinX());
        }
        if (horizontal && tightSourceBounds.getMaxX() >= sourceBounds.getMaxX()) {
            maxX = Math.max(maxX, outputSourceBounds.getMaxX());
        }
        if (vertical && tightSourceBounds.getMinY() <= sourceBounds.getMinY()) {
            y = Math.min(y, outputSourceBounds.getMinY());
        }
        if (vertical && tightSourceBounds.getMaxY() >= sourceBounds.getMaxY()) {
            maxY = Math.max(maxY, outputSourceBounds.getMaxY());
        }
        return new Rectangle2D.Double(x, y, maxX - x, maxY - y);
    }

    private static @NotNull Rectangle2D repeatedAffectedBounds(@NotNull Rectangle2D tightSourceBounds,
            @NotNull Rectangle2D outputSourceBounds, boolean horizontal, boolean vertical) {
        if (tightSourceBounds.isEmpty() || outputSourceBounds.isEmpty()) return new Rectangle2D.Double();

        double x = horizontal
                ? Math.min(tightSourceBounds.getMinX(), outputSourceBounds.getMinX())
                : tightSourceBounds.getMinX();
        double y = vertical
                ? Math.min(tightSourceBounds.getMinY(), outputSourceBounds.getMinY())
                : tightSourceBounds.getMinY();
        double maxX = horizontal
                ? Math.max(tightSourceBounds.getMaxX(), outputSourceBounds.getMaxX())
                : tightSourceBounds.getMaxX();
        double maxY = vertical
                ? Math.max(tightSourceBounds.getMaxY(), outputSourceBounds.getMaxY())
                : tightSourceBounds.getMaxY();
        return new Rectangle2D.Double(x, y, maxX - x, maxY - y);
    }

    private static @NotNull Rectangle2D repeatedInputBounds(@NotNull Rectangle2D inputBounds,
            @NotNull Rectangle2D clipBounds, boolean horizontal, boolean vertical) {
        double x = horizontal ? inputBounds.getX() : clipBounds.getX();
        double y = vertical ? inputBounds.getY() : clipBounds.getY();
        double maxX = horizontal ? inputBounds.getMaxX() : clipBounds.getMaxX();
        double maxY = vertical ? inputBounds.getMaxY() : clipBounds.getMaxY();
        return new Rectangle2D.Double(x, y, Math.max(0, maxX - x), Math.max(0, maxY - y));
    }
}
