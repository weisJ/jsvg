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
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasGeometryContextImpl;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.AttributeUtil;

@ElementCategories({Category.Graphic, Category.TextContent})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.TextContentChild},
    anyOf = Anchor.class,
    charData = true
)
public final class Text extends LinearTextContainer<TextLayoutGroup> implements HasGeometryContext.ByDelegate,
        Renderable {
    public static final String TAG = "text";

    private HasGeometryContext geometryContext;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        geometryContext = HasGeometryContextImpl.parse(attributeNode);
    }

    @Override
    public @NotNull HasGeometryContext geometryContextDelegate() {
        return geometryContext;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible;
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof TextSegment || node instanceof TextLayoutGroup;
    }

    @Override
    protected void doAdd(@NotNull SVGNode node) {
        if (node instanceof TextSegment) {
            lastLinearLayoutGroup().segments().add((TextSegment) node);
        }
        if (node instanceof TextLayoutGroup) {
            children.add((TextLayoutGroup) node);
        }
    }

    @Override
    public void addContent(@NotNull TextContent.Segment content) {
        if (content.isConstant() && content.text().isEmpty()) return;
        if (children.isEmpty() && content.isConstant() && AttributeUtil.isBlank(content.text())) return;

        LinearTextLayoutGroup linearTextLayoutGroup = lastLinearLayoutGroup();
        linearTextLayoutGroup.segments().add(
                new StringTextSegment(this, linearTextLayoutGroup, linearTextLayoutGroup.segments().size(), content));
    }

    private @NotNull LinearTextLayoutGroup lastLinearLayoutGroup() {
        TextLayoutGroup lastGroup = children.isEmpty() ? null : children.get(children.size() - 1);
        if (!(lastGroup instanceof LinearTextLayoutGroup)) {
            lastGroup = new LinearTextLayoutGroup(this);
            children.add(lastGroup);
        }
        return (LinearTextLayoutGroup) lastGroup;
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Output output) {
        Point2D start = null;
        for (TextLayoutGroup layoutGroup : children()) {
            RenderContext currentContext = context;
            if (layoutGroup instanceof TextContainer) {
                currentContext = NodeRenderer.setupRenderContext(layoutGroup, context);
            }
            start = layoutGroup.renderText(start, currentContext, output);
        }
    }

    @Override
    protected @NotNull Shape glyphShape(@NotNull RenderContext context) {
        Path2D shape = new Path2D.Float();
        Point2D start = null;
        for (TextLayoutGroup layoutGroup : children()) {
            start = layoutGroup.appendGlyphShape(start, context, shape);
        }
        return shape;
    }

    @Override
    public @NotNull GlyphCursor createLocalCursor(boolean isInitial, @NotNull GlyphCursor current) {
        if (!isInitial) return current;
        return super.createLocalCursor(isInitial, current);
    }
}
