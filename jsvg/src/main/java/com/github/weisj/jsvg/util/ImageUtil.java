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
package com.github.weisj.jsvg.util;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class ImageUtil {

    private ImageUtil() {}

    public static @NotNull BufferedImage createCompatibleTransparentImage(@NotNull Output output,
            double width, double height) {
        return createCompatibleTransparentImage(output.transform(), width, height);
    }

    public static @NotNull BufferedImage createCompatibleTransparentImage(int width, int height) {
        return new BufferedImage(width, height, TYPE_INT_ARGB_PRE);
    }

    public static @NotNull BufferedImage createCompatibleTransparentImage(@Nullable AffineTransform at, double width,
            double height) {
        return createCompatibleTransparentImage(
                (int) Math.ceil(GeometryUtil.scaleXOfTransform(at) * width),
                (int) Math.ceil(GeometryUtil.scaleYOfTransform(at) * height));
    }

    public static @NotNull BufferedImage createLuminosityBuffer(@Nullable AffineTransform at, double width,
            double height) {
        return new BufferedImage(
                (int) Math.ceil(GeometryUtil.scaleXOfTransform(at) * width),
                (int) Math.ceil(GeometryUtil.scaleYOfTransform(at) * height), BufferedImage.TYPE_BYTE_GRAY);
    }

    public static boolean is_INT_PACK_Data(@NotNull SampleModel sm, boolean requireAlpha) {
        if (!(sm instanceof SinglePixelPackedSampleModel)) return false;
        if (sm.getDataType() != DataBuffer.TYPE_INT) return false;

        int[] masks = ((SinglePixelPackedSampleModel) sm).getBitMasks();
        if (masks.length == 3) {
            if (requireAlpha) {
                return false;
            }
        } else if (masks.length != 4) {
            return false;
        }

        if (masks[0] != 0x00ff0000) return false;
        if (masks[1] != 0x0000ff00) return false;
        if (masks[2] != 0x000000ff) return false;
        return (masks.length != 4) || (masks[3] == 0xff000000);
    }

    public static int[] getINT_RGBA_DataBank(@NotNull Raster raster) {
        DataBufferInt dstDB = (DataBufferInt) raster.getDataBuffer();
        return dstDB.getBankData()[0];
    }

    public static int getINT_RGBA_DataOffset(@NotNull Raster raster) {
        DataBufferInt dstDB = (DataBufferInt) raster.getDataBuffer();
        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) raster.getSampleModel();
        return dstDB.getOffset() + sppsm.getOffset(
                raster.getMinX() - raster.getSampleModelTranslateX(),
                raster.getMinY() - raster.getSampleModelTranslateY());
    }

    public static int getINT_RGBA_DataAdjust(@NotNull Raster raster) {
        return getINT_RGBA_ScanlineStride(raster) - raster.getWidth();
    }

    public static int getINT_RGBA_ScanlineStride(@NotNull Raster raster) {
        return ((SinglePixelPackedSampleModel) raster.getSampleModel()).getScanlineStride();
    }

    public static @NotNull BufferedImage copy(@NotNull RenderContext context, @NotNull ImageProducer producer) {
        return toBufferedImage(context.platformSupport().createImage(producer));
    }

    public static @NotNull BufferedImage toBufferedImage(@NotNull Image img) {
        BufferedImage bufferedImage = createCompatibleTransparentImage((AffineTransform) null,
                img.getWidth(null), img.getHeight(null));
        Graphics2D g = GraphicsUtil.createGraphics(bufferedImage);
        g.drawImage(img, null, null);
        g.dispose();
        return bufferedImage;
    }

    private static @NotNull ColorModel coerceColorModel(@NotNull ColorModel cm, boolean newAlphaPreMultiplied) {
        if (cm.isAlphaPremultiplied() == newAlphaPreMultiplied) return cm;

        // Easiest way to build proper color-model for new Alpha state...
        // Eventually this should switch on known ColorModel types and
        // only fall back on this hack when the CM type is unknown.
        WritableRaster wr = cm.createCompatibleWritableRaster(1, 1);
        return cm.coerceData(wr, newAlphaPreMultiplied);
    }

    public static @NotNull ColorModel coerceData(@NotNull WritableRaster wr, @NotNull ColorModel cm,
            boolean newAlphaPreMultiplied) {
        if (!cm.hasAlpha()) return cm;
        if (cm.isAlphaPremultiplied() == newAlphaPreMultiplied) return cm;

        if (newAlphaPreMultiplied) {
            multiplyAlpha(wr);
        } else {
            divideAlpha(wr);
        }

        return coerceColorModel(cm, newAlphaPreMultiplied);
    }

    public static void multiplyAlpha(@NotNull WritableRaster wr) {
        if (is_INT_PACK_Data(wr.getSampleModel(), true)) {
            multiply_INT_PACK_Data(wr);
        } else {
            int[] pixel = null;
            int bands = wr.getNumBands();
            float norm = 1.0f / 255f;
            int x0 = wr.getMinX();
            int x1 = x0 + wr.getWidth();
            int y0 = wr.getMinY();
            int y1 = y0 + wr.getHeight();
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    pixel = wr.getPixel(x, y, pixel);
                    int a = pixel[bands - 1];
                    if ((a >= 0) && (a < 255)) {
                        float alpha = a * norm;
                        for (int b = 0; b < bands - 1; b++) {
                            pixel[b] = (int) (pixel[b] * alpha + 0.5f);
                        }
                        wr.setPixel(x, y, pixel);
                    }
                }
            }
        }
    }

    public static void divideAlpha(@NotNull WritableRaster wr) {
        if (is_INT_PACK_Data(wr.getSampleModel(), true)) {
            divide_INT_PACK_Data(wr);
        } else {
            int bands = wr.getNumBands();
            int[] pixel = null;

            int x0 = wr.getMinX();
            int x1 = x0 + wr.getWidth();
            int y0 = wr.getMinY();
            int y1 = y0 + wr.getHeight();
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    pixel = wr.getPixel(x, y, pixel);
                    int a = pixel[bands - 1];
                    if ((a > 0) && (a < 255)) {
                        float alpha = 255 / (float) a;
                        for (int b = 0; b < bands - 1; b++) {
                            pixel[b] = (int) (pixel[b] * alpha + 0.5f);
                        }
                        wr.setPixel(x, y, pixel);
                    }
                }
            }
        }
    }

    private static void divide_INT_PACK_Data(@NotNull WritableRaster wr) {
        final int width = wr.getWidth();
        final int scanStride = getINT_RGBA_DataAdjust(wr);
        final int base = getINT_RGBA_DataOffset(wr);
        final int[] pixels = getINT_RGBA_DataBank(wr);

        for (int y = 0; y < wr.getHeight(); y++) {
            int sp = base + y * scanStride;
            final int end = sp + width;
            while (sp < end) {
                int pixel = pixels[sp];
                int a = pixel >>> 24;
                if (a == 0) {
                    pixels[sp] = 0x00FFFFFF;
                } else if (a < 255) {
                    int aFP = (0x00FF0000 / a);
                    pixels[sp] = ((a << 24) |
                            ((((pixel & 0xFF0000) >> 16) * aFP) & 0xFF0000) |
                            (((((pixel & 0x00FF00) >> 8) * aFP) & 0xFF0000) >> 8) |
                            (((pixel & 0x0000FF) * aFP) & 0xFF0000) >> 16);
                }
                sp++;
            }
        }
    }

    private static void multiply_INT_PACK_Data(@NotNull WritableRaster wr) {
        final int width = wr.getWidth();
        final int scanStride = getINT_RGBA_DataAdjust(wr);
        final int base = getINT_RGBA_DataOffset(wr);
        final int[] pixels = getINT_RGBA_DataBank(wr);

        for (int y = 0; y < wr.getHeight(); y++) {
            int sp = base + y * scanStride;
            final int end = sp + width;
            while (sp < end) {
                int pixel = pixels[sp];
                int a = pixel >>> 24;
                if (a < 255) { // this does NOT include a == 255 (0xff) !
                    pixels[sp] = ((a << 24) |
                            ((((pixel & 0xFF0000) * a) >> 8) & 0xFF0000) |
                            ((((pixel & 0x00FF00) * a) >> 8) & 0x00FF00) |
                            ((((pixel & 0x0000FF) * a) >> 8) & 0x0000FF));
                }
                sp++;
            }
        }
    }

}
