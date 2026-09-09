/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.util;

import static com.github.weisj.jsvg.util.ColorUtil.div255;

import java.awt.*;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Porter-Duff compositing in linearRGB for packed integer images. Colors are read as 8-bit
 * sRGB through their ColorModel and written using the destination ColorModel; alpha is coverage.
 */
public final class LinearRGBComposite implements Composite {
    public static final @NotNull LinearRGBComposite SrcOver = new LinearRGBComposite(AlphaComposite.SRC_OVER);
    public static final @NotNull LinearRGBComposite SrcAtop = new LinearRGBComposite(AlphaComposite.SRC_ATOP);
    public static final @NotNull LinearRGBComposite Xor = new LinearRGBComposite(AlphaComposite.XOR);

    private final int rule;

    private LinearRGBComposite(int rule) {
        this.rule = rule;
    }

    @Override
    public @NotNull CompositeContext createContext(@NotNull ColorModel srcColorModel,
            @NotNull ColorModel dstColorModel,
            @Nullable RenderingHints hints) {
        if (!(srcColorModel instanceof DirectColorModel) || !(dstColorModel instanceof DirectColorModel)
                || srcColorModel.getTransferType() != DataBuffer.TYPE_INT
                || dstColorModel.getTransferType() != DataBuffer.TYPE_INT) {
            throw new RasterFormatException("Incompatible color models");
        }
        return new CompositeContext() {
            @Override
            public void dispose() {
                // No resources to release.
            }

            @Override
            public void compose(@NotNull Raster src, @NotNull Raster dstIn, @NotNull WritableRaster dstOut) {
                int width = Math.min(src.getWidth(), dstIn.getWidth());
                int height = Math.min(src.getHeight(), dstIn.getHeight());
                int[] source = new int[width];
                int[] destination = new int[width];
                int[] result = new int[1];
                for (int y = 0; y < height; y++) {
                    src.getDataElements(src.getMinX(), src.getMinY() + y, width, 1, source);
                    dstIn.getDataElements(dstIn.getMinX(), dstIn.getMinY() + y, width, 1, destination);
                    for (int x = 0; x < width; x++) {
                        int color = blend(srcColorModel.getRGB(source[x]), dstColorModel.getRGB(destination[x]));
                        dstColorModel.getDataElements(color, result);
                        destination[x] = result[0];
                    }
                    dstOut.setDataElements(dstOut.getMinX(), dstOut.getMinY() + y, width, 1, destination);
                }
            }
        };
    }

    private int blend(int src, int dst) {
        int srcAlpha = src >>> 24;
        int dstAlpha = dst >>> 24;
        if (srcAlpha == 0) return dst;
        if (dstAlpha == 0) return rule == AlphaComposite.SRC_ATOP ? 0 : src;
        if (rule == AlphaComposite.SRC_OVER && srcAlpha == 255) return src;

        int srcFactor = rule == AlphaComposite.SRC_OVER ? 255
                : rule == AlphaComposite.SRC_ATOP ? dstAlpha : 255 - dstAlpha;
        int srcWeight = srcAlpha * srcFactor;
        int dstWeight = dstAlpha * (255 - srcAlpha);
        int weight = srcWeight + dstWeight;
        if (weight == 0) return 0;
        // Round alpha contributions as AlphaComposite does, independently of the color space.
        int alpha = div255(srcWeight) + div255(dstWeight);
        // Keep the premultiplied color sums until division by alpha. Rounding linear
        // premultiplied bytes first loses precision, particularly at low opacity.
        int red = blendBand((src >> 16) & 255, (dst >> 16) & 255, srcWeight, dstWeight, weight);
        int green = blendBand((src >> 8) & 255, (dst >> 8) & 255, srcWeight, dstWeight, weight);
        int blue = blendBand(src & 255, dst & 255, srcWeight, dstWeight, weight);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int blendBand(int src, int dst, int srcWeight, int dstWeight, int weight) {
        int linear = (ColorUtil.sRGBtoLinearRGBBand(src) * srcWeight
                + ColorUtil.sRGBtoLinearRGBBand(dst) * dstWeight + weight / 2) / weight;
        return ColorUtil.linearRGBtoSRGBBand(linear);
    }
}
