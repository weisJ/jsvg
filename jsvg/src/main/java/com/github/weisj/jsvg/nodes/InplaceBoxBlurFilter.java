/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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
package com.github.weisj.jsvg.nodes;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.util.ImageUtil;

/*
 * NOTICE: This file contains adapted code from the Batik project:
 * https://xmlgraphics.apache.org/batik/ by the Apache Software Foundation. It is licensed under the
 * Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
public final class InplaceBoxBlurFilter {

    private InplaceBoxBlurFilter() {}

    public static void horizontalPass(@NotNull Raster src, @NotNull WritableRaster dst,
            int skipX, int skipY, int boxSize, int loc) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (width < 2 * skipX + boxSize) return;
        if (height < 2 * skipY) return;

        int srcScanStride = ImageUtil.getINT_RGBA_ScanlineStride(src);
        int dstScanStride = ImageUtil.getINT_RGBA_ScanlineStride(dst);

        int srcOff = ImageUtil.getINT_RGBA_DataOffset(src);
        int dstOff = ImageUtil.getINT_RGBA_DataOffset(dst);

        int[] srcPixels = ImageUtil.getINT_RGBA_DataBank(src);
        int[] dstPixels = ImageUtil.getINT_RGBA_DataBank(dst);

        int[] buffer = new int[boxSize];
        int curr, prev;
        int scale = (1 << 24) / boxSize;

        for (int y = skipY; y < (height - skipY); y++) {
            int sp = srcOff + y * srcScanStride;
            int dp = dstOff + y * dstScanStride;
            int rowEnd = sp + (width - skipX);

            int k = 0;
            int sumA = 0;
            int sumR = 0;
            int sumG = 0;
            int sumB = 0;

            sp += skipX;
            int end = sp + boxSize;

            while (sp < end) {
                curr = buffer[k] = srcPixels[sp];
                sumA += (curr >>> 24);
                sumR += (curr >> 16) & 0xFF;
                sumG += (curr >> 8) & 0xFF;
                sumB += (curr) & 0xFF;
                k++;
                sp++;
            }

            dp += skipX + loc;
            prev = dstPixels[dp] = (((sumA * scale) & 0xFF000000) |
                    (((sumR * scale) & 0xFF000000) >>> 8) |
                    (((sumG * scale) & 0xFF000000) >>> 16) |
                    (((sumB * scale) & 0xFF000000) >>> 24));
            dp++;
            k = 0;
            while (sp < rowEnd) {
                curr = buffer[k];
                if (curr == srcPixels[sp]) {
                    dstPixels[dp] = prev;
                } else {
                    sumA -= (curr >>> 24);
                    sumR -= (curr >> 16) & 0xFF;
                    sumG -= (curr >> 8) & 0xFF;
                    sumB -= (curr) & 0xFF;

                    curr = buffer[k] = srcPixels[sp];

                    sumA += (curr >>> 24);
                    sumR += (curr >> 16) & 0xFF;
                    sumG += (curr >> 8) & 0xFF;
                    sumB += (curr) & 0xFF;
                    prev = dstPixels[dp] = (((sumA * scale) & 0xFF000000) |
                            (((sumR * scale) & 0xFF000000) >>> 8) |
                            (((sumG * scale) & 0xFF000000) >>> 16) |
                            (((sumB * scale) & 0xFF000000) >>> 24));
                }
                k = (k + 1) % boxSize;
                sp++;
                dp++;
            }
        }
    }

    public static void verticalPass(@NotNull Raster src, @NotNull WritableRaster dst,
            int skipX, int skipY, int boxSize, int loc) {
        int w = src.getWidth();
        int h = src.getHeight();

        if (w < 2 * skipX) return;
        if (h < 2 * skipY + boxSize) return;

        int srcScanStride = ImageUtil.getINT_RGBA_ScanlineStride(src);
        int dstScanStride = ImageUtil.getINT_RGBA_ScanlineStride(dst);

        int srcOff = ImageUtil.getINT_RGBA_DataOffset(src);
        int dstOff = ImageUtil.getINT_RGBA_DataOffset(dst);

        int[] srcPixels = ImageUtil.getINT_RGBA_DataBank(src);
        int[] dstPixels = ImageUtil.getINT_RGBA_DataBank(dst);

        int[] buffer = new int[boxSize];
        int curr, prev;

        final int scale = (1 << 24) / boxSize;

        for (int x = skipX; x < (w - skipX); x++) {
            int sp = srcOff + x;
            int dp = dstOff + x;
            int colEnd = sp + (h - skipY) * srcScanStride;

            int k = 0;
            int sumA = 0;
            int sumR = 0;
            int sumG = 0;
            int sumB = 0;

            sp += skipY * srcScanStride;
            int end = sp + (boxSize * srcScanStride);

            while (sp < end) {
                curr = buffer[k] = srcPixels[sp];
                sumA += (curr >>> 24);
                sumR += (curr >> 16) & 0xFF;
                sumG += (curr >> 8) & 0xFF;
                sumB += (curr) & 0xFF;
                k++;
                sp += srcScanStride;
            }


            dp += (skipY + loc) * dstScanStride;
            prev = dstPixels[dp] = (((sumA * scale) & 0xFF000000) |
                    (((sumR * scale) & 0xFF000000) >>> 8) |
                    (((sumG * scale) & 0xFF000000) >>> 16) |
                    (((sumB * scale) & 0xFF000000) >>> 24));
            dp += dstScanStride;
            k = 0;
            while (sp < colEnd) {
                curr = buffer[k];
                if (curr == srcPixels[sp]) {
                    dstPixels[dp] = prev;
                } else {
                    sumA -= (curr >>> 24);
                    sumR -= (curr >> 16) & 0xFF;
                    sumG -= (curr >> 8) & 0xFF;
                    sumB -= (curr) & 0xFF;

                    curr = buffer[k] = srcPixels[sp];

                    sumA += (curr >>> 24);
                    sumR += (curr >> 16) & 0xFF;
                    sumG += (curr >> 8) & 0xFF;
                    sumB += (curr) & 0xFF;
                    prev = dstPixels[dp] = (((sumA * scale) & 0xFF000000) |
                            (((sumR * scale) & 0xFF000000) >>> 8) |
                            (((sumG * scale) & 0xFF000000) >>> 16) |
                            (((sumB * scale) & 0xFF000000) >>> 24));
                }
                k = (k + 1) % boxSize;
                sp += srcScanStride;
                dp += dstScanStride;
            }
        }
    }
}
