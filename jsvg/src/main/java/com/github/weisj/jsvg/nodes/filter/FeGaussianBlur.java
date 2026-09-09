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
import com.github.weisj.jsvg.attributes.filter.LayoutBounds.CoversWholeRegion;
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
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(), input.region());
        // Wrapping can repeat content anywhere; duplication only adds content outside the input region.
        boolean extendsInput = edgeMode == EdgeMode.Wrap
                || (edgeMode == EdgeMode.Duplicate && !input.region().contains(region));
        boolean nonZeroKernel = sigma[0] > 0 || sigma[1] > 0;
        CoversWholeRegion coversWholeRegion = extendsInput && nonZeroKernel && !input.region().isEmpty()
                ? CoversWholeRegion.YES
                : CoversWholeRegion.NO;
        LayoutBounds bounds =
                input.grow(hUserExtend, vUserExtend, filterLayoutContext).withRegion(region, coversWholeRegion);
        impl().saveLayoutResult(bounds, filterLayoutContext);
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
        ImageProducer input = inputChannel.producer();

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

        Rectangle2D inputRegion = filterContext.layout(impl().inputChannelKey()).region();
        Rectangle sourceBounds = GeometryUtil.transformBounds(
                filterContext.info().output().transform(), inputRegion).getBounds();
        ImageProducer output = edgeMode.convolve(context, filterContext, input, sourceBounds,
                new MixedQualityConvolveOperation(xBlurKernel, yBlurKernel, dX, dY,
                        !onlyAlpha && colorInterpolation(filterContext) == ColorInterpolation.LinearRGB));
        impl().saveResult(new ImageProducerChannel(output), filterContext);
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

    private static final class MixedQualityConvolveOperation implements EdgeMode.ConvolveOperation {

        private final @Nullable Kernel xKernel;
        private final @Nullable Kernel yKernel;

        private final int dX;
        private final int dY;
        private final boolean linearRGB;

        private MixedQualityConvolveOperation(@Nullable Kernel xKernel, @Nullable Kernel yKernel, int dX, int dY,
                boolean linearRGB) {
            this.xKernel = xKernel;
            this.yKernel = yKernel;
            this.dX = dX;
            this.dY = dY;
            this.linearRGB = linearRGB;
        }


        @Override
        public @NotNull Dimension kernelRadius() {
            return new Dimension(
                    xKernel != null ? xKernel.getXOrigin() : boxRadius(dX),
                    yKernel != null ? yKernel.getYOrigin() : boxRadius(dY));
        }

        @Override
        public @NotNull ImageProducer convolve(@NotNull BufferedImage image, @Nullable RenderingHints hints,
                int awtEdgeMode) {
            WritableRaster raster = image.getRaster();
            if (!image.getColorModel().isAlphaPremultiplied()) {
                throw new IllegalStateException("Image should be premultiplied");
            }

            if (linearRGB) {
                ImageUtil.mapPixels(raster, ColorUtil::sRGBtoLinearRGBPre);
            }
            BufferedImage result;
            if (xKernel != null && yKernel != null) {
                BufferedImageOp op = new MultiConvolveOp(new ConvolveOp[] {
                        new ConvolveOp(xKernel, awtEdgeMode, hints),
                        new ConvolveOp(yKernel, awtEdgeMode, hints)
                });
                result = op.filter(image, null);
            } else if (xKernel != null) {
                verticalBoxBlur(raster);
                result = new ConvolveOp(xKernel, awtEdgeMode, hints).filter(image, null);
            } else if (yKernel != null) {
                horizontalBoxBlur(raster);
                result = new ConvolveOp(yKernel, awtEdgeMode, hints).filter(image, null);
            } else {
                horizontalBoxBlur(raster);
                verticalBoxBlur(raster);
                result = image;
            }
            if (linearRGB) {
                ImageUtil.mapPixels(result.getRaster(), ColorUtil::linearRGBtoSRGBPre);
            }
            return result.getSource();
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
