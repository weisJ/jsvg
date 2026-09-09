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
package com.github.weisj.jsvg.util;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.function.IntBinaryOperator;

import org.jetbrains.annotations.NotNull;

public final class NormalizedAlphaSampler {
    private final int maxX;
    private final int maxY;
    private final @NotNull IntBinaryOperator alpha;

    public NormalizedAlphaSampler(@NotNull BufferedImage image) {
        maxX = image.getWidth() - 1;
        maxY = image.getHeight() - 1;
        Raster raster = image.getRaster();
        if (!image.getColorModel().hasAlpha()) {
            alpha = (x, y) -> 255;
        } else if (ImageUtil.is_INT_PACK_Data(raster.getSampleModel(), true)) {
            int[] pixels = ImageUtil.getINT_RGBA_DataBank(raster);
            int offset = ImageUtil.getINT_RGBA_DataOffset(raster);
            int stride = ImageUtil.getINT_RGBA_ScanlineStride(raster);
            alpha = (x, y) -> pixels[offset + y * stride + x] >>> 24;
        } else {
            alpha = (x, y) -> image.getRGB(x, y) >>> 24;
        }
    }

    /**
     * Returns the alpha value at an integer pixel coordinate within the image bounds.
     */
    public double at(int x, int y) {
        return alpha.applyAsInt(x, y) / 255.0;
    }

    /**
     * Bilinearly interpolates alpha, clamping coordinates to the image bounds.
     */
    public double at(double x, double y) {
        double clampedX = Math.max(0, Math.min(maxX, x));
        double clampedY = Math.max(0, Math.min(maxY, y));
        int x0 = (int) clampedX;
        int y0 = (int) clampedY;
        double tx = clampedX - x0;
        double ty = clampedY - y0;

        double a00 = alpha.applyAsInt(x0, y0);
        if (tx == 0 && ty == 0) return a00 / 255.0;

        int x1 = Math.min(maxX, x0 + 1);
        int y1 = Math.min(maxY, y0 + 1);
        double a10 = alpha.applyAsInt(x1, y0);
        double a01 = alpha.applyAsInt(x0, y1);
        double a11 = alpha.applyAsInt(x1, y1);
        double a0 = a00 + (a10 - a00) * tx;
        double a1 = a01 + (a11 - a01) * tx;
        return (a0 + (a1 - a0) * ty) / 255.0;
    }
}
