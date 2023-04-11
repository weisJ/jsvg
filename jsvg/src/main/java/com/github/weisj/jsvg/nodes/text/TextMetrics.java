/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jannis Weis
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

public final class TextMetrics {
    private final double letterSpacingLength;
    private final double glyphLength;
    private final double fixedGlyphLength;
    private final int glyphCount;
    private final int controllableLetterSpacingCount;

    public TextMetrics(double letterSpacingLength, double visibleCodepointLength, int glyphCount,
            double fixedGlyphLength, int controllableLetterSpacingCount) {
        this.letterSpacingLength = letterSpacingLength;
        this.glyphLength = visibleCodepointLength;
        this.glyphCount = glyphCount;
        this.fixedGlyphLength = fixedGlyphLength;
        this.controllableLetterSpacingCount = controllableLetterSpacingCount;
    }

    public double letterSpacingLength() {
        return letterSpacingLength;
    }

    public double glyphLength() {
        return glyphLength;
    }

    public double fixedGlyphLength() {
        return fixedGlyphLength;
    }

    public double totalAdjustableLength() {
        return glyphLength() + letterSpacingLength();
    }

    public int glyphCount() {
        return glyphCount;
    }

    public int controllableLetterSpacingCount() {
        return controllableLetterSpacingCount;
    }

    @Override
    public String toString() {
        return "TextMetrics{" +
                "whiteSpaceLength=" + letterSpacingLength +
                ", glyphLength=" + glyphLength +
                ", glyphCount=" + glyphCount +
                ", fixedGlyphLength=" + fixedGlyphLength +
                '}';
    }
}
