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

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class BaseInnerViewContainer extends RenderableContainerNode {

    protected ViewBox viewBox;
    protected PreserveAspectRatio preserveAspectRatio;

    protected abstract Point2D outerLocation(@NotNull MeasureContext context);

    protected abstract Point2D innerLocation(@NotNull MeasureContext context);

    public abstract @NotNull FloatSize size(@NotNull RenderContext context);

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        viewBox = attributeNode.getViewBox();
        preserveAspectRatio = PreserveAspectRatio.parse(attributeNode.getValue("preserveAspectRatio"));
    }

    @Override
    public final void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        renderAtLocation(null, context, g);
    }

    public void renderAtLocation(@Nullable ViewBox useSiteViewBox, @NotNull RenderContext context,
            @NotNull Graphics2D g) {
        MeasureContext measureContext = context.measureContext();
        Point2D outerPos = outerLocation(measureContext);
        float xPos = (float) outerPos.getX();
        float yPos = (float) outerPos.getY();
        if (useSiteViewBox != null) {
            if (useSiteViewBox.hasSpecifiedX()) xPos = useSiteViewBox.x;
            if (useSiteViewBox.hasSpecifiedY()) yPos = useSiteViewBox.y;
        }
        g.translate(xPos, yPos);

        AffineTransform viewTransform = null;
        FloatSize size;
        if (viewBox != null) {
            if (useSiteViewBox != null && useSiteViewBox.hasSpecifiedWidth() && useSiteViewBox.hasSpecifiedHeight()) {
                size = useSiteViewBox.size();
            } else {
                size = size(context);
                if (useSiteViewBox != null && useSiteViewBox.hasSpecifiedWidth()) size.width = useSiteViewBox.width;
                if (useSiteViewBox != null && useSiteViewBox.hasSpecifiedHeight()) size.height = useSiteViewBox.height;
            }
            viewTransform = preserveAspectRatio.computeViewPortTransform(size, viewBox);
        } else {
            size = size(context);
        }
        FloatSize viewSize = viewBox != null ? viewBox.size() : size;

        RenderContext innerContext = NodeRenderer.setupInnerViewRenderContext(new ViewBox(viewSize), context);
        MeasureContext innerMeasure = innerContext.measureContext();

        Point2D innerPos = innerLocation(innerMeasure);
        if (viewTransform != null) {
            innerPos.setLocation(
                    innerPos.getX() * viewTransform.getScaleX(),
                    innerPos.getY() * viewTransform.getScaleY());
        }

        g.translate(innerPos.getX(), innerPos.getY());

        // Todo: This should be determined by the overflow parameter
        g.clipRect(0, 0, (int) size.width, (int) size.height);

        if (viewTransform != null) g.transform(viewTransform);
        super.render(innerContext, g);
    }
}
