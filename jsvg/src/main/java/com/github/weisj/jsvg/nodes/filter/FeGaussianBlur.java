/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeGaussianBlur extends AbstractFilterPrimitive {
    public static final String TAG = "fegaussianblur";
    private static final double SQRT_2_PI = Math.sqrt(2 * Math.PI);
    private static final double THREE_QUARTER_SQRT_2_PI = SQRT_2_PI * 3f / 4f;
    private static final float KERNEL_PRECISION = 0.001f;

    private static final double BOX_BLUR_APPROXIMATION_THRESHOLD = 2;

    private float stdDeviationX;
    private float stdDeviationY;
    private EdgeMode edgeMode;

    private double xCurrent;
    private double yCurrent;
    private Kernel xBlur;
    private Kernel yBlur;
    private boolean onlyAlpha;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        float[] stdDeviation = attributeNode.getFloatList("stdDeviation");
        stdDeviationX = stdDeviation.length > 0 ? stdDeviation[0] : 0;
        stdDeviationY = stdDeviation.length > 1 ? stdDeviation[1] : stdDeviationX;
        if (stdDeviationX < 0 || stdDeviationY < 0) {
            stdDeviationX = 0;
            stdDeviationY = 0;
        }
        edgeMode = attributeNode.getEnum("edgeMode", EdgeMode.None);
    }

    @ApiStatus.Internal
    public void setOnlyAlpha(boolean onlyAlpha) {
        this.onlyAlpha = onlyAlpha;
    }

    private double[] computeStdDeviation(@NotNull UnitType units,
            @NotNull Rectangle2D elementBounds) {
        double xSigma = stdDeviationX;
        double ySigma = stdDeviationY;
        if (units == UnitType.ObjectBoundingBox) {
            xSigma *= elementBounds.getWidth();
            ySigma *= elementBounds.getHeight();
        }
        return new double[] {xSigma, ySigma};
    }

    @Override
    public boolean requiresAlignedBuffer(@NotNull FilterLayoutContext context) {
        double[] sigma = computeStdDeviation(context.primitiveUnits(), context.elementBounds());
        if (sigma[0] == 0 && sigma[1] == 0) return false;
        // Duplicate and wrap extend the input rectangle along primitive axes, even for a circular kernel.
        if (edgeMode != EdgeMode.None) return true;
        AffineTransform transform = context.transform();
        double xContribution = transform.getScaleX() * transform.getShearY() * sigma[0] * sigma[0];
        double yContribution = transform.getShearX() * transform.getScaleY() * sigma[1] * sigma[1];
        // Off-diagonal covariance must vanish for independent horizontal and vertical passes.
        // Allow rounding when the two contributions cancel, as for a circular kernel under rotation.
        return Math.abs(xContribution + yContribution) > 8
                * Math.ulp(Math.max(Math.abs(xContribution), Math.abs(yContribution)));
    }

    private double[] computeAbsoluteStdDeviation(@NotNull AffineTransform transform, @NotNull UnitType units,
            @NotNull Rectangle2D elementBounds) {
        double[] sigma = computeStdDeviation(units, elementBounds);
        double x = sigma[0];
        double y = sigma[1];
        sigma[0] = Math.hypot(transform.getScaleX() * x, transform.getShearX() * y);
        sigma[1] = Math.hypot(transform.getShearY() * x, transform.getScaleY() * y);
        return sigma;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        AffineTransform transform = filterLayoutContext.transform();
        double[] sigma = computeAbsoluteStdDeviation(transform,
                filterLayoutContext.primitiveUnits(), filterLayoutContext.elementBounds());
        int dX = kernelDiameterForStandardDeviation(sigma[0]);
        int dY = kernelDiameterForStandardDeviation(sigma[1]);
        float hExtend = extendForKernelDiameter(sigma[0], dX);
        float vExtend = extendForKernelDiameter(sigma[1], dY);
        // Kernel sizes are rounded in device pixels; layout grows in user coordinates.
        AffineTransform inverse = filterLayoutContext.inverseTransform();
        float hUserExtend = (float) (Math.abs(inverse.getScaleX()) * hExtend
                + Math.abs(inverse.getShearX()) * vExtend);
        float vUserExtend = (float) (Math.abs(inverse.getShearY()) * hExtend
                + Math.abs(inverse.getScaleY()) * vExtend);
        Rectangle2D sourceBounds = input.sourceBounds();
        Rectangle2D outputSourceBounds = filterLayoutContext.filterPrimitiveRegion(impl(), sourceBounds);
        Rectangle2D tightInputBounds = input.tightSourceBounds().createIntersection(sourceBounds);
        LayoutBounds grownInput = input.withTightSourceBounds(tightInputBounds)
                .grow(hUserExtend, vUserExtend, filterLayoutContext);

        // sourceBounds is the logical image extent. Transparent space between it and the tight
        // bounds is part of the input image, so all edge modes conceptually operate on the same
        // outputSourceBounds. edgeMode only changes how samples outside sourceBounds are supplied.
        // The tighter affected bounds below may still limit allocation and final output clipping to
        // pixels that can actually become nontransparent.
        LayoutBounds affectedBounds;
        switch (edgeMode) {
            case None:
                affectedBounds = grownInput;
                break;
            case Duplicate:
                Rectangle2D duplicatedBounds = duplicateAffectedBounds(
                        tightInputBounds, sourceBounds, outputSourceBounds, hUserExtend, vUserExtend);
                affectedBounds = grownInput.withTightSourceBounds(
                        GeometryUtil.union(grownInput.tightSourceBounds(), duplicatedBounds));
                break;
            case Wrap:
                Rectangle2D wrappedBounds = wrapAffectedBounds(
                        tightInputBounds, outputSourceBounds, hUserExtend > 0, vUserExtend > 0);
                affectedBounds = grownInput.withTightSourceBounds(
                        GeometryUtil.union(grownInput.tightSourceBounds(), wrappedBounds));
                // A clipped repaint at a logical input edge can read the opposite edge. Asking for
                // the full logical span does not allocate its transparent padding: allocation is
                // still intersected with tightSourceBounds.
                Rectangle2D requiredInput = wrapInputBounds(
                        sourceBounds, filterLayoutContext.clipBounds(), hUserExtend > 0, vUserExtend > 0);
                affectedBounds = affectedBounds.ensuringRectIsAvailable(requiredInput, filterLayoutContext);
                break;
            default:
                throw new IllegalStateException("Unknown edge mode: " + edgeMode);
        }
        // Do not intersect tightSourceBounds with the output subregion here. Samples just outside
        // that subregion can still contribute to pixels inside it; saveResult performs the actual
        // primitive-subregion clip.
        LayoutBounds bounds = affectedBounds.withSourceBounds(outputSourceBounds);
        impl().saveLayoutResult(bounds, filterLayoutContext);
    }

    private static @NotNull Rectangle2D duplicateAffectedBounds(@NotNull Rectangle2D tightSourceBounds,
            @NotNull Rectangle2D sourceBounds, @NotNull Rectangle2D outputSourceBounds,
            float horizontalRadius, float verticalRadius) {
        if (tightSourceBounds.isEmpty() || sourceBounds.isEmpty()) return new Rectangle2D.Double();

        Rectangle2D grownBounds = GeometryUtil.grow(tightSourceBounds,
                new FloatInsets(verticalRadius, horizontalRadius, verticalRadius, horizontalRadius));
        double x = grownBounds.getX();
        double y = grownBounds.getY();
        double maxX = grownBounds.getMaxX();
        double maxY = grownBounds.getMaxY();
        if (horizontalRadius > 0 && tightSourceBounds.getMinX() <= sourceBounds.getMinX()) {
            x = Math.min(x, outputSourceBounds.getMinX());
        }
        if (horizontalRadius > 0 && tightSourceBounds.getMaxX() >= sourceBounds.getMaxX()) {
            maxX = Math.max(maxX, outputSourceBounds.getMaxX());
        }
        if (verticalRadius > 0 && tightSourceBounds.getMinY() <= sourceBounds.getMinY()) {
            y = Math.min(y, outputSourceBounds.getMinY());
        }
        if (verticalRadius > 0 && tightSourceBounds.getMaxY() >= sourceBounds.getMaxY()) {
            maxY = Math.max(maxY, outputSourceBounds.getMaxY());
        }
        return new Rectangle2D.Double(x, y, maxX - x, maxY - y);
    }

    private static @NotNull Rectangle2D wrapAffectedBounds(@NotNull Rectangle2D tightSourceBounds,
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

    private static @NotNull Rectangle2D wrapInputBounds(@NotNull Rectangle2D inputBounds,
            @NotNull Rectangle2D clipBounds, boolean horizontal, boolean vertical) {
        double x = horizontal ? inputBounds.getX() : clipBounds.getX();
        double y = vertical ? inputBounds.getY() : clipBounds.getY();
        double maxX = horizontal ? inputBounds.getMaxX() : clipBounds.getMaxX();
        double maxY = vertical ? inputBounds.getMaxY() : clipBounds.getMaxY();
        return new Rectangle2D.Double(x, y, Math.max(0, maxX - x), Math.max(0, maxY - y));
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (stdDeviationX == 0 && stdDeviationY == 0) {
            impl().noop(filterContext);
            return;
        }

        double[] sigma = computeAbsoluteStdDeviation(filterContext.info().output().transform(),
                filterContext.primitiveUnits(), filterContext.info().elementBounds());
        double xSigma = sigma[0];
        double ySigma = sigma[1];

        if (xSigma <= 0 && ySigma <= 0) {
            impl().noop(filterContext);
            return;
        }

        Channel inputChannel = impl().inputChannel(filterContext);
        if (onlyAlpha) {
            inputChannel = inputChannel.alphaChannel();
        }
        ImageProducer inputProducer = inputChannel.producer();

        Kernel xBlurKernel = null;
        Kernel yBlurKernel = null;
        int dX = kernelDiameterForStandardDeviation(xSigma);
        int dY = kernelDiameterForStandardDeviation(ySigma);

        if (xSigma > 0 && xSigma < BOX_BLUR_APPROXIMATION_THRESHOLD) {
            xBlurKernel = createConvolveKernel(dX, xSigma, true);
        }
        if (ySigma > 0 && ySigma < BOX_BLUR_APPROXIMATION_THRESHOLD) {
            yBlurKernel = createConvolveKernel(dY, ySigma, false);
        }
        int xRadius = convolutionRadius(xSigma, xBlurKernel, dX, true);
        int yRadius = convolutionRadius(ySigma, yBlurKernel, dY, false);

        Rectangle2D inputSourceBounds = filterContext.layout(impl().inputChannelKey()).sourceBounds();
        Rectangle sourceBounds = GeometryUtil.transformBounds(
                filterContext.info().output().transform(), inputSourceBounds).getBounds();
        BufferedImage input = ImageUtil.copy(context, inputProducer, ImageUtil.Premultiplied.Yes);
        boolean linearRGB = !onlyAlpha && colorInterpolation(filterContext) == ColorInterpolation.LinearRGB;
        if (linearRGB) {
            ImageUtil.mapPixels(input.getRaster(), ColorUtil::sRGBtoLinearRGBPre);
        }
        BufferedImage output = edgeMode.convolveSeparable(input, sourceBounds, filterContext.renderingHints(),
                new MixedQualityConvolveOperation(xBlurKernel, yBlurKernel, xRadius, yRadius, dX, dY));
        if (linearRGB) {
            ImageUtil.mapPixels(output.getRaster(), ColorUtil::linearRGBtoSRGBPre);
        }
        impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
    }

    private static int convolutionRadius(double sigma, @Nullable Kernel kernel, int diameter,
            boolean horizontal) {
        if (sigma <= 0) {
            // A zero stdDeviation component omits convolution on this axis. Since edgeMode only
            // supplies samples required by a convolution kernel, an omitted pass needs no padding.
            return 0;
        }
        int pixels;
        if (kernel != null) {
            pixels = horizontal ? kernel.getXOrigin() : kernel.getYOrigin();
        } else {
            pixels = boxRadius(diameter);
        }
        return pixels;
    }

    private @NotNull Kernel createConvolveKernel(int diameter, double sigma, boolean horizontal) {
        if (horizontal && xBlur != null && xCurrent == sigma) return xBlur;
        if (!horizontal && yBlur != null && yCurrent == sigma) return yBlur;

        if (horizontal) {
            xCurrent = sigma;
        } else {
            yCurrent = sigma;
        }

        float[] data = computeGaussianKernelData(diameter, sigma);

        if (horizontal) {
            xBlur = new Kernel(diameter, 1, data);
        } else {
            yBlur = new Kernel(1, diameter, data);
        }

        return horizontal ? xBlur : yBlur;
    }

    private static float normalConvolve(float x, double standardDeviation) {
        return (float) (Math.pow(Math.E, -x * x / (2 * standardDeviation * standardDeviation))
                / (standardDeviation * SQRT_2_PI));
    }

    private static float[] computeGaussianKernelData(int diameter, double standardDeviation) {
        final float[] data = new float[diameter];

        int mid = diameter / 2;
        float total = 0;
        for (int i = 0; i < diameter; i++) {
            data[i] = normalConvolve((float) i - mid, standardDeviation);
            total += data[i];
        }

        // Otherwise, data is all zeros, which we can't reasonably normalize.
        if (total > 0) {
            for (int i = 0; i < diameter; i++) {
                data[i] /= total;
            }
        }

        return data;
    }

    private static int kernelDiameterForStandardDeviation(double standardDeviation) {
        if (standardDeviation < BOX_BLUR_APPROXIMATION_THRESHOLD) {
            float areaSum = (float) (0.5 / (standardDeviation * SQRT_2_PI));
            int i = 0;
            while (areaSum < 0.5 - KERNEL_PRECISION) {
                areaSum += normalConvolve(i, standardDeviation);
                i++;
            }
            return i * 2 + 1;
        } else {
            return (int) Math.floor(THREE_QUARTER_SQRT_2_PI * standardDeviation + 0.5f);
        }
    }

    private static float extendForKernelDiameter(double standardDeviation, int diameter) {
        return standardDeviation < BOX_BLUR_APPROXIMATION_THRESHOLD ? diameter / 2.0f : boxRadius(diameter);
    }

    private static int boxRadius(int diameter) {
        // Both layout and padding must cover all three box passes.
        return (diameter & 1) == 0 ? 3 * (diameter / 2) - 1 : 3 * (diameter / 2);
    }

    private static final class MixedQualityConvolveOperation implements EdgeMode.SeparableConvolveOperation {

        private final @Nullable Kernel xKernel;
        private final @Nullable Kernel yKernel;
        private final int xRadius;
        private final int yRadius;

        private final int dX;
        private final int dY;

        private MixedQualityConvolveOperation(@Nullable Kernel xKernel, @Nullable Kernel yKernel,
                int xRadius, int yRadius, int dX, int dY) {
            this.xKernel = xKernel;
            this.yKernel = yKernel;
            this.xRadius = xRadius;
            this.yRadius = yRadius;
            this.dX = dX;
            this.dY = dY;
        }

        @Override
        public int radius(@NotNull EdgeMode.Axis axis) {
            switch (axis) {
                case Horizontal:
                    return xRadius;
                case Vertical:
                    return yRadius;
                default:
                    throw new IllegalArgumentException("Unknown axis: " + axis);
            }
        }

        @Override
        public @NotNull EdgeMode.Axis firstAxis() {
            return xKernel != null && yKernel == null && dY > 1
                    ? EdgeMode.Axis.Vertical
                    : EdgeMode.Axis.Horizontal;
        }

        @Override
        public @NotNull BufferedImage convolve(@NotNull BufferedImage image, @NotNull EdgeMode.Axis axis,
                @Nullable RenderingHints hints) {
            WritableRaster raster = image.getRaster();
            if (!image.getColorModel().isAlphaPremultiplied()) {
                throw new IllegalStateException("Image should be premultiplied");
            }

            if (axis == EdgeMode.Axis.Horizontal) {
                if (xKernel != null) {
                    return new ConvolveOp(xKernel, ConvolveOp.EDGE_NO_OP, hints).filter(image, null);
                }
                horizontalBoxBlur(raster);
            } else if (yKernel != null) {
                return new ConvolveOp(yKernel, ConvolveOp.EDGE_NO_OP, hints).filter(image, null);
            } else {
                verticalBoxBlur(raster);
            }
            return image;
        }

        private void horizontalBoxBlur(@NotNull WritableRaster raster) {
            if (dX == 1) return;
            if ((dX & 0x01) == 0) {
                InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, dX, dX / 2);
                InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, dX, dX / 2 - 1);
                InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, dX + 1, dX / 2);
            } else {
                InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, dX, dX / 2);
                InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, dX, dX / 2);
                InplaceBoxBlurFilter.horizontalPass(raster, raster, 0, 0, dX, dX / 2);
            }
        }

        private void verticalBoxBlur(@NotNull WritableRaster raster) {
            if (dY == 1) return;
            if ((dY & 0x01) == 0) {
                InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, dY, dY / 2);
                InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, dY, dY / 2 - 1);
                InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, dY + 1, dY / 2);
            } else {
                InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, dY, dY / 2);
                InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, dY, dY / 2);
                InplaceBoxBlurFilter.verticalPass(raster, raster, 0, 0, dY, dY / 2);
            }
        }
    }
}
