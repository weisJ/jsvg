/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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
package com.github.weisj.jsvg.nodes.text;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.SystemUtil;

class AbstractGlyphRun<T extends Shape> {

    static class Metrics {
        final @NotNull Rectangle2D paintBounds;
        final @NotNull Rectangle2D layoutBounds;

        Metrics(@NotNull Rectangle2D paintBounds, @NotNull Rectangle2D layoutBounds) {
            this.paintBounds = paintBounds;
            this.layoutBounds = layoutBounds;
        }

        static @NotNull Metrics createDefault() {
            return new Metrics(
                    new Rectangle2D.Float(Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW, 0, 0),
                    new Rectangle2D.Float(Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW, 0, 0));
        }

        void union(@NotNull Metrics metrics) {
            if (Length.isUnspecified((float) paintBounds.getX())) {
                paintBounds.setRect(metrics.paintBounds);
                layoutBounds.setRect(metrics.layoutBounds);
                return;
            }
            Rectangle2D.union(paintBounds, metrics.paintBounds, paintBounds);
            Rectangle2D.union(layoutBounds, metrics.layoutBounds, layoutBounds);
        }
    }

    private final @NotNull T shape;
    private final @NotNull Metrics metrics;
    private final @NotNull List<@NotNull PaintableEmoji> emojis;

    public AbstractGlyphRun(@NotNull T shape, @NotNull Metrics metrics, @NotNull List<@NotNull PaintableEmoji> emojis) {
        this.shape = shape;
        this.metrics = metrics;
        this.emojis = emojis;
    }

    public @NotNull T shape() {
        return shape;
    }

    public @NotNull Metrics metrics() {
        return metrics;
    }

    public @NotNull List<@NotNull PaintableEmoji> emojis() {
        return emojis;
    }

    public static class PaintableEmoji {
        private final @NotNull EmojiGlyph glyph;
        private final @NotNull AffineTransform transform;

        PaintableEmoji(@NotNull EmojiGlyph glyph, @NotNull AffineTransform transform) {
            this.glyph = glyph;
            this.transform = transform;
        }

        public void render(@NotNull Output output, @NotNull SVGFont font) {
            output.applyTransform(transform);
            int fontSize = font.size();

            // The JDK text pipeline on macOS can only render Emoji up to 100pt.
            // In this case we have to paint to a buffer which is then scaled up.
            int maxFontSize = SystemUtil.isMacOS ? 100 : Integer.MAX_VALUE;

            if (output.transform().getScaleY() * fontSize > maxFontSize) {
                // Guess where the baseline ought to be for descenders to be fully visible. Very hackish.
                float baselinePosition = 0.9f;
                if (glyph.largeBitmap == null) {
                    // Size of bitmap is guessed, as there is now reliable way to get the size of the glyph.
                    BufferedImage bitmap = ImageUtil.createCompatibleTransparentImage(maxFontSize, maxFontSize);
                    Graphics g = bitmap.getGraphics();
                    g.setFont(g.getFont().deriveFont((float) maxFontSize));

                    g.drawString(glyph.codepoint(), 0, (int) (baselinePosition * maxFontSize));
                    g.dispose();
                    glyph.largeBitmap = bitmap;
                }
                output.scale((double) fontSize / maxFontSize, (double) fontSize / maxFontSize);
                output.translate(0, -(int) (baselinePosition * maxFontSize));
                output.drawImage(glyph.largeBitmap);
            } else {
                if (output instanceof Graphics2DOutput) {
                    Graphics2D g = ((Graphics2DOutput) output).graphics();
                    g.setFont(g.getFont().deriveFont((float) fontSize));
                    g.drawString(glyph.codepoint(), 0, 0);
                }
            }
        }
    }
}
