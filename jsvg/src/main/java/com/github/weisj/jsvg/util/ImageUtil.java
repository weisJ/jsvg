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
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
    }


    public static @NotNull BufferedImage createCompatibleTransparentImage(@Nullable AffineTransform at, double width,
            double height) {
        return new BufferedImage(
                (int) Math.ceil(GeometryUtil.scaleXOfTransform(at) * width),
                (int) Math.ceil(GeometryUtil.scaleYOfTransform(at) * height), BufferedImage.TYPE_INT_ARGB_PRE);
    }

    public static @NotNull BufferedImage createLuminosityBuffer(@Nullable AffineTransform at, double width,
            double height) {
        return new BufferedImage(
                (int) Math.ceil(GeometryUtil.scaleXOfTransform(at) * width),
                (int) Math.ceil(GeometryUtil.scaleYOfTransform(at) * height), BufferedImage.TYPE_BYTE_GRAY);
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
        Image img = context.platformSupport().createImage(producer);
        BufferedImage bufferedImage = createCompatibleTransparentImage((AffineTransform) null,
                img.getWidth(null), img.getHeight(null));
        Graphics2D g = GraphicsUtil.createGraphics(bufferedImage);
        g.drawImage(img, null, null);
        g.dispose();
        return bufferedImage;
    }
}
