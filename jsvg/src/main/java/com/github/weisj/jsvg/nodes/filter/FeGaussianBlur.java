/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
import java.awt.image.*;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.InplaceBoxBlurFilter;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

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

    private float[] stdDeviation;
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
        stdDeviation = attributeNode.getFloatList("stdDeviation");
        edgeMode = attributeNode.getEnum("edgeMode", EdgeMode.Duplicate);
    }

    @ApiStatus.Internal
    public void setOnlyAlpha(boolean onlyAlpha) {
        this.onlyAlpha = onlyAlpha;
    }

    private double[] computeAbsoluteStdDeviation(@Nullable AffineTransform at) {
        if (stdDeviation.length == 0) return new double[] {0, 0};
        double xSigma = stdDeviation[0];
        double ySigma = stdDeviation[Math.min(stdDeviation.length - 1, 1)];
        if (at != null) {
            xSigma *= GeometryUtil.scaleXOfTransform(at);
            ySigma *= GeometryUtil.scaleYOfTransform(at);
        }
        return new double[] {xSigma, ySigma};
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        double[] sigma = computeAbsoluteStdDeviation(null);
        int hExtend = kernelDiameterForStandardDeviation(sigma[0]);
        int vExtend = kernelDiameterForStandardDeviation(sigma[1]);
        impl().saveLayoutResult(input.grow(hExtend, vExtend, filterLayoutContext), filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (stdDeviation.length == 0) {
            impl().noop(filterContext);
            return;
        }

        double[] sigma = computeAbsoluteStdDeviation(filterContext.info().output().transform());
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
            yBlurKernel = createConvolveKernel(dX, ySigma, false);
        }

        ImageProducer output = edgeMode.convolve(context, filterContext, input,
                new MixedQualityConvolveOperation(xBlurKernel, yBlurKernel, dX, dY));
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

    public static int kernelDiameterForStandardDeviation(double standardDeviation) {
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


    private static final class MixedQualityConvolveOperation implements EdgeMode.ConvolveOperation {

        private final @Nullable Kernel xKernel;
        private final @Nullable Kernel yKernel;

        private final int dX;
        private final int dY;

        private MixedQualityConvolveOperation(@Nullable Kernel xKernel, @Nullable Kernel yKernel, int dX, int dY) {
            this.xKernel = xKernel;
            this.yKernel = yKernel;
            this.dX = dX;
            this.dY = dY;
        }


        @Override
        public @NotNull Dimension maximumKernelSize() {
            return new Dimension(
                    xKernel != null ? xKernel.getXOrigin() : dX,
                    yKernel != null ? yKernel.getXOrigin() : dY);
        }

        @Override
        public @NotNull ImageProducer convolve(@NotNull BufferedImage image, @Nullable RenderingHints hints,
                int awtEdgeMode) {
            WritableRaster raster = image.getRaster();
            if (!image.getColorModel().isAlphaPremultiplied()) {
                throw new IllegalStateException("Image should be premultiplied");
            }

            if (xKernel != null && yKernel != null) {
                BufferedImageOp op = new MultiConvolveOp(new ConvolveOp[] {
                        new ConvolveOp(xKernel, awtEdgeMode, hints),
                        new ConvolveOp(yKernel, awtEdgeMode, hints)
                });
                return new FilteredImageSource(image.getSource(), new BufferedImageFilter(op));
            } else if (xKernel != null) {
                verticalBoxBlur(raster);
                return new FilteredImageSource(image.getSource(), new BufferedImageFilter(
                        new ConvolveOp(xKernel, awtEdgeMode, hints)));
            } else if (yKernel != null) {
                horizontalBoxBlur(raster);
                return new FilteredImageSource(image.getSource(), new BufferedImageFilter(
                        new ConvolveOp(yKernel, awtEdgeMode, hints)));
            } else {
                horizontalBoxBlur(raster);
                verticalBoxBlur(raster);
                return image.getSource();
            }
        }

        private void horizontalBoxBlur(@NotNull WritableRaster raster) {
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
