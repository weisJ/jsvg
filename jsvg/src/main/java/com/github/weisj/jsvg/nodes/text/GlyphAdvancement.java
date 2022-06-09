/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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

import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.text.LengthAdjust;
import com.google.errorprone.annotations.Immutable;

@Immutable
class GlyphAdvancement {
    private float spacingAdjustment = 1;
    private float absoluteSpacingAdjustment = 0;
    private float glyphAdjustment = 1;

    private GlyphAdvancement() {}

    GlyphAdvancement(@NotNull TextMetrics textMetrics, float desiredLength, @NotNull LengthAdjust lengthAdjust) {
        double totalSpace = desiredLength - textMetrics.fixedGlyphLength();
        switch (lengthAdjust) {
            default:
            case Spacing:
                if (textMetrics.glyphCount() == 0) {
                    absoluteSpacingAdjustment = 0;
                } else {
                    // FIXME: This doesn't properly respect spacings between segments.
                    // The adjustment should only be compensated using spaces inside the segment,
                    // but the way it works currently also the last space is used as well.
                    absoluteSpacingAdjustment =
                            (float) (totalSpace - textMetrics.glyphLength()) / textMetrics.glyphCount();
                }
                break;
            case SpacingAndGlyphs:
                spacingAdjustment = (float) (totalSpace / textMetrics.totalAdjustableLength());
                glyphAdjustment = spacingAdjustment;
                break;
        }
    }

    static GlyphAdvancement defaultAdvancement() {
        return new GlyphAdvancement();
    }

    float advancement(@NotNull Glyph glyph, float letterSpacing) {
        return glyphAdvancement(glyph) + spacingAdvancement(letterSpacing);
    }

    public float maxLookBehind() {
        return -Math.min(0, absoluteSpacingAdjustment);
    }

    float spacingAdvancement(float letterSpacing) {
        return letterSpacing * spacingAdjustment + absoluteSpacingAdjustment;
    }

    float glyphAdvancement(@NotNull Glyph glyph) {
        return glyph.advance() * glyphAdjustment;
    }

    @NotNull
    AffineTransform glyphTransform(@NotNull AffineTransform transform) {
        if (glyphAdjustment == 1) return transform;
        AffineTransform glyphTransform = new AffineTransform(transform);
        glyphTransform.scale(glyphAdjustment, 1);
        return glyphTransform;
    }

    @Override
    public String toString() {
        return "GlyphAdvancement{" +
                "spacingAdjustment=" + spacingAdjustment +
                ", absoluteSpacingAdjustment=" + absoluteSpacingAdjustment +
                ", glyphAdjustment=" + glyphAdjustment +
                '}';
    }
}
