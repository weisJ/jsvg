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

import static com.github.weisj.jsvg.util.ColorUtil.toRgbRange;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.EdgeMode;
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

/** Applies an arbitrary convolution kernel to an input filter image. */
@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeConvolveMatrix extends AbstractConvolveFilter {
    public static final String TAG = "feconvolvematrix";

    private int orderX;
    private int orderY;
    private float @NotNull [] kernelMatrix = new float[0];
    private float divisor;
    private float bias;
    private int targetX;
    private int targetY;
    private EdgeMode edgeMode;
    private boolean preserveAlpha;
    private double @Nullable [] kernelUnitLength;

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
            int orderValue = (int) order[0];
            orderX = orderValue;
            orderY = orderValue;
        } else if (order.length == 2) {
            orderX = (int) order[0];
            orderY = (int) order[1];
        } else {
            orderX = 0;
            orderY = 0;
        }

        kernelMatrix = attributeNode.getFloatList("kernelMatrix");
        double kernelSum = sumKernel(kernelMatrix);
        float defaultDivisor = kernelSum == 0 ? 1 : (float) kernelSum;
        divisor = attributeNode.getFloat("divisor", defaultDivisor);
        if (divisor == 0) divisor = defaultDivisor;

        bias = attributeNode.getFloat("bias", 0);
        targetX = attributeNode.getInt("targetX", orderX / 2);
        targetY = attributeNode.getInt("targetY", orderY / 2);
        edgeMode = attributeNode.getEnum("edgeMode", EdgeMode.Duplicate);
        preserveAlpha = "true".equalsIgnoreCase(attributeNode.getValue("preserveAlpha"));
        kernelUnitLength = parseKernelUnitLength(attributeNode.getDoubleList("kernelUnitLength"));
    }

    private static double sumKernel(float @NotNull [] kernel) {
        // Compensated summation preserves cancellations such as 8 * 0.1f - 0.8f. A naive float
        // sum turns that mathematically zero kernel into a tiny divisor and amplifies every sample.
        double sum = 0;
        double correction = 0;
        for (float value : kernel) {
            double correctedValue = value - correction;
            double nextSum = sum + correctedValue;
            correction = (nextSum - sum) - correctedValue;
            sum = nextSum;
        }
        return sum;
    }

    private static double @Nullable [] parseKernelUnitLength(double @NotNull [] values) {
        if (values.length != 1 && values.length != 2) return null;
        double x = values[0];
        double y = values.length == 2 ? values[1] : x;
        if (!Double.isFinite(x) || !Double.isFinite(y) || x <= 0 || y <= 0) return null;
        return new double[] {x, y};
    }

    @Override
    public boolean isValid() {
        if (orderX <= 0 || orderY <= 0 || targetX < 0 || targetX >= orderX
                || targetY < 0 || targetY >= orderY || !Float.isFinite(divisor)
                || !Float.isFinite(bias)) {
            return false;
        }
        long kernelSize = (long) orderX * orderY;
        if (kernelSize != kernelMatrix.length) return false;
        for (float value : kernelMatrix) {
            if (!Float.isFinite(value)) return false;
        }
        return true;
    }

    @Override
    public boolean requiresAlignedBuffer(@NotNull FilterLayoutContext context) {
        // An explicit kernel grid is expressed in primitive coordinates. Non-zero edge modes also
        // extend the logical input rectangle along those axes, so both need an axis-aligned buffer.
        return kernelUnitLength != null || edgeMode != EdgeMode.None;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        double[] step = samplingStep(filterLayoutContext.transform(), filterLayoutContext.primitiveUnits(),
                filterLayoutContext.elementBounds());
        FloatInsets deviceInsets = samplingInsets(step[0], step[1]);
        FloatInsets userInsets = userSpaceInsetsForDeviceSpaceInsets(
                filterLayoutContext.inverseTransform(), deviceInsets);
        saveConvolutionLayout(filterLayoutContext, edgeMode, userInsets,
                !preserveAlpha && bias > 0);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        AffineTransform outputTransform = filterContext.info().output().transform();
        double[] step = samplingStep(outputTransform, filterContext.primitiveUnits(),
                filterContext.info().elementBounds());
        FloatInsets deviceInsets = samplingInsets(step[0], step[1]);

        Channel inputChannel = impl().inputChannel(filterContext);
        Rectangle2D requiredInput = constantInputRegion(filterContext, edgeMode, deviceInsets);
        ConvolveMatrixOperation operation = new ConvolveMatrixOperation(
                kernelMatrix, orderX, orderY, targetX, targetY, divisor, bias,
                preserveAlpha, step[0], step[1]);
        boolean linearRGB = colorInterpolation(filterContext) == ColorInterpolation.LinearRGB;
        if (inputChannel.isConstant()
                && filterContext.channelOutputCovers(impl().inputChannelKey(), requiredInput)) {
            int constant = convolveConstant(inputChannel.constantColor(), operation, linearRGB);
            impl().saveResult(inputChannel.withConstantColor(constant), filterContext);
            return;
        }

        Rectangle2D sourceRegion = filterContext.layout(impl().inputChannelKey()).sourceBounds();
        Rectangle sourceBounds = GeometryUtil.transformBounds(outputTransform, sourceRegion).getBounds();
        BufferedImage input = ImageUtil.copy(
                context, inputChannel.producer(), ImageUtil.Premultiplied.Yes);
        if (linearRGB) {
            ImageUtil.mapPixels(input.getRaster(), ColorUtil::sRGBtoLinearRGBPre);
        }

        Insets padding = new Insets(
                checkedCeil(deviceInsets.top()), checkedCeil(deviceInsets.left()),
                checkedCeil(deviceInsets.bottom()), checkedCeil(deviceInsets.right()));
        BufferedImage output = edgeMode.convolve(
                input, sourceBounds, padding, filterContext.renderingHints(), operation);
        if (linearRGB) {
            ImageUtil.mapPixels(output.getRaster(), ColorUtil::linearRGBtoSRGBPre);
        }
        impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
    }

    private int convolveConstant(int argb, @NotNull ConvolveMatrixOperation operation, boolean linearRGB) {
        int premultiplied = ColorUtil.premultiply(argb);
        if (linearRGB) premultiplied = ColorUtil.sRGBtoLinearRGBPre(premultiplied);
        int result = operation.convolveConstant(premultiplied);
        if (linearRGB) result = ColorUtil.linearRGBtoSRGBPre(result);
        return ColorUtil.unpremultiply(result);
    }

    private double @NotNull [] samplingStep(@NotNull AffineTransform outputTransform,
            @NotNull UnitType primitiveUnits, @NotNull Rectangle2D elementBounds) {
        if (kernelUnitLength == null) return new double[] {1, 1};

        AffineTransform primitiveToImage = primitiveUnits.applyToTransform(
                new AffineTransform(outputTransform), elementBounds);
        Point2D.Double xStep = new Point2D.Double(kernelUnitLength[0], 0);
        Point2D.Double yStep = new Point2D.Double(0, kernelUnitLength[1]);
        primitiveToImage.deltaTransform(xStep, xStep);
        primitiveToImage.deltaTransform(yStep, yStep);
        return new double[] {xStep.distance(0, 0), yStep.distance(0, 0)};
    }

    private @NotNull FloatInsets samplingInsets(double stepX, double stepY) {
        return new FloatInsets(
                (float) (targetY * stepY),
                (float) (targetX * stepX),
                (float) ((orderY - 1 - targetY) * stepY),
                (float) ((orderX - 1 - targetX) * stepX));
    }

    private static int checkedCeil(float value) {
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Convolution sampling extent exceeds the supported integer range");
        }
        return (int) Math.ceil(value);
    }

    private static final class ConvolveMatrixOperation implements EdgeMode.ConvolveOperation {
        private final float @NotNull [] kernel;
        private final int orderX;
        private final int orderY;
        private final int targetX;
        private final int targetY;
        private final float divisor;
        private final float bias;
        private final boolean preserveAlpha;
        private final double stepX;
        private final double stepY;
        private final double kernelSum;
        private final @Nullable Kernel awtKernel;

        private ConvolveMatrixOperation(float @NotNull [] kernel, int orderX, int orderY,
                int targetX, int targetY, float divisor, float bias, boolean preserveAlpha,
                double stepX, double stepY) {
            this.kernel = kernel;
            this.orderX = orderX;
            this.orderY = orderY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.divisor = divisor;
            this.bias = bias;
            this.preserveAlpha = preserveAlpha;
            this.stepX = stepX;
            this.stepY = stepY;
            kernelSum = sumKernel(kernel);
            awtKernel = createAwtKernel(
                    kernel, orderX, orderY, divisor, bias, preserveAlpha, stepX, stepY);
        }

        @Override
        public @NotNull BufferedImage convolve(@NotNull BufferedImage image,
                @NotNull Rectangle resultRegion, @Nullable RenderingHints hints) {
            if (awtKernel != null) {
                return convolveWithAwt(image, resultRegion, hints);
            }
            return convolveWithCustomSampling(image, resultRegion);
        }

        private static @Nullable Kernel createAwtKernel(float @NotNull [] kernel,
                int orderX, int orderY, float divisor, float bias, boolean preserveAlpha,
                double stepX, double stepY) {
            // ConvolveOp clamps its result immediately. Therefore bias cannot be added afterward,
            // and preserveAlpha needs the exact path's straight-color convolution. ConvolveOp also
            // has no way to express non-unit sampling distances.
            if (bias != 0 || preserveAlpha || stepX != 1 || stepY != 1) return null;

            float[] normalizedKernel = kernel.clone();
            for (int i = 0; i < normalizedKernel.length; i++) {
                normalizedKernel[i] /= divisor;
            }
            return new Kernel(orderX, orderY, normalizedKernel);
        }

        private @NotNull BufferedImage convolveWithAwt(@NotNull BufferedImage image,
                @NotNull Rectangle resultRegion, @Nullable RenderingHints hints) {
            assert awtKernel != null;
            BufferedImage paddedResult = new ConvolveOp(
                    awtKernel, ConvolveOp.EDGE_ZERO_FILL, hints).filter(image, null);

            // ConvolveOp's placement already agrees with SVG's default floor(order / 2) target,
            // including for even-sized kernels. Shift only the difference from that SVG default.
            int resultX = resultRegion.x + orderX / 2 - targetX;
            int resultY = resultRegion.y + orderY / 2 - targetY;
            return copyPremultipliedRegion(paddedResult,
                    new Rectangle(resultX, resultY, resultRegion.width, resultRegion.height));
        }

        private static @NotNull BufferedImage copyPremultipliedRegion(
                @NotNull BufferedImage sourceImage, @NotNull Rectangle sourceRegion) {
            BufferedImage result = ImageUtil.createCompatibleTransparentImage(
                    sourceRegion.width, sourceRegion.height, ImageUtil.Premultiplied.Yes);
            Raster source = sourceImage.getRaster();
            WritableRaster destination = result.getRaster();

            int[] sourcePixels = ImageUtil.getINT_RGBA_DataBank(source);
            int sourceOffset = ImageUtil.getINT_RGBA_DataOffset(source);
            int sourceStride = ImageUtil.getINT_RGBA_ScanlineStride(source);
            int[] destinationPixels = ImageUtil.getINT_RGBA_DataBank(destination);
            int destinationOffset = ImageUtil.getINT_RGBA_DataOffset(destination);
            int destinationStride = ImageUtil.getINT_RGBA_ScanlineStride(destination);

            int sourceIndex = sourceOffset + sourceRegion.y * sourceStride + sourceRegion.x;
            int destinationIndex = destinationOffset;
            for (int y = 0; y < sourceRegion.height; y++) {
                for (int x = 0; x < sourceRegion.width; x++) {
                    int pixel = sourcePixels[sourceIndex++];
                    int alpha = pixel >>> 24;
                    int red = Math.min(alpha, (pixel >>> 16) & 0xff);
                    int green = Math.min(alpha, (pixel >>> 8) & 0xff);
                    int blue = Math.min(alpha, pixel & 0xff);
                    destinationPixels[destinationIndex++] = alpha << 24 | red << 16 | green << 8 | blue;
                }
                sourceIndex += sourceStride - sourceRegion.width;
                destinationIndex += destinationStride - sourceRegion.width;
            }
            return result;
        }

        private @NotNull BufferedImage convolveWithCustomSampling(@NotNull BufferedImage image,
                @NotNull Rectangle resultRegion) {
            BufferedImage result = ImageUtil.createCompatibleTransparentImage(
                    resultRegion.width, resultRegion.height, ImageUtil.Premultiplied.Yes);
            Raster source = image.getRaster();
            WritableRaster destination = result.getRaster();

            int[] sourcePixels = ImageUtil.getINT_RGBA_DataBank(source);
            int sourceOffset = ImageUtil.getINT_RGBA_DataOffset(source);
            int sourceStride = ImageUtil.getINT_RGBA_ScanlineStride(source);
            int[] destinationPixels = ImageUtil.getINT_RGBA_DataBank(destination);
            int destinationOffset = ImageUtil.getINT_RGBA_DataOffset(destination);
            int destinationStride = ImageUtil.getINT_RGBA_ScanlineStride(destination);

            boolean integralSampling = stepX == 1 && stepY == 1;
            int destinationIndex = destinationOffset;
            for (int y = 0; y < resultRegion.height; y++) {
                int centerY = resultRegion.y + y;
                for (int x = 0; x < resultRegion.width; x++) {
                    int centerX = resultRegion.x + x;
                    int center = sourcePixels[sourceOffset + centerY * sourceStride + centerX];
                    double alpha = 0;
                    double red = 0;
                    double green = 0;
                    double blue = 0;
                    for (int row = 0; row < orderY; row++) {
                        double sampleY = centerY + (row - targetY) * stepY;
                        for (int column = 0; column < orderX; column++) {
                            double sampleX = centerX + (column - targetX) * stepX;
                            int sample = integralSampling
                                    ? sourcePixels[sourceOffset + (int) sampleY * sourceStride + (int) sampleX]
                                    : bilinearSample(sourcePixels, sourceOffset, sourceStride,
                                            image.getWidth(), image.getHeight(), sampleX, sampleY);
                            float weight = kernel[(orderY - 1 - row) * orderX + orderX - 1 - column];
                            int sampleAlpha = sample >>> 24;
                            alpha += sampleAlpha * weight;
                            if (preserveAlpha) {
                                if (sampleAlpha != 0) {
                                    red += ((sample >>> 16) & 0xff) * (255.0 / sampleAlpha) * weight;
                                    green += ((sample >>> 8) & 0xff) * (255.0 / sampleAlpha) * weight;
                                    blue += (sample & 0xff) * (255.0 / sampleAlpha) * weight;
                                }
                            } else {
                                red += ((sample >>> 16) & 0xff) * weight;
                                green += ((sample >>> 8) & 0xff) * weight;
                                blue += (sample & 0xff) * weight;
                            }
                        }
                    }
                    destinationPixels[destinationIndex++] = resultPixel(
                            alpha, red, green, blue, center >>> 24);
                }
                destinationIndex += destinationStride - resultRegion.width;
            }
            return result;
        }

        private int convolveConstant(int premultipliedArgb) {
            int alpha = premultipliedArgb >>> 24;
            double red;
            double green;
            double blue;
            if (preserveAlpha && alpha != 0) {
                double unpremultiplyFactor = 255.0 / alpha;
                red = ((premultipliedArgb >>> 16) & 0xff) * unpremultiplyFactor * kernelSum;
                green = ((premultipliedArgb >>> 8) & 0xff) * unpremultiplyFactor * kernelSum;
                blue = (premultipliedArgb & 0xff) * unpremultiplyFactor * kernelSum;
            } else {
                red = ((premultipliedArgb >>> 16) & 0xff) * kernelSum;
                green = ((premultipliedArgb >>> 8) & 0xff) * kernelSum;
                blue = (premultipliedArgb & 0xff) * kernelSum;
            }
            return resultPixel(alpha * kernelSum, red, green, blue, alpha);
        }

        private int resultPixel(double alphaSum, double redSum, double greenSum, double blueSum,
                int centerAlpha) {
            int alpha;
            int red;
            int green;
            int blue;
            if (preserveAlpha) {
                alpha = centerAlpha;
                red = multiplyAlpha(toRgbRange(redSum / divisor + bias * 255), alpha);
                green = multiplyAlpha(toRgbRange(greenSum / divisor + bias * 255), alpha);
                blue = multiplyAlpha(toRgbRange(blueSum / divisor + bias * 255), alpha);
            } else {
                double resultAlpha = alphaSum / divisor + bias * 255;
                alpha = toRgbRange(resultAlpha);
                red = Math.min(alpha, toRgbRange(redSum / divisor + bias * resultAlpha));
                green = Math.min(alpha, toRgbRange(greenSum / divisor + bias * resultAlpha));
                blue = Math.min(alpha, toRgbRange(blueSum / divisor + bias * resultAlpha));
            }
            return alpha << 24 | red << 16 | green << 8 | blue;
        }

        private static int multiplyAlpha(int component, int alpha) {
            return ColorUtil.div255(component * alpha);
        }

        private static int bilinearSample(int @NotNull [] pixels, int offset, int stride,
                int width, int height, double x, double y) {
            int x0 = (int) Math.floor(x);
            int y0 = (int) Math.floor(y);
            int x1 = Math.min(x0 + 1, width - 1);
            int y1 = Math.min(y0 + 1, height - 1);
            x0 = Math.max(0, x0);
            y0 = Math.max(0, y0);
            double fractionX = x - Math.floor(x);
            double fractionY = y - Math.floor(y);
            int topLeft = pixels[offset + y0 * stride + x0];
            int topRight = pixels[offset + y0 * stride + x1];
            int bottomLeft = pixels[offset + y1 * stride + x0];
            int bottomRight = pixels[offset + y1 * stride + x1];
            int alpha = interpolate(topLeft >>> 24, topRight >>> 24,
                    bottomLeft >>> 24, bottomRight >>> 24, fractionX, fractionY);
            int red = interpolate((topLeft >>> 16) & 0xff, (topRight >>> 16) & 0xff,
                    (bottomLeft >>> 16) & 0xff, (bottomRight >>> 16) & 0xff, fractionX, fractionY);
            int green = interpolate((topLeft >>> 8) & 0xff, (topRight >>> 8) & 0xff,
                    (bottomLeft >>> 8) & 0xff, (bottomRight >>> 8) & 0xff, fractionX, fractionY);
            int blue = interpolate(topLeft & 0xff, topRight & 0xff,
                    bottomLeft & 0xff, bottomRight & 0xff, fractionX, fractionY);
            return alpha << 24 | red << 16 | green << 8 | blue;
        }

        private static int interpolate(int topLeft, int topRight, int bottomLeft, int bottomRight,
                double fractionX, double fractionY) {
            double top = topLeft + fractionX * (topRight - topLeft);
            double bottom = bottomLeft + fractionX * (bottomRight - bottomLeft);
            return toRgbRange(top + fractionY * (bottom - top));
        }
    }
}
