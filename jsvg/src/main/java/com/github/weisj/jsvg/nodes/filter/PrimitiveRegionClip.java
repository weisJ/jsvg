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

import java.awt.Shape;
import java.awt.image.ColorModel;
import java.awt.image.ImageFilter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PrimitiveRegionClip extends ImageFilter {
    private final @NotNull Shape clip;
    private int @Nullable [] row;

    PrimitiveRegionClip(@NotNull Shape clip) {
        this.clip = clip;
    }

    @Override
    public void setColorModel(ColorModel model) {
        consumer.setColorModel(ColorModel.getRGBdefault());
    }

    private int @NotNull [] row(int width) {
        if (row == null || row.length < width) {
            row = new int[width];
        }
        return row;
    }

    @Override
    public void setPixels(int x, int y, int width, int height, ColorModel model, int[] pixels, int offset,
            int scansize) {
        if (clip.contains(x, y, width, height)) {
            consumer.setPixels(x, y, width, height, model, pixels, offset, scansize);
            return;
        }
        int[] rowData = row(width);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                // Retain every pixel that partly intersects the primitive subregion.
                rowData[i] = clip.intersects((double) x + i, (double) y + j, 1, 1)
                        ? model.getRGB(pixels[offset + i])
                        : 0;
            }
            consumer.setPixels(x, y + j, width, 1, ColorModel.getRGBdefault(), rowData, 0, width);
            offset += scansize;
        }
    }

    @Override
    public void setPixels(int x, int y, int width, int height, ColorModel model, byte[] pixels, int offset,
            int scansize) {
        if (clip.contains(x, y, width, height)) {
            consumer.setPixels(x, y, width, height, model, pixels, offset, scansize);
            return;
        }
        int[] rowData = row(width);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                rowData[i] = clip.intersects((double) x + i, (double) y + j, 1, 1)
                        ? model.getRGB(pixels[offset + i] & 0xff)
                        : 0;
            }
            consumer.setPixels(x, y + j, width, 1, ColorModel.getRGBdefault(), rowData, 0, width);
            offset += scansize;
        }
    }
}
