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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;

public final class GraphicsUtil {
    private GraphicsUtil() {}

    public static void safelySetPaint(@NotNull Graphics2D g, @NotNull Paint paint) {
        g.setPaint(setupPaint(g.getPaint(), paint));
    }

    public static @NotNull Paint setupPaint(@NotNull Paint current, @NotNull Paint paint) {
        if (current instanceof WrappingPaint) {
            ((WrappingPaint) current).setPaint(paint);
            return current;
        }
        return paint;
    }

    public static @NotNull Graphics2D createGraphics(@NotNull BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.clipRect(0, 0, image.getWidth(), image.getHeight());
        return g;
    }

    public interface WrappingPaint {
        void setPaint(@NotNull Paint paint);
    }
}
