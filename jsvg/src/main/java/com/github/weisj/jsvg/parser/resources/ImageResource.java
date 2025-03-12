/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jannis Weis
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
package com.github.weisj.jsvg.parser.resources;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.impl.RenderContext;

public class ImageResource implements RenderableResource {
    private final @NotNull BufferedImage image;

    public ImageResource(@NotNull BufferedImage image) {
        this.image = image;
    }

    @Override
    public @NotNull FloatSize intrinsicSize(@NotNull RenderContext context) {
        return new FloatSize(
                image.getWidth(context.platformSupport().imageObserver()),
                image.getHeight(context.platformSupport().imageObserver()));
    }

    @Override
    public void render(@NotNull Output output, @NotNull RenderContext context, @NotNull AffineTransform imgTransform) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        Object imageAntialiasing = output.renderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING);
        if (imageAntialiasing == SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF) {
            output.drawImage(image, imgTransform, context.platformSupport().imageObserver());
        } else {
            output.applyTransform(imgTransform);
            Rectangle imgRect = new Rectangle(0, 0, imgWidth, imgHeight);
            // Painting using a TexturePaint allows for anti-aliased edges with a nontrivial transform
            output.setPaint(new TexturePaint(image, imgRect));
            output.fillShape(imgRect);
        }
    }
}
