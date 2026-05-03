/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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

import java.util.List;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;

class LayoutGroupSegment<E, T extends TextContainer<E> & CursorContext> implements TextSegment.RenderableSegment {
    private final @NotNull T parent;
    private final @NotNull TextLayoutGroup group;

    LayoutGroupSegment(@NotNull T parent, @NotNull TextLayoutGroup group) {
        this.parent = parent;
        this.group = group;
    }

    @Override
    public boolean isSegmentVisible(@NotNull RenderContext currentContext) {
        return parent.isVisible(currentContext);
    }

    @Override
    public void prepareSegmentForRendering(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull TextOutput textOutput) {
        SVGFont font = RenderContextAccessor.instance().font(context);

        GlyphCursor localCursor = parent.createLocalCursor(false, cursor);
        localCursor.setAdvancement(localGlyphAdvancement(context, cursor));

        forEachSegment(
                group.segments(),
                context,
                (seg, ctx) -> GlyphRenderer.prepareGlyphRun(seg, localCursor, font, ctx, textOutput),
                (seg, ctx) -> seg.prepareSegmentForRendering(localCursor, ctx, textOutput));

        parent.cleanUpLocalCursor(cursor, localCursor);
    }

    private @NotNull GlyphAdvancement localGlyphAdvancement(@NotNull RenderContext context,
            @NotNull GlyphCursor cursor) {
        Length length = group.fixedLength();
        if (length != null) {
            return new GlyphAdvancement(
                    computeTextMetrics(context, UseTextLengthForCalculation.NO),
                    length.resolve(context.measureContext()), parent.lengthAdjust);
        }
        return cursor.advancement();
    }

    @Override
    public void renderSegmentWithoutLayout(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull Output output) {
        forEachSegment(
                group.segments(),
                context,
                (seg, ctx) -> {
                    if ((parent instanceof Renderable) && !((Renderable) parent).isVisible(ctx)) {
                        return;
                    }
                    GlyphRenderer.renderGlyphRun(
                            output, RenderContextAccessor.instance().paintOrder(context),
                            parent.vectorEffects(), seg);
                },
                (seg, ctx) -> seg.renderSegmentWithoutLayout(cursor, ctx, output));
    }

    @Override
    public @NotNull TextMetrics computeTextMetrics(@NotNull RenderContext context,
            @NotNull UseTextLengthForCalculation flag) {
        return TextMetrics.computeTextMetrics(group, context, flag);
    }

    @Override
    public void appendTextShape(@NotNull GlyphCursor cursor, @NotNull MutableGlyphRun glyphRun,
            @NotNull RenderContext context) {
        SVGFont font = RenderContextAccessor.instance().font(context);

        GlyphCursor localCursor = parent.createLocalCursor(false, cursor);
        localCursor.setAdvancement(localGlyphAdvancement(context, cursor));

        forEachSegment(
                group.segments(),
                context,
                (seg, ctx) -> glyphRun.append(
                        GlyphRenderer.layoutGlyphRun(seg, localCursor, font, ctx, NullTextOutput.INSTANCE)),
                (seg, ctx) -> seg.appendTextShape(localCursor, glyphRun, ctx));

        parent.cleanUpLocalCursor(cursor, localCursor);
    }

    static void forEachSegment(@NotNull List<? extends @NotNull TextSegment> segments,
            @NotNull RenderContext context,
            @NotNull BiConsumer<StringTextSegment, RenderContext> onStringTextSegment,
            @NotNull BiConsumer<TextSegment.RenderableSegment, RenderContext> onRenderableSegment) {
        for (TextSegment segment : segments) {
            RenderContext currentContext = context;
            if (segment instanceof TextContainer) {
                currentContext = NodeRenderer.setupRenderContext(segment, context);
            }
            if (segment instanceof StringTextSegment) {
                onStringTextSegment.accept((StringTextSegment) segment, currentContext);
            } else if (segment instanceof TextSegment.RenderableSegment) {
                onRenderableSegment.accept((TextSegment.RenderableSegment) segment, currentContext);
            } else {
                throw new IllegalStateException("Unexpected segment " + segment);
            }
        }
    }
}
