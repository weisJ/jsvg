/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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
package com.github.weisj.jsvg.renderer.output;

import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.text.NullTextOutput;
import com.github.weisj.jsvg.renderer.RenderContext;

public interface TextOutput {

    void codepoint(@NotNull String codepoint, @NotNull AffineTransform glyphTransform, @NotNull RenderContext context);

    static @NotNull TextOutput createDefault() {
        return NullTextOutput.INSTANCE;
    }

    /**
     * Reports the text-anchor offset of the current text element. The glyph transforms passed to
     * {@link #codepoint} are produced during layout, before the anchor offset is known, and hence do
     * not include it. Implementations which render glyphs themselves must therefore buffer glyph runs
     * and shift them by {@code -offset} along the x-axis when rendering.
     * <p>
     * Called between {@link #beginText()} and {@link #endText()}, after all codepoints of the text
     * element have been reported. Not called for text on a path (there the anchor is already part of
     * the glyph transforms).
     */
    default void textAnchorOffset(double offset) {}

    void beginText();

    /**
     * Signals that the next glyph has manual placement and therefore is visually separated from the previous glyph.
     */
    void glyphRunBreak();

    void endText();
}
