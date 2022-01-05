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
package com.github.weisj.jsvg.nodes.container;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.*;
import com.github.weisj.jsvg.nodes.prototype.impl.HasContextImpl;
import com.github.weisj.jsvg.nodes.prototype.impl.HasGeometryContextImpl;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.PaintContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class CommonRenderableContainerNode extends BaseContainerNode<SVGNode>
        implements Renderable, HasGeometryContext.ByDelegate, HasContext {
    private final List<@NotNull SVGNode> children = new ArrayList<>();

    private boolean isVisible;
    private HasGeometryContext geometryContext;
    private HasContext context;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        isVisible = parseIsVisible(attributeNode);
        geometryContext = HasGeometryContextImpl.parse(attributeNode);
        context = HasContextImpl.parse(attributeNode);
    }

    @Override
    public @NotNull HasGeometryContext geometryContextDelegate() {
        return geometryContext;
    }

    @Override
    protected void doAdd(@NotNull SVGNode node) {
        children.add(node);
    }

    @Override
    public List<? extends @NotNull SVGNode> children() {
        return children;
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        for (SVGNode child : children()) {
            NodeRenderer.renderNode(child, context, g);
        }
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible;
    }

    @Override
    public final @NotNull Mutator<PaintContext> paintContext() {
        return context.paintContext();
    }

    @Override
    public final @NotNull FontRenderContext fontRenderContext() {
        return context.fontRenderContext();
    }

    @Override
    public final @NotNull Mutator<MeasurableFontSpec> fontSpec() {
        return context.fontSpec();
    }

    @Override
    public final @NotNull FillRule fillRule() {
        return context.fillRule();
    }
}
