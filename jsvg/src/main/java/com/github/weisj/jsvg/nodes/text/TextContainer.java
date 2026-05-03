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
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.text.LengthAdjust;
import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.container.BaseContainerNode;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.HasVectorEffects;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasContextImpl;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;

abstract class TextContainer<T> extends BaseContainerNode<T>
        implements HasShape, HasContext.ByDelegate, HasVectorEffects {
    protected final List<@NotNull T> children = new ArrayList<>();

    protected boolean isVisible;
    protected AttributeFontSpec fontSpec;
    protected LengthAdjust lengthAdjust;
    protected Length textLength;

    private HasContext context;

    private Set<VectorEffect> vectorEffects;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        fontSpec = FontParser.parseFontSpec(attributeNode);
        lengthAdjust = attributeNode.getEnum("lengthAdjust", LengthAdjust.Spacing);
        textLength = attributeNode.getLength("textLength", PercentageDimension.NONE, Length.UNSPECIFIED);
        if (textLength.raw() < 0) textLength = Length.UNSPECIFIED;

        isVisible = Renderable.parseVisibility(attributeNode);
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
    public List<? extends @NotNull T> children() {
        return children;
    }

    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible;
    }

    abstract @NotNull Shape glyphShape(@NotNull RenderContext context);

    public final TextAnchor textAnchor(@NotNull RenderContext context) {
        return RenderContextAccessor.instance().fontRenderContext(context).textAnchor();
    }

    @Override
    public final @NotNull Shape untransformedElementShape(@NotNull RenderContext context, @NotNull Box box) {
        Shape shape = glyphShape(context);
        switch (box) {
            case BoundingBox:
                return shape;
            case StrokeBox:
                Area area = new Area(shape);
                area.add(new Area(context.stroke(1).createStrokedShape(shape)));
                return area;
            default:
                throw new IllegalStateException("Unexpected value: " + box);
        }
    }

    @Override
    public @NotNull Rectangle2D untransformedElementBounds(@NotNull RenderContext context, Box box) {
        // TODO: Bounding-box is specified by the character box.
        return untransformedElementShape(context, box).getBounds2D();
    }
}
