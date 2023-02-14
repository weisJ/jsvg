/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;

public final class ImageUtil {
    private ImageUtil() {}

    public static @NotNull BufferedImage createCompatibleTransparentImage(@NotNull Graphics2D g, double width,
            double height) {
        AffineTransform at = g.getTransform();
        return new BufferedImage(
                (int) Math.ceil(GeometryUtil.scaleXOfTransform(at) * width),
                (int) Math.ceil(GeometryUtil.scaleYOfTransform(at) * height), BufferedImage.TYPE_INT_ARGB_PRE);
    }

    public static @NotNull BufferedImage createLuminosityBuffer(@NotNull AffineTransform at, double width,
            double height) {
        return new BufferedImage(
                (int) Math.ceil(GeometryUtil.scaleXOfTransform(at) * width),
                (int) Math.ceil(GeometryUtil.scaleYOfTransform(at) * height), BufferedImage.TYPE_BYTE_GRAY);
    }

    public static int[] getINT_RGBA_DataBank(@NotNull WritableRaster raster) {
        DataBufferInt dstDB = (DataBufferInt) raster.getDataBuffer();
        return dstDB.getBankData()[0];
    }

    public static int getINT_RGBA_DataOffset(@NotNull WritableRaster raster) {
        DataBufferInt dstDB = (DataBufferInt) raster.getDataBuffer();
        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) raster.getSampleModel();
        return dstDB.getOffset() + sppsm.getOffset(
                raster.getMinX() - raster.getSampleModelTranslateX(),
                raster.getMinY() - raster.getSampleModelTranslateY());
    }

    public static int getINT_RGBA_DataAdjust(@NotNull WritableRaster raster) {
        return ((SinglePixelPackedSampleModel) raster.getSampleModel()).getScanlineStride() - raster.getWidth();
    }
}
