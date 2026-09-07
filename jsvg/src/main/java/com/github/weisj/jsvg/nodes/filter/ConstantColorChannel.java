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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;

/**
 * A uniform, non-premultiplied ARGB image. Primitive subregions are applied by {@link Channel#clip},
 * so a partially clipped result is no longer a constant-color channel.
 */
public final class ConstantColorChannel implements Channel, PixelProvider {
    private final int width;
    private final int height;
    private final int color;
    private @Nullable ImageProducer producer;
    private @Nullable BufferedImage image;

    public ConstantColorChannel(int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public int color() {
        return color;
    }

    public @NotNull ConstantColorChannel withColor(int color) {
        if (this.color == color) return this;
        return new ConstantColorChannel(width, height, color);
    }

    @Override
    public @NotNull ImageProducer producer() {
        if (producer == null) {
            int[] row = new int[width];
            Arrays.fill(row, color);
            // Every image row reads the same pixels; no full image buffer is needed.
            producer = new MemoryImageSource(width, height, ColorModel.getRGBdefault(), row, 0, 0);
        }
        return producer;
    }

    @Override
    public @NotNull Image toImage(@NotNull RenderContext context) {
        if (image == null) {
            image = toBufferedImageNonAliased(context);
        }
        return image;
    }

    @Override
    public @NotNull BufferedImage toBufferedImageNonAliased(@NotNull RenderContext context) {
        BufferedImage result = ImageUtil.createCompatibleTransparentImage(width, height);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(color, true));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return result;
    }

    @Override
    public @NotNull Channel applyFilter(@NotNull ImageFilter filter) {
        return new ImageProducerChannel(new FilteredImageSource(producer(), filter));
    }

    @Override
    public @NotNull PixelProvider pixels(@NotNull RenderContext context) {
        return this;
    }

    @Override
    public int pixelAt(double x, double y) {
        return x >= 0 && y >= 0 && x < width && y < height ? color : 0;
    }

    @Override
    public @NotNull Channel alphaChannel() {
        return withColor(color & 0xff000000);
    }
}
