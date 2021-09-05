/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class InnerViewContainer extends RenderableContainerNode implements ShapedContainer<SVGNode> {
    protected Length x;
    protected Length y;
    protected Length width;
    protected Length height;

    protected ViewBox viewBox;
    protected PreserveAspectRatio preserveAspectRatio;

    protected Point2D outerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(x.resolveWidth(context), y.resolveHeight(context));
    }

    protected Point2D innerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(0, 0);
    }

    public @NotNull FloatSize size(@NotNull MeasureContext context) {
        if (width.isSpecified() && height.isSpecified()) {
            return new FloatSize(width.resolveWidth(context), height.resolveHeight(context));
        }
        Rectangle2D bounds = bounds(context, true);
        return new FloatSize((float) bounds.getWidth(), (float) bounds.getHeight());
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return !width.isZero() && !height.isZero() && super.isVisible(context);
    }

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", 0);
        y = attributeNode.getLength("y", 0);
        viewBox = parseViewBox(attributeNode);
        width = attributeNode.getLength("width",
                viewBox != null ? Unit.Raw.valueOf(viewBox.width) : Length.UNSPECIFIED);
        height = attributeNode.getLength("height",
                viewBox != null ? Unit.Raw.valueOf(viewBox.height) : Length.UNSPECIFIED);
        preserveAspectRatio = PreserveAspectRatio.parse(attributeNode.getValue("preserveAspectRatio"));
    }

    private @Nullable ViewBox parseViewBox(@NotNull AttributeNode attributeNode) {
        float[] viewBoxCords = attributeNode.getFloatList("viewBox");
        return viewBoxCords.length == 4 ? new ViewBox(viewBoxCords) : null;
    }

    @Override
    public final void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        renderByUse(null, context, g);
    }

    public void renderByUse(@Nullable ViewBox useSiteViewBox, @NotNull RenderContext context, @NotNull Graphics2D g) {
        MeasureContext measureContext = context.measureContext();
        Point2D outerPos = outerLocation(measureContext);
        float xPos = (float) outerPos.getX();
        float yPos = (float) outerPos.getY();
        if (useSiteViewBox != null) {
            if (useSiteViewBox.hasSpecifiedX()) xPos = useSiteViewBox.x;
            if (useSiteViewBox.hasSpecifiedY()) yPos = useSiteViewBox.y;
        }
        g.translate(xPos, yPos);

        FloatSize size;
        if (viewBox != null) {
            if (useSiteViewBox != null && useSiteViewBox.hasSpecifiedWidth() && useSiteViewBox.hasSpecifiedHeight()) {
                size = useSiteViewBox.size();
            } else {
                size = size(measureContext);
                if (useSiteViewBox != null && useSiteViewBox.hasSpecifiedWidth()) size.width = useSiteViewBox.width;
                if (useSiteViewBox != null && useSiteViewBox.hasSpecifiedHeight()) size.height = useSiteViewBox.height;
            }
            AffineTransform viewTransform = preserveAspectRatio.computeViewPortTransform(size, viewBox);
            g.transform(viewTransform);
        } else {
            size = size(measureContext);
        }
        FloatSize viewSize = viewBox != null ? viewBox.size() : size;

        RenderContext innerContext = NodeRenderer.setupInnerViewRenderContext(this, new ViewBox(viewSize), context);
        MeasureContext innerMeasure = innerContext.measureContext();
        Point2D innerPos = innerLocation(innerMeasure);
        g.translate(innerPos.getX(), innerPos.getY());
        // Todo: This should be determined by the overflow parameter
        g.clipRect(0, 0, (int) innerMeasure.viewWidth(), (int) innerMeasure.viewHeight());
        super.render(innerContext, g);
    }
}
