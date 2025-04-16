/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.RenderContext;

public class ImageProducerChannel implements Channel, PixelProvider {

    private final @NotNull ImageProducer producer;

    private BufferedImage rasterBuffer;
    private WritableRaster raster;

    public ImageProducerChannel(@NotNull ImageProducer producer) {
        this.producer = producer;
    }

    @Override
    public @NotNull ImageProducer producer() {
        return producer;
    }

    @Override
    public @NotNull Channel applyFilter(@NotNull ImageFilter filter) {
        return new ImageProducerChannel(new FilteredImageSource(producer, filter));
    }

    @Override
    public @NotNull Image toImage(@NotNull RenderContext context) {
        return rasterBuffer(context);
    }

    private @NotNull BufferedImage rasterBuffer(@NotNull RenderContext context) {
        if (rasterBuffer == null) {
            rasterBuffer = Channel.makeNonAliased(context.platformSupport().createImage(producer()));
        }
        return rasterBuffer;
    }

    @Override
    public @NotNull PixelProvider pixels(@NotNull RenderContext context) {
        if (raster == null) {
            raster = rasterBuffer(context).getRaster();
        }
        return this;
    }

    @Override
    public int pixelAt(double x, double y) {
        int[] rgb = raster.getPixel((int) x, (int) y, (int[]) null);
        return (rgb[3] << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }
}
