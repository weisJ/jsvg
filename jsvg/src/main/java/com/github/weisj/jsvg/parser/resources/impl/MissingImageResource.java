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
package com.github.weisj.jsvg.parser.resources.impl;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.impl.RenderContext;
import com.github.weisj.jsvg.util.LazyProvider;

public final class MissingImageResource implements RenderableResource {
    private static final int SIZE = 100;
    private static final LazyProvider<SVGDocument> missingImage = new LazyProvider<>(() -> {
        SVGLoader loader = new SVGLoader();
        return loader.load(new ByteArrayInputStream(
                ("<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" x=\"0px\"\n" +
                        "     y=\"0px\" viewBox=\"0 0 16 16\">\n" +
                        "    <g fill=\"#9AA7B0\">\n" +
                        "        <rect x=\"1\" y=\"1\" width=\"1\" height=\"11.3\"/>\n" +
                        "        <rect x=\"2\" y=\"1\" width=\"12\" height=\"1\"/>\n" +
                        "        <path d=\"M7,12.5c0-0.1,0-0.1,0-0.2H1v1h6.1C7,13,7,12.8,7,12.5z\"/>\n" +
                        "        <path d=\"M15,9.7V1h-1v7.8C14.4,9,14.7,9.3,15,9.7z\"/>\n" +
                        "    </g>\n" +
                        "    <path fill=\"#40B6E0\"\n" +
                        "          d=\"M11.2,8c0.1,0,0.2,0,0.3,0c0.5,0,1,0.1,1.5,0.3V3H3v4.2c0,0,2.9-2.8,4.3-1.4C8.5,6.8,9.8,7.5,11.2,8z M10.5,4.7\n"
                        +
                        "\tc0.6,0,0.8,0.4,0.8,0.9s-0.2,0.8-0.8,0.8c-0.5,0-0.8-0.3-0.8-0.9C9.7,5,9.9,4.7,10.5,4.7z\"/>\n"
                        +
                        "    <path fill=\"#62B543\"\n" +
                        "          d=\"M9.3,8.6c-0.7-0.4-1.4-0.9-2-1.4C6,5.9,3,8.6,3,8.6v2.8h4.1C7.5,10.2,8.2,9.2,9.3,8.6z\"/>\n"
                        +
                        "    <path fill=\"#DB5860\"\n" +
                        "          d=\"M15,12.5c0,1.9-1.6,3.5-3.5,3.5S8,14.4,8,12.5S9.6,9,11.5,9S15,10.6,15,12.5\"/>\n"
                        +
                        "    <polygon fill=\"#231F20\" opacity=\"0.8\" points=\"13.6,11.1 12.9,10.4 11.5,11.8 10.1,10.4 9.4,11.1 10.8,12.5 9.4,13.9 10.1,14.6 11.5,13.2 12.9,14.6\n"
                        +
                        "\t    13.6,13.9 12.2,12.5 \"/>\n" +
                        "</svg>\n").getBytes(StandardCharsets.UTF_8)),
                null, LoaderContext.createDefault());
    });

    @Override
    public @NotNull FloatSize intrinsicSize(@NotNull RenderContext context) {
        return new FloatSize(SIZE, SIZE);
    }

    @Override
    public void render(@NotNull Output output, @NotNull RenderContext context, @NotNull AffineTransform transform) {
        output.applyTransform(transform);
        synchronized (missingImage) {
            missingImage.get().renderWithPlatform(context.platformSupport(), output, new ViewBox(0, 0, SIZE, SIZE));
        }
    }
}
