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
package com.github.weisj.jsvg.nodes.text;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.attributes.text.LengthAdjust;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.BaseContainerNode;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasContextImpl;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.*;

abstract class TextContainer extends BaseContainerNode<TextSegment>
        implements TextSegment.RenderableSegment, HasShape, HasContext.ByDelegate, Renderable {
    private final List<@NotNull TextSegment> segments = new ArrayList<>();

    private PaintOrder paintOrder;
    protected AttributeFontSpec fontSpec;
    protected LengthAdjust lengthAdjust;
    protected Length textLength;

    private boolean isVisible;
    private HasContext context;

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
        segments.add(new StringTextSegment(content));
    }

    @Override
    public List<? extends @NotNull TextSegment> children() {
        return segments;
    }

    protected abstract GlyphCursor createLocalCursor(@NotNull RenderContext context, @NotNull GlyphCursor current);

    protected abstract void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local);

    protected final void renderSegment(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull Graphics2D g) {
        prepareSegmentForRendering(cursor, context);

        double offset;
        switch (context.fontRenderContext().textAnchor()) {
            default:
            case Start:
                offset = 0;
                break;
            case Middle:
                offset = cursor.completeGlyphRunBounds.getWidth() / 2f;
                break;
            case End:
                offset = cursor.completeGlyphRunBounds.getWidth();
                break;
        }
        g.translate(-offset, 0);

        renderSegmentWithoutLayout(cursor, context, g);
    }

    private void forEachSegment(@NotNull RenderContext context,
            @NotNull BiConsumer<StringTextSegment, RenderContext> onStringTextSegment,
            @NotNull BiConsumer<RenderableSegment, RenderContext> onRenderableSegment) {
        for (TextSegment segment : children()) {
            RenderContext currentContext = context;
            if (segment instanceof Renderable) {
                if (!((Renderable) segment).isVisible(context)) continue;
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

    @Override
    public @NotNull TextMetrics computeTextMetrics(@NotNull RenderContext context) {
        SVGFont font = context.font();
        float letterSpacing = context.fontRenderContext().letterSpacing().resolveLength(context.measureContext());

        double whiteSpaceLength = 0;
        double glyphLength = 0;
        int glyphCount = 0;
        for (TextSegment segment : children()) {
            RenderContext currentContext = context;
            if (segment instanceof Renderable) {
                if (!((Renderable) segment).isVisible(context)) continue;
                currentContext = NodeRenderer.setupRenderContext(segment, context);
            }
            if (segment instanceof StringTextSegment) {
                glyphCount += ((StringTextSegment) segment).codepoints().length;
                if (glyphCount > 0) whiteSpaceLength += (glyphCount - 1) * letterSpacing;
                for (char codepoint : ((StringTextSegment) segment).codepoints()) {
                    glyphLength += font.codepointGlyph(codepoint).advance();
                }
            } else if (segment instanceof RenderableSegment) {
                TextMetrics textMetrics = ((RenderableSegment) segment).computeTextMetrics(currentContext);
                whiteSpaceLength += textMetrics.whiteSpaceLength();
                glyphLength += textMetrics.glyphLength();
                glyphCount += textMetrics.glyphCount();
            } else {
                throw new IllegalStateException("Unexpected segment " + segment);
            }
        }
        return new TextMetrics(whiteSpaceLength, glyphLength, glyphCount);
    }

    @Override
    public void renderSegmentWithoutLayout(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull Graphics2D g) {
        forEachSegment(context,
                (segment, ctx) -> GlyphRenderer.renderGlyphRun(paintOrder, segment, cursor.completeGlyphRunBounds, g),
                (segment, ctx) -> segment.renderSegmentWithoutLayout(cursor, ctx, g));
    }

    @Override
    public void prepareSegmentForRendering(@NotNull GlyphCursor cursor, @NotNull RenderContext context) {
        SVGFont font = context.font();

        GlyphCursor localCursor = createLocalCursor(context, cursor);

        localCursor.setAdvancement(textLength.isSpecified()
                ? new GlyphAdvancement(computeTextMetrics(context),
                        textLength.resolveWidth(context.measureContext()), lengthAdjust)
                : cursor.advancement());

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

        forEachSegment(context,
                (segment, ctx) -> textShape.append(GlyphRenderer.layoutGlyphRun(segment, localCursor, font,
                        ctx.measureContext(), ctx.fontRenderContext()), false),
                (segment, ctx) -> segment.appendTextShape(localCursor, textShape, ctx));

        cleanUpLocalCursor(cursor, localCursor);
    }

    @Override
    public @NotNull Rectangle2D untransformedElementBounds(@NotNull RenderContext context) {
        return untransformedElementShape(context).getBounds2D();
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible;
    }
}
