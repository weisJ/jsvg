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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.ReferenceCounter;


public final class GraphicsUtil {
    private static final Logger LOGGER = Logger.getLogger(GraphicsUtil.class.getName());

    private GraphicsUtil() {}

    public static void safelySetPaint(@NotNull Graphics2D g, @NotNull Paint paint) {
        safelySetPaint(g, paint, true);
    }

    public static void safelySetPaint(@NotNull Graphics2D g, @NotNull Paint paint, boolean disposeOld) {
        g.setPaint(exchangePaint(g.getPaint(), paint, disposeOld));
    }

    public static void cleanupPaint(@NotNull Paint paint) {
        if (paint instanceof WrappingPaint) {
            cleanupPaint(((WrappingPaint) paint).paint());
        }
        if (paint instanceof ReferenceCountedPaint) {
            ReferenceCounter counter = ((ReferenceCountedPaint) paint).referenceCounter();
            if (counter != null) counter.decreaseReference();
        }
    }

    public static void preparePaint(@NotNull Paint paint) {
        if (paint instanceof WrappingPaint) {
            preparePaint(((WrappingPaint) paint).paint());
        }
        if (paint instanceof ReferenceCountedPaint) {
            ReferenceCounter counter = ((ReferenceCountedPaint) paint).referenceCounter();
            if (counter != null) counter.increaseReference();
        }
    }

    private static @NotNull Paint exchangePaint(@NotNull Paint current, @NotNull Paint paint, boolean updateRefCounts) {
        if (current instanceof WrappingPaint) {
            WrappingPaint wrappingPaint = (WrappingPaint) current;
            wrappingPaint.safelySetPaint(paint, updateRefCounts);
            return current;
        }
        if (updateRefCounts) {
            preparePaint(paint);
            cleanupPaint(current);
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
            LOGGER.warning(String.format("Composite %s will be overridden by opacity %s", composite, opacity));
        }
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
    }

    public static void safelyDrawImage(@NotNull Graphics2D g, @NotNull Image image, @Nullable ImageObserver observer) {
        Paint p = g.getPaint();
        if (p instanceof WrappingPaint) {
            WrappingPaint wrappingPaint = (WrappingPaint) p;
            Paint inner = wrappingPaint.innerPaint();

            Rectangle r = new Rectangle(0, 0, image.getWidth(observer), image.getHeight(observer));
            BufferedImage img = image instanceof BufferedImage
                    ? (BufferedImage) image
                    : ImageUtil.toBufferedImage(image);
            TexturePaint texturePaint = new TexturePaint(img, r);

            wrappingPaint.setPaint(exchangePaint(wrappingPaint.paint(), texturePaint, false));
            g.fill(r);
            wrappingPaint.setPaint(exchangePaint(texturePaint, inner, false));
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

        default void safelySetPaint(@NotNull Paint paint, boolean updateRefCounts) {
            setPaint(exchangePaint(paint(), paint, updateRefCounts));
        }
    }

    public interface ReferenceCountedPaint {
        @Nullable
        ReferenceCounter referenceCounter();
    }
}
