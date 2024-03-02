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
package com.github.weisj.jsvg.nodes.text;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.attributes.text.LengthAdjust;
import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.BaseContainerNode;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.HasVectorEffects;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasContextImpl;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.*;

abstract class TextContainer extends BaseContainerNode<TextSegment>
        implements TextSegment.RenderableSegment, HasShape, HasContext.ByDelegate, HasVectorEffects, Renderable {
    private final List<@NotNull TextSegment> segments = new ArrayList<>();

    private PaintOrder paintOrder;
    protected AttributeFontSpec fontSpec;
    protected LengthAdjust lengthAdjust;
    protected Length textLength;

    private boolean isVisible;
    private HasContext context;

    private Set<VectorEffect> vectorEffects;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        paintOrder = PaintOrder.parse(attributeNode);
        fontSpec = FontParser.parseFontSpec(attributeNode);
        lengthAdjust = attributeNode.getEnum("lengthAdjust", LengthAdjust.Spacing);
        textLength = attributeNode.getLength("textLength", Length.UNSPECIFIED);
        if (textLength.raw() < 0) textLength = Length.UNSPECIFIED;

        isVisible = parseIsVisible(attributeNode);
        context = HasContextImpl.parse(attributeNode);

        // Todo: Current vector effect pipeline doesn't allow them to be defined on 'tspan' and 'text-path'.
        vectorEffects = VectorEffect.parse(attributeNode);
    }

    @Override
    public @NotNull Set<VectorEffect> vectorEffects() {
        return vectorEffects;
    }

    @Override
    public @NotNull HasContext contextDelegate() {
        return context;
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof TextSegment;
    }

    @Override
    protected void doAdd(@NotNull SVGNode node) {
        segments.add((TextSegment) node);
    }

    @Override
    public final void addContent(char[] content) {
        if (content.length == 0) return;
        segments.add(new StringTextSegment(this, segments.size(), content));
    }

    @Override
    public List<? extends @NotNull TextSegment> children() {
        return segments;
    }

    protected abstract GlyphCursor createLocalCursor(@NotNull RenderContext context, @NotNull GlyphCursor current);

    // Update necessary information from the local cursor to the parent cursor e.g. the current x/y
    // position.
    protected abstract void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local);

    protected final void renderSegment(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull Output output) {
        prepareSegmentForRendering(cursor, context);

        double offset = textAnchorOffset(context.fontRenderContext().textAnchor(), cursor);
        context.translate(output, -offset, 0);

        renderSegmentWithoutLayout(cursor, context, output);
    }

    private double textAnchorOffset(@NotNull TextAnchor textAnchor, @NotNull GlyphCursor glyphCursor) {
        switch (textAnchor) {
            default:
            case Start:
                return 0;
            case Middle:
                return glyphCursor.completeGlyphRunBounds.getWidth() / 2f;
            case End:
                return glyphCursor.completeGlyphRunBounds.getWidth();
        }
    }

    private void forEachSegment(@NotNull RenderContext context,
            @NotNull BiConsumer<StringTextSegment, RenderContext> onStringTextSegment,
            @NotNull BiConsumer<RenderableSegment, RenderContext> onRenderableSegment) {
        for (TextSegment segment : children()) {
            RenderContext currentContext = context;
            if (segment instanceof Renderable) {
                currentContext = NodeRenderer.setupRenderContext(segment, context);
            }
            if (segment instanceof StringTextSegment) {
                onStringTextSegment.accept((StringTextSegment) segment, currentContext);
            } else if (segment instanceof RenderableSegment) {
                onRenderableSegment.accept((RenderableSegment) segment, currentContext);
            } else {
                throw new IllegalStateException("Unexpected segment " + segment);
            }
        }
    }

    private static final class IntermediateTextMetrics {
        double letterSpacingLength = 0;
        double glyphLength = 0;
        double fixedGlyphLength = 0;

        int glyphCount = 0;
        int controllableLetterSpacingCount = 0;
    }

    @Override
    public @NotNull TextMetrics computeTextMetrics(@NotNull RenderContext context,
            @NotNull UseTextLengthForCalculation flag) {
        if (flag == UseTextLengthForCalculation.YES && hasFixedLength()) {
            return new TextMetrics(0, 0, 0,
                    textLength.resolveLength(context.measureContext()), 0);
        }

        SVGFont font = context.font();
        float letterSpacing = context.fontRenderContext().letterSpacing().resolveLength(context.measureContext());

        IntermediateTextMetrics metrics = new IntermediateTextMetrics();

        int index = 0;
        for (TextSegment segment : children()) {
            RenderContext currentContext = context;
            if (segment instanceof Renderable) {
                currentContext = NodeRenderer.setupRenderContext(segment, context);
            }
            if (segment instanceof StringTextSegment) {
                StringTextSegment stringTextSegment = (StringTextSegment) segment;
                accumulateSegmentMetrics(metrics, stringTextSegment, font, letterSpacing, index);
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

    private void accumulateRenderableSegmentMetrics(@NotNull RenderableSegment segment,
            @NotNull IntermediateTextMetrics metrics, @NotNull RenderContext currentContext) {
        TextMetrics textMetrics = segment.computeTextMetrics(currentContext, UseTextLengthForCalculation.YES);
        metrics.letterSpacingLength += textMetrics.letterSpacingLength();
        metrics.glyphLength += textMetrics.glyphLength();
        metrics.glyphCount += textMetrics.glyphCount();
        metrics.fixedGlyphLength += textMetrics.fixedGlyphLength();
        metrics.controllableLetterSpacingCount += textMetrics.controllableLetterSpacingCount();
    }

    private void accumulateSegmentMetrics(@NotNull IntermediateTextMetrics metrics, @NotNull StringTextSegment segment,
            @NotNull SVGFont font, float letterSpacing, int index) {
        int glyphCount = segment.codepoints().length;

        boolean lastSegment = index == children().size() - 1;
        int whiteSpaceCount = lastSegment ? (glyphCount - 1) : glyphCount;

        metrics.glyphCount += glyphCount;
        metrics.letterSpacingLength += whiteSpaceCount * letterSpacing;
        metrics.controllableLetterSpacingCount += whiteSpaceCount;

        for (char codepoint : segment.codepoints()) {
            metrics.glyphLength += font.codepointGlyph(codepoint).advance();
        }
    }

    @Override
    public boolean hasFixedLength() {
        return textLength.isSpecified();
    }

    @Override
    public void renderSegmentWithoutLayout(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull Output output) {
        forEachSegment(context,
                (segment, ctx) -> {
                    if (isVisible(ctx)) {
                        GlyphRenderer.renderGlyphRun(output, paintOrder, vectorEffects(), segment,
                                cursor.completeGlyphRunBounds);
                    }
                },
                (segment, ctx) -> segment.renderSegmentWithoutLayout(cursor, ctx, output));
    }

    @Override
    public void prepareSegmentForRendering(@NotNull GlyphCursor cursor, @NotNull RenderContext context) {
        SVGFont font = context.font();

        GlyphCursor localCursor = createLocalCursor(context, cursor);
        localCursor.setAdvancement(localGlyphAdvancement(context, cursor));

        forEachSegment(context,
                (segment, ctx) -> GlyphRenderer.prepareGlyphRun(segment, localCursor, font, ctx),
                (segment, ctx) -> segment.prepareSegmentForRendering(localCursor, ctx));

        cleanUpLocalCursor(cursor, localCursor);
    }

    @Override
    public void appendTextShape(@NotNull GlyphCursor cursor, @NotNull Path2D textShape,
            @NotNull RenderContext context) {
        SVGFont font = context.font();
        GlyphCursor localCursor = createLocalCursor(context, cursor);
        localCursor.setAdvancement(localGlyphAdvancement(context, cursor));

        forEachSegment(context,
                (segment, ctx) -> textShape.append(GlyphRenderer.layoutGlyphRun(segment, localCursor, font,
                        ctx.measureContext(), ctx.fontRenderContext()), false),
                (segment, ctx) -> segment.appendTextShape(localCursor, textShape, ctx));

        cleanUpLocalCursor(cursor, localCursor);
    }

    private @NotNull GlyphAdvancement localGlyphAdvancement(@NotNull RenderContext context,
            @NotNull GlyphCursor cursor) {
        if (hasFixedLength()) {
            return new GlyphAdvancement(
                    computeTextMetrics(context, UseTextLengthForCalculation.NO),
                    textLength.resolveWidth(context.measureContext()), lengthAdjust);
        }
        return cursor.advancement();
    }

    @Override
    public @NotNull Rectangle2D untransformedElementBounds(@NotNull RenderContext context, Box box) {
        // TODO: Bounding-box is specified by the character box.
        return untransformedElementShape(context).getBounds2D();
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible;
    }
}
