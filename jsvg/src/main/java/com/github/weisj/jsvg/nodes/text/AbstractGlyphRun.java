/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class AbstractGlyphRun<T extends Shape> {
    private final @NotNull T shape;
    private final @NotNull List<@NotNull PaintableEmoji> emojis;

    public AbstractGlyphRun(@NotNull T shape, @NotNull List<@NotNull PaintableEmoji> emojis) {
        this.shape = shape;
        this.emojis = emojis;
    }

    public @NotNull T shape() {
        return shape;
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

        public @NotNull EmojiGlyph glyph() {
            return glyph;
        }

        public @NotNull AffineTransform transform() {
            return transform;
        }
    }
}
