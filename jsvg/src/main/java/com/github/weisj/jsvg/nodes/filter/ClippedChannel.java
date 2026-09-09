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
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.ShapeUtil;

final class ClippedChannel implements Channel {
    private final @NotNull Channel input;
    private final @NotNull Shape clip;
    private final @NotNull Rectangle bounds;
    private final boolean empty;
    private @Nullable ImageProducer producer;
    private @Nullable BufferedImage image;

    ClippedChannel(@NotNull Channel input, @NotNull Shape clip, int width, int height) {
        this.input = input;
        this.clip = clip;
        this.bounds = new Rectangle(0, 0, width, height);
        this.empty = !clip.intersects(bounds);
    }

    @NotNull
    Channel clipTo(@NotNull Shape region) {
        if (clip.equals(region) || region.contains(clip.getBounds2D())) return this;
        Shape intersection = ShapeUtil.intersect(clip, region, true, true);
        return new ClippedChannel(input, intersection, bounds.width, bounds.height);
    }

    @Override
    public @NotNull ImageProducer producer() {
        if (producer != null) return producer;
        if (empty) {
            producer = new MemoryImageSource(bounds.width, bounds.height,
                    ColorModel.getRGBdefault(), new int[bounds.width], 0, 0);
        } else if (clip.contains(bounds)) {
            producer = input.producer();
        } else {
            producer = new FilteredImageSource(input.producer(), new PrimitiveRegionClip(clip));
        }
        return producer;
    }

    @Override
    public @NotNull Channel applyFilter(@NotNull ImageFilter filter) {
        return new ImageProducerChannel(new FilteredImageSource(producer(), filter));
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
        if (empty) return ImageUtil.createCompatibleTransparentImage(bounds.width, bounds.height);
        BufferedImage result = input.toBufferedImageNonAliased(context);
        if (clip.contains(bounds)) return result;
        Path2D complement = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        complement.append(bounds, false);
        complement.append(clip, false);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.fill(complement);
        graphics.dispose();
        return result;
    }

    @Override
    public @NotNull PixelProvider pixels(@NotNull RenderContext context) {
        if (empty) return (x, y) -> 0;
        PixelProvider pixels = input.pixels(context);
        return (x, y) -> {
            if (bounds.contains(x, y) && clip.contains(x, y)) return pixels.pixelAt(x, y);
            return 0;
        };
    }
}
