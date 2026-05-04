/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

/**
 * Implements the SVG feConvolveMatrix filter primitive.
 *
 * <p>Applies a matrix convolution filter effect. A convolution combines pixels in the input image
 * with neighbouring pixels to produce a resulting image, using a kernel matrix.
 *
 * @see <a href="https://www.w3.org/TR/filter-effects-1/#feConvolveMatrixElement">SVG spec</a>
 */
@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeConvolveMatrix extends AbstractFilterPrimitive {
    public static final String TAG = "feConvolveMatrix";

    private int orderX;
    private int orderY;
    private float[] kernelMatrix;
    private float divisor;
    private float bias;
    private int targetX;
    private int targetY;
    private EdgeMode edgeMode;
    private boolean preserveAlpha;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        float[] order = attributeNode.getFloatList("order");
        if (order.length == 0) {
            orderX = 3;
            orderY = 3;
        } else if (order.length == 1) {
            orderX = Math.max(1, (int) order[0]);
            orderY = orderX;
        } else {
            orderX = Math.max(1, (int) order[0]);
            orderY = Math.max(1, (int) order[1]);
        }

        kernelMatrix = attributeNode.getFloatList("kernelMatrix");

        if (kernelMatrix.length == orderX * orderY) {
            float sum = 0;
            for (float v : kernelMatrix) sum += v;
            divisor = attributeNode.getFloat("divisor", sum == 0 ? 1 : sum);
        } else {
            divisor = 1;
        }

        bias = attributeNode.getFloat("bias", 0);
        targetX = attributeNode.getInt("targetX", orderX / 2);
        targetY = attributeNode.getInt("targetY", orderY / 2);
        edgeMode = attributeNode.getEnum("edgeMode", EdgeMode.Duplicate);
        preserveAlpha = "true".equalsIgnoreCase(attributeNode.getValue("preserveAlpha"));
    }

    @Override
    public boolean isValid() {
        return kernelMatrix != null
                && kernelMatrix.length == orderX * orderY
                && orderX > 0 && orderY > 0
                && divisor != 0
                && targetX >= 0 && targetX < orderX
                && targetY >= 0 && targetY < orderY;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        int hExtend = Math.max(targetX, orderX - 1 - targetX);
        int vExtend = Math.max(targetY, orderY - 1 - targetY);
        impl().saveLayoutResult(input.grow(hExtend, vExtend, filterLayoutContext), filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (!isValid()) {
            impl().noop(filterContext);
            return;
        }

        Channel inputChannel = impl().inputChannel(filterContext);
        ImageProducer input = inputChannel.producer();

        ConvolveMatrixOperation op = new ConvolveMatrixOperation(
                kernelMatrix, orderX, orderY, targetX, targetY, divisor, bias, preserveAlpha);

        ImageProducer output = edgeMode.convolve(context, filterContext, input, op);
        impl().saveResult(new ImageProducerChannel(output), filterContext);
    }

    private static final class ConvolveMatrixOperation implements EdgeMode.ConvolveOperation {

        private final float[] kernelMatrix;
        private final int orderX, orderY;
        private final int targetX, targetY;
        private final float divisor, bias;
        private final boolean preserveAlpha;

        ConvolveMatrixOperation(float[] kernelMatrix, int orderX, int orderY,
                int targetX, int targetY, float divisor, float bias, boolean preserveAlpha) {
            this.kernelMatrix = kernelMatrix;
            this.orderX = orderX;
            this.orderY = orderY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.divisor = divisor;
            this.bias = bias;
            this.preserveAlpha = preserveAlpha;
        }

        @Override
        public @NotNull Dimension maximumKernelSize() {
            return new Dimension(
                    2 * Math.max(targetX, orderX - 1 - targetX),
                    2 * Math.max(targetY, orderY - 1 - targetY));
        }

        @Override
        public @NotNull ImageProducer convolve(@NotNull BufferedImage image, @Nullable RenderingHints hints,
                int awtEdgeMode) {
            BufferedImageOp op = new ConvolveMatrixOp(
                    kernelMatrix, orderX, orderY, targetX, targetY, divisor, bias, preserveAlpha, awtEdgeMode);
            return new FilteredImageSource(image.getSource(), new BufferedImageFilter(op));
        }
    }

    private static final class ConvolveMatrixOp implements BufferedImageOp {

        private final float[] kernelMatrix;
        private final int orderX, orderY;
        private final int targetX, targetY;
        private final float divisor;
        private final float biasScaled; // bias * 255
        private final boolean preserveAlpha;
        private final int awtEdgeMode;

        ConvolveMatrixOp(float[] kernelMatrix, int orderX, int orderY,
                int targetX, int targetY, float divisor, float bias, boolean preserveAlpha, int awtEdgeMode) {
            this.kernelMatrix = kernelMatrix;
            this.orderX = orderX;
            this.orderY = orderY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.divisor = divisor;
            this.biasScaled = bias * 255f;
            this.preserveAlpha = preserveAlpha;
            this.awtEdgeMode = awtEdgeMode;
        }

        @Override
        public @NotNull BufferedImage createCompatibleDestImage(@NotNull BufferedImage src,
                @Nullable ColorModel destCM) {
            if (destCM == null) destCM = src.getColorModel();
            return new BufferedImage(destCM,
                    destCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()),
                    destCM.isAlphaPremultiplied(), null);
        }

        @Override
        public @NotNull Rectangle2D getBounds2D(@NotNull BufferedImage src) {
            return new Rectangle(0, 0, src.getWidth(), src.getHeight());
        }

        @Override
        public @NotNull Point2D getPoint2D(@NotNull Point2D srcPt, @Nullable Point2D dstPt) {
            if (dstPt == null) dstPt = new Point2D.Float();
            dstPt.setLocation(srcPt);
            return dstPt;
        }

        @Override
        public @Nullable RenderingHints getRenderingHints() {
            return null;
        }

        @Override
        public @NotNull BufferedImage filter(@NotNull BufferedImage src, @Nullable BufferedImage dst) {
            if (src == dst) throw new IllegalArgumentException("src and dst must be different");
            if (dst == null) dst = createCompatibleDestImage(src, null);

            Raster srcRaster = src.getRaster();
            WritableRaster dstRaster = dst.getRaster();

            int width = src.getWidth();
            int height = src.getHeight();

            int[] srcPixel = new int[4];
            int[] dstPixel = new int[4];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float sumR = 0, sumG = 0, sumB = 0, sumA = 0;

                    for (int kj = 0; kj < orderY; kj++) {
                        int srcY = y - targetY + kj;
                        for (int ki = 0; ki < orderX; ki++) {
                            int srcX = x - targetX + ki;
                            // The SVG spec formula uses kernelMatrix[(orderX-i-1) + (orderY-j-1)*orderX]
                            float kernelVal = kernelMatrix[(orderX - ki - 1) + (orderY - kj - 1) * orderX];

                            int r, g, b, a;
                            if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                                srcRaster.getPixel(srcX, srcY, srcPixel);
                                r = srcPixel[0];
                                g = srcPixel[1];
                                b = srcPixel[2];
                                a = srcPixel[3];
                            } else if (awtEdgeMode == ConvolveOp.EDGE_ZERO_FILL) {
                                r = g = b = a = 0;
                            } else {
                                // EDGE_NO_OP: the image should have been pre-padded so this shouldn't happen
                                r = g = b = a = 0;
                            }

                            sumR += r * kernelVal;
                            sumG += g * kernelVal;
                            sumB += b * kernelVal;
                            sumA += a * kernelVal;
                        }
                    }

                    dstPixel[0] = clamp(sumR / divisor + biasScaled);
                    dstPixel[1] = clamp(sumG / divisor + biasScaled);
                    dstPixel[2] = clamp(sumB / divisor + biasScaled);
                    dstPixel[3] = clamp(sumA / divisor + biasScaled);

                    if (preserveAlpha) {
                        srcRaster.getPixel(x, y, srcPixel);
                        dstPixel[3] = srcPixel[3];
                    }

                    dstRaster.setPixel(x, y, dstPixel);
                }
            }

            return dst;
        }

        private static int clamp(float value) {
            return Math.max(0, Math.min(255, Math.round(value)));
        }
    }
}
