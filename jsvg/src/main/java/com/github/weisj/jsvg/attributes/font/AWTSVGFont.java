/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.text.Bidi;
import java.util.Collections;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.text.EmojiGlyph;
import com.github.weisj.jsvg.nodes.text.Glyph;

public final class AWTSVGFont implements SVGFont {

    private final @NotNull Font font;
    private final FontRenderContext frc = new FontRenderContext(null, true, true);
    private final HashMap<String, Glyph> glyphCache;
    private final HashMap<String, Float> kerningCache = new HashMap<>();
    private @Nullable Font kerningFont;

    private @Nullable LineMetrics lineMetrics;
    private float[] baselineOffsets;
    private float exHeight = Length.UNSPECIFIED_RAW;
    private float mathBaseline = Length.UNSPECIFIED_RAW;

    public AWTSVGFont(@NotNull Font font) {
        this.font = font;
        this.glyphCache = new HashMap<>();
    }

    public @NotNull Font font() {
        return font;
    }

    @Override
    public @NotNull Glyph codepointGlyph(@NotNull String codepoint) {
        Glyph glyph = glyphCache.get(codepoint);
        if (glyph != null) return glyph;
        glyph = createGlyph(codepoint.toCharArray());
        glyphCache.put(codepoint, glyph);
        return glyph;
    }

    @Override
    public float kerningAdjustment(@NotNull String leftCodepoint, @NotNull String rightCodepoint) {
        String pair = leftCodepoint + '\0' + rightCodepoint;
        Float cached = kerningCache.get(pair);
        if (cached != null) return cached;
        float kerning = computeKerningAdjustment(leftCodepoint, rightCodepoint);
        kerningCache.put(pair, kerning);
        return kerning;
    }

    private float computeKerningAdjustment(@NotNull String leftCodepoint, @NotNull String rightCodepoint) {
        if (isDefaultIgnorable(leftCodepoint) || isDefaultIgnorable(rightCodepoint)) return 0;
        Glyph leftGlyph = codepointGlyph(leftCodepoint);
        Glyph rightGlyph = codepointGlyph(rightCodepoint);
        if (leftGlyph instanceof EmojiGlyph || rightGlyph instanceof EmojiGlyph) return 0;

        char[] pair = (leftCodepoint + rightCodepoint).toCharArray();
        if (Bidi.requiresBidi(pair, 0, pair.length)) return 0;

        if (kerningFont == null) {
            kerningFont = font.deriveFont(
                    Collections.singletonMap(TextAttribute.KERNING, TextAttribute.KERNING_ON));
        }
        GlyphVector glyphVector = kerningFont.layoutGlyphVector(frc, pair, 0, pair.length,
                Font.LAYOUT_LEFT_TO_RIGHT);
        // Contextual shaping changed the glyphs; the advance difference is not a kern.
        if (glyphVector.getNumGlyphs() != 2) return 0;
        float kernedAdvance = (float) glyphVector.getGlyphPosition(2).getX();
        return kernedAdvance - leftGlyph.advance() - rightGlyph.advance();
    }

    @Override
    public @NotNull String family() {
        return font.getFamily();
    }

    @Override
    public int size() {
        return font.getSize();
    }

    private @NotNull LineMetrics lineMetrics() {
        if (lineMetrics == null) {
            lineMetrics = font.getLineMetrics("Ax-", 0, 1, frc);
        }
        return lineMetrics;
    }

    @Override
    public float effectiveExHeight() {
        if (Length.isUnspecified(exHeight)) {
            exHeight = (float) codepointGlyph("x").glyphOutline().getBounds2D().getHeight();
        }
        return exHeight;
    }

    @Override
    public float effectiveEmHeight() {
        return font.getSize2D();
    }

    @Override
    public float mathematicalBaseline() {
        if (Length.isUnspecified(mathBaseline)) {
            mathBaseline = -effectiveExHeight() / 2;
        }
        return mathBaseline;
    }

    private float[] baselineOffsets() {
        if (baselineOffsets == null) baselineOffsets = lineMetrics().getBaselineOffsets();
        return baselineOffsets;
    }

    @Override
    public float hangingBaseline() {
        return baselineOffsets()[Font.HANGING_BASELINE];
    }

    @Override
    public float romanBaseline() {
        return baselineOffsets()[Font.ROMAN_BASELINE];
    }

    @Override
    public float centerBaseline() {
        return baselineOffsets()[Font.CENTER_BASELINE];
    }

    @Override
    public float middleBaseline() {
        return (romanBaseline() - effectiveExHeight()) / 2;
    }

    @Override
    public float textUnderBaseline() {
        return lineMetrics().getUnderlineOffset();
    }

    @Override
    public float textOverBaseline() {
        return textUnderBaseline() - effectiveEmHeight();
    }

    @NotNull
    private Glyph createGlyph(char @NotNull [] codepoint) {
        // invisible even if the font has no glyph or a visible one
        if (isDefaultIgnorable(String.valueOf(codepoint))) {
            return new Glyph(new Path2D.Float(), 0, true);
        }
        GlyphVector glyphVector = font.createGlyphVector(frc, codepoint);
        GlyphMetrics gm = glyphVector.getGlyphMetrics(0);
        float advance = gm.getAdvanceX();

        if (isPossibleEmoji(codepoint)) {
            return new EmojiGlyph(String.valueOf(codepoint), advance);
        }

        Shape shape = glyphVector.getOutline(0, 0);
        return new Glyph(shape, advance, gm.getBounds2D().isEmpty());
    }

    /** True if every code point is Default_Ignorable_Code_Point. */
    private static boolean isDefaultIgnorable(@NotNull String codepoint) {
        if (codepoint.isEmpty()) return false;
        for (int i = 0; i < codepoint.length();) {
            int cp = codepoint.codePointAt(i);
            if (!isDefaultIgnorable(cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    /**
     * Except Hangul fillers and U+1BCA0..1BCA3, which keep their spacing glyphs as in HarfBuzz.
     *
     * @see <a href="https://www.unicode.org/Public/UCD/latest/ucd/DerivedCoreProperties.txt">Unicode data</a>
     */
    private static boolean isDefaultIgnorable(int cp) {
        if (cp < 0x00AD) return false;
        return cp == 0x00AD
                || cp == 0x034F
                || cp == 0x061C
                || (cp >= 0x17B4 && cp <= 0x17B5)
                || (cp >= 0x180B && cp <= 0x180F)
                || (cp >= 0x200B && cp <= 0x200F)
                || (cp >= 0x202A && cp <= 0x202E)
                || (cp >= 0x2060 && cp <= 0x206F)
                || (cp >= 0xFE00 && cp <= 0xFE0F)
                || cp == 0xFEFF
                || (cp >= 0xFFF0 && cp <= 0xFFF8)
                || (cp >= 0x1D173 && cp <= 0x1D17A)
                || (cp >= 0xE0000 && cp <= 0xE0FFF);
    }

    private static boolean isPossibleEmoji(char @NotNull [] codepoint) {
        for (char c : codepoint) {
            if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                return true;
            }
        }
        return false;
    }
}
