/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.attributes.font;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.nodes.prototype.spec.NotImplemented;
import com.github.weisj.jsvg.nodes.text.Glyph;

public class AWTSVGFont implements SVGFont {
    private final @NotNull Font font;
    private final @NotImplemented @Percentage float stretch;
    private final FontRenderContext frc = new FontRenderContext(null, true, true);
    private final HashMap<Character, Glyph> glyphCache;

    public AWTSVGFont(@NotNull Font font, @Percentage float stretch) {
        this.font = font;
        this.stretch = stretch;
        this.glyphCache = new HashMap<>();
    }

    public @NotNull Font font() {
        return font;
    }

    @Override
    public @NotNull Glyph codepointGlyph(char codepoint) {
        Glyph glyph = glyphCache.get(codepoint);
        if (glyph != null) return glyph;
        glyph = createGlyph(codepoint);
        glyphCache.put(codepoint, glyph);
        return glyph;
    }

    @Override
    public @NotNull String family() {
        return font.getFamily();
    }

    @Override
    public int size() {
        return font.getSize();
    }

    @NotNull
    private Glyph createGlyph(char codepoint) {
        char[] buffer = new char[] {codepoint};
        GlyphVector glyphVector = font.createGlyphVector(frc, buffer);
        GlyphMetrics gm = glyphVector.getGlyphMetrics(0);
        float advance = gm.getAdvanceX();
        Shape shape = glyphVector.getGlyphOutline(0);
        return new Glyph(codepoint, shape, advance, gm.getBounds2D().isEmpty());
    }
}
