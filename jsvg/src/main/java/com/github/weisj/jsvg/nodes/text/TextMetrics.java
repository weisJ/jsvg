/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.text.TextSegment.RenderableSegment;
import com.github.weisj.jsvg.nodes.text.TextSegment.RenderableSegment.UseTextLengthForCalculation;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;

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

    static @NotNull TextMetrics computeTextMetrics(@NotNull TextLayoutGroup layoutGroup,
            @NotNull RenderContext context, @NotNull UseTextLengthForCalculation flag) {
        if (flag == UseTextLengthForCalculation.YES) {
            Length fixedLength = layoutGroup.fixedLength();
            if (fixedLength != null) {
                return new TextMetrics(0, 0, 0,
                        fixedLength.resolve(context.measureContext()), 0);
            }
        }

        RenderContextAccessor.Accessor accessor = RenderContextAccessor.instance();
        SVGFont font = accessor.font(context);
        float letterSpacing = accessor.fontRenderContext(context).letterSpacing().resolve(context.measureContext());

        IntermediateTextMetrics metrics = new IntermediateTextMetrics();

        int index = 0;
        for (TextSegment segment : layoutGroup.segments()) {
            RenderContext currentContext = context;
            if (segment instanceof Renderable) {
                currentContext = NodeRenderer.setupRenderContext(segment, context);
            }
            if (segment instanceof StringTextSegment) {
                StringTextSegment stringTextSegment = (StringTextSegment) segment;
                accumulateSegmentMetrics(layoutGroup, metrics, stringTextSegment, font, letterSpacing, index);
            } else if (segment instanceof RenderableSegment) {
                accumulateRenderableSegmentMetrics((RenderableSegment) segment, metrics, currentContext);
            } else {
                throw new IllegalStateException("Unexpected segment " + segment);
            }
            index++;
        }
        return new TextMetrics(metrics.letterSpacingLength, metrics.glyphLength, metrics.glyphCount,
                metrics.fixedGlyphLength, metrics.controllableLetterSpacingCount);
    }

    private static void accumulateRenderableSegmentMetrics(@NotNull RenderableSegment segment,
            @NotNull IntermediateTextMetrics metrics, @NotNull RenderContext currentContext) {
        TextMetrics textMetrics = segment.computeTextMetrics(currentContext, UseTextLengthForCalculation.YES);
        metrics.letterSpacingLength += textMetrics.letterSpacingLength();
        metrics.glyphLength += textMetrics.glyphLength();
        metrics.glyphCount += textMetrics.glyphCount();
        metrics.fixedGlyphLength += textMetrics.fixedGlyphLength();
        metrics.controllableLetterSpacingCount += textMetrics.controllableLetterSpacingCount();
    }

    private static void accumulateSegmentMetrics(@NotNull TextLayoutGroup layoutGroup,
            @NotNull IntermediateTextMetrics metrics,
            @NotNull StringTextSegment segment,
            @NotNull SVGFont font, float letterSpacing, int index) {
        int glyphCount = segment.codepoints().size();

        boolean lastSegment = index == layoutGroup.segments().size() - 1;
        int whiteSpaceCount = lastSegment ? (glyphCount - 1) : glyphCount;

        metrics.glyphCount += glyphCount;
        metrics.letterSpacingLength += whiteSpaceCount * letterSpacing;
        metrics.controllableLetterSpacingCount += whiteSpaceCount;

        for (String codepoint : segment.codepoints()) {
            metrics.glyphLength += font.codepointGlyph(codepoint).advance();
        }
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

    private static final class IntermediateTextMetrics {
        double letterSpacingLength = 0;
        double glyphLength = 0;
        double fixedGlyphLength = 0;

        int glyphCount = 0;
        int controllableLetterSpacingCount = 0;
    }
}
