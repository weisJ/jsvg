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
package com.github.weisj.jsvg.renderer.output.impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.ImageUtil;


public final class GraphicsUtil {
    private static final Logger LOGGER = LogFactory.createLogger(GraphicsUtil.class);

    private GraphicsUtil() {}

    static void safelySetPaint(@NotNull Output output, @NotNull Graphics2D g, @NotNull Paint paint) {
        g.setPaint(exchangePaint(output, g.getPaint(), paint, true));
    }

    static void cleanupPaint(@NotNull Output output, @NotNull Paint paint) {
        if (paint instanceof WrappingPaint) {
            cleanupPaint(output, ((WrappingPaint) paint).paint());
        }
        if (paint instanceof DisposablePaint) {
            ((DisposablePaint) paint).cleanupIfNeeded(output);
        }
    }

    static void preparePaint(@NotNull Paint paint) {
        if (paint instanceof WrappingPaint) {
            preparePaint(((WrappingPaint) paint).paint());
        }
    }

    private static @NotNull Paint exchangePaint(@NotNull Output output, @NotNull Paint current, @NotNull Paint paint,
            boolean doCleanUp) {
        if (paint instanceof WrappingPaint) {
            WrappingPaint wrappingPaint = (WrappingPaint) paint;
            wrappingPaint.safelySetPaint(output, current, doCleanUp);
            return paint;
        }
        if (current instanceof WrappingPaint) {
            WrappingPaint wrappingPaint = (WrappingPaint) current;
            wrappingPaint.safelySetPaint(output, paint, doCleanUp);
            return current;
        }
        if (doCleanUp) {
            preparePaint(paint);
            cleanupPaint(output, current);
        }
        return paint;
    }

    public static @NotNull Graphics2D createGraphics(@NotNull BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.clipRect(0, 0, image.getWidth(), image.getHeight());
        return g;
    }

    public static @NotNull Composite deriveComposite(@NotNull Graphics2D g, float opacity) {
        Composite composite = g.getComposite();
        if (composite instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) composite;
            return AlphaComposite.getInstance(ac.getRule(), ac.getAlpha() * opacity);
        } else if (composite != null) {
            LOGGER.log(Level.WARNING,
                    () -> String.format("Composite %s will be overridden by opacity %s", composite, opacity));
        }
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
    }

    public static void safelyDrawImage(@NotNull Output output, @NotNull Graphics2D g, @NotNull Image image,
            @Nullable ImageObserver observer) {
        Paint p = g.getPaint();
        if (p instanceof WrappingPaint) {
            WrappingPaint wrappingPaint = (WrappingPaint) p;
            Paint inner = wrappingPaint.innerPaint();

            Rectangle r = new Rectangle(0, 0, image.getWidth(observer), image.getHeight(observer));
            BufferedImage img = image instanceof BufferedImage
                    ? (BufferedImage) image
                    : ImageUtil.toBufferedImage(image);
            TexturePaint texturePaint = new TexturePaint(img, r);

            wrappingPaint.setPaint(exchangePaint(output, wrappingPaint.paint(), texturePaint, false));
            g.fill(r);
            wrappingPaint.setPaint(exchangePaint(output, texturePaint, inner, false));
        } else {
            g.drawImage(image, 0, 0, observer);
        }
    }

    public interface WrappingPaint {
        void setPaint(@NotNull Paint paint);

        @NotNull
        Paint paint();

        default @NotNull Paint innerPaint() {
            Paint p = paint();
            if (p instanceof WrappingPaint) {
                return ((WrappingPaint) p).innerPaint();
            }
            return p;
        }

        default void safelySetPaint(@NotNull Output output, @NotNull Paint paint, boolean updateRefCounts) {
            setPaint(exchangePaint(output, paint(), paint, updateRefCounts));
        }
    }

    public interface DisposablePaint {

        void cleanupIfNeeded(@NotNull Output output);
    }
}
