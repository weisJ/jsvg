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
package com.github.weisj.jsvg.util;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.function.IntUnaryOperator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;

public final class ImageUtil {
    public static final @NotNull ColorModel LINEAR_RGB_COLOR_MODEL = new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), 32,
            0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000, false, DataBuffer.TYPE_INT);

    private ImageUtil() {}

    public enum Premultiplied {
        Yes,
        No
    }

    public static @NotNull BufferedImage createCompatibleDestImage(@NotNull BufferedImage src,
            @Nullable ColorModel dstCM) {
        if (dstCM == null) {
            dstCM = src.getColorModel();
        }
        return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()),
                dstCM.isAlphaPremultiplied(), null);
    }

    public static @NotNull BufferedImage createCompatibleTransparentImage(@NotNull Output output,
            double width, double height) {
        return createCompatibleTransparentImage(output.transform(), width, height);
    }

    public static @NotNull BufferedImage createCompatibleTransparentImage(int width, int height) {
        return createCompatibleTransparentImage(width, height, Premultiplied.No);
    }

    public static @NotNull BufferedImage createCompatibleTransparentImage(int width, int height,
            Premultiplied preMultiplied) {
        int type = preMultiplied == Premultiplied.Yes ? TYPE_INT_ARGB_PRE : TYPE_INT_ARGB;
        return new BufferedImage(width, height, type);
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

    /**
     * Maps packed ARGB samples in place, retaining the raster's origin and scanline stride.
     * The caller owns the pixels and is responsible for their color space and premultiplication.
     * This changes samples only; it does not update an image's ColorModel metadata.
     */
    public static void mapPixels(@NotNull WritableRaster raster, @NotNull IntUnaryOperator operation) {
        if (!is_INT_PACK_Data(raster.getSampleModel(), true)) {
            throw new IllegalArgumentException("Raster must use packed integer ARGB samples");
        }
        int[] pixels = getINT_RGBA_DataBank(raster);
        int offset = getINT_RGBA_DataOffset(raster);
        int stride = getINT_RGBA_ScanlineStride(raster);
        for (int y = 0; y < raster.getHeight(); y++) {
            int end = offset + raster.getWidth();
            for (int i = offset; i < end; i++) {
                pixels[i] = operation.applyAsInt(pixels[i]);
            }
            offset += stride;
        }
    }

    public static @NotNull BufferedImage copy(@NotNull RenderContext context, @NotNull ImageProducer producer,
            Premultiplied premultiplied) {
        return toBufferedImage(context.platformSupport().createImage(producer), premultiplied);
    }

    public static @NotNull BufferedImage toBufferedImage(@NotNull Image img) {
        return toBufferedImage(img, Premultiplied.No);
    }

    public static @NotNull BufferedImage toBufferedImage(@NotNull Image img, Premultiplied premultiplied) {
        BufferedImage bufferedImage = createCompatibleTransparentImage(img.getWidth(null), img.getHeight(null),
                premultiplied);
        Graphics2D g = GraphicsUtil.createGraphics(bufferedImage);
        g.drawImage(img, null, null);
        g.dispose();
        return bufferedImage;
    }

    public static @NotNull ColorModel coerceData(@NotNull WritableRaster wr, @NotNull ColorModel cm,
            boolean newAlphaPreMultiplied) {
        if (!cm.hasAlpha()) return cm;
        if (cm.isAlphaPremultiplied() == newAlphaPreMultiplied) return cm;
        return cm.coerceData(wr, newAlphaPreMultiplied);
    }
}
