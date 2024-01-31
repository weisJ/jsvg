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
package com.github.weisj.jsvg.nodes;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.geometry.AWTSVGShape;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.CommonInnerViewContainer;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.*;
import com.github.weisj.jsvg.renderer.PaintContext;

@ElementCategories({Category.Graphic, Category.GraphicsReferencing, Category.Structural})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Use extends RenderableSVGNode implements HasContext, HasShape, Instantiator {
    public static final String TAG = "use";

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private @Nullable SVGNode referencedNode;

    private PaintContext paintContext;
    private FontRenderContext fontRenderContext;
    private AttributeFontSpec fontSpec;
    private FillRule fillRule;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public @Nullable SVGNode referencedNode() {
        return referencedNode;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return super.isVisible(context) && referencedNode instanceof Renderable;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", 0);
        y = attributeNode.getLength("y", 0);
        width = attributeNode.getLength("width", Length.UNSPECIFIED);
        height = attributeNode.getLength("height", Length.UNSPECIFIED);

        String href = attributeNode.getValue("href");
        if (href == null) href = attributeNode.getValue("xlink:href");
        referencedNode = attributeNode.getElementByHref(SVGNode.class, href);

        paintContext = PaintContext.parse(attributeNode);
        fontRenderContext = FontRenderContext.parse(attributeNode);
        fontSpec = FontParser.parseFontSpec(attributeNode);
        fillRule = FillRule.parse(attributeNode);
    }

    @Override
    public @NotNull Shape untransformedElementShape(@NotNull RenderContext context) {
        // Todo: Inner views need to handle this differently
        return referencedNode instanceof HasShape
                ? ((HasShape) referencedNode)
                        .elementShape(NodeRenderer.createChildContext(referencedNode, context, this))
                : AWTSVGShape.EMPTY_SHAPE;
    }

    @Override
    public @NotNull Rectangle2D untransformedElementBounds(@NotNull RenderContext context) {
        // Todo: Inner views need to handle this differently
        return referencedNode instanceof HasShape
                ? ((HasShape) referencedNode)
                        .elementBounds(NodeRenderer.createChildContext(referencedNode, context, this))
                : AWTSVGShape.EMPTY_SHAPE;
    }

    @Override
    public @NotNull PaintContext paintContext() {
        return paintContext;
    }

    @Override
    public @NotNull FontRenderContext fontRenderContext() {
        return fontRenderContext;
    }

    @Override
    public @NotNull AttributeFontSpec fontSpec() {
        return fontSpec;
    }

    @Override
    public @NotNull FillRule fillRule() {
        return fillRule;
    }

    @Override
    public boolean canInstantiate(@NotNull SVGNode node) {
        return node instanceof CommonInnerViewContainer;
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Output output) {
        if (referencedNode != null) {
            MeasureContext measureContext = context.measureContext();
            context.translate(output, x.resolveWidth(measureContext), y.resolveHeight(measureContext));

            // Todo: Vector Effects

            try (NodeRenderer.Info info = NodeRenderer.createRenderInfo(referencedNode, context, output, this)) {
                if (info == null) return;
                if (referencedNode instanceof CommonInnerViewContainer) {
                    FloatSize targetViewBox = new FloatSize(Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW);
                    if (width.isSpecified()) targetViewBox.width = width.resolveWidth(measureContext);
                    if (height.isSpecified()) targetViewBox.height = height.resolveHeight(measureContext);
                    CommonInnerViewContainer view = (CommonInnerViewContainer) referencedNode;
                    view.renderWithSize(targetViewBox, view.viewBox(info.context), info.context, info.output());
                } else {
                    info.renderable.render(info.context, info.output());
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Use{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", referencedNode=" + (referencedNode != null ? referencedNode.id() : null) +
                ", styleContext=" + paintContext +
                ", fillRule=" + fillRule +
                '}';
    }
}
