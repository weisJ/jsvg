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
package com.github.weisj.jsvg.nodes.text;

import java.awt.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;

@ElementCategories({Category.TextContent, Category.TextContentChild})
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {Anchor.class, TextSpan.class, Animate.class, AnimateTransform.class, Set.class /* <altGlyph>, <tref> */ },
    charData = true
)
public final class TextSpan extends LinearTextContainer<TextSegment> implements TextSegment.RenderableSegment {
    public static final String TAG = "tspan";

    private LinearTextLayoutGroup layoutGroup;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        layoutGroup = new LinearTextLayoutGroup(this, children);
    }

    @Override
    protected void doAdd(@NotNull SVGNode node) {
        children.add((TextSegment) node);
    }

    @Override
    public void addContent(@NotNull TextContent.Segment content) {
        if (content.isConstant() && content.text().isEmpty()) return;
        children.add(new StringTextSegment(this, children.size(), content));
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof TextSegment;
    }

    @Override
    public void prepareSegmentForRendering(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull TextOutput textOutput) {
        layoutGroup.asSegment().prepareSegmentForRendering(cursor, context, textOutput);
    }

    @Override
    public void renderSegmentWithoutLayout(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
            @NotNull Output output) {
        layoutGroup.asSegment().renderSegmentWithoutLayout(cursor, context, output);
    }

    @Override
    public @NotNull TextMetrics computeTextMetrics(@NotNull RenderContext context,
            @NotNull UseTextLengthForCalculation flag) {
        return layoutGroup.asSegment().computeTextMetrics(context, flag);
    }

    @Override
    public void appendTextShape(@NotNull GlyphCursor cursor, @NotNull MutableGlyphRun glyphRun,
            @NotNull RenderContext context) {
        layoutGroup.asSegment().appendTextShape(cursor, glyphRun, context);
    }

    @Override
    @NotNull
    Shape glyphShape(@NotNull RenderContext context) {
        return layoutGroup.glyphShape(context);
    }

    @Override
    public boolean isSegmentVisible(@NotNull RenderContext currentContext) {
        return layoutGroup.asSegment().isSegmentVisible(currentContext);
    }
}
