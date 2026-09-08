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
package com.github.weisj.jsvg.nodes.container;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.ViewContainer;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

public abstract class BaseInnerViewContainer extends CommonRenderableContainerNode implements ViewContainer {

    protected ViewBox viewBox;
    protected PreserveAspectRatio preserveAspectRatio;
    private Overflow overflow;

    protected abstract @NotNull Point2D outerLocation(@NotNull MeasureContext context);

    protected abstract @Nullable Point2D anchorLocation(@NotNull MeasureContext context);

    /** Reference-point adjustment in the outer viewport's coordinates. */
    protected @Nullable Point2D anchorLocation(@NotNull MeasureContext context,
            @Nullable AffineTransform viewTransform) {
        Point2D anchor = anchorLocation(context);
        if (anchor != null && viewTransform != null) {
            // anchorLocation returns the negated content reference point.
            viewTransform.deltaTransform(anchor, anchor);
            anchor.setLocation(
                    anchor.getX() - viewTransform.getTranslateX(),
                    anchor.getY() - viewTransform.getTranslateY());
        }
        return anchor;
    }

    protected abstract @NotNull Overflow defaultOverflow();

    @Override
    public @Nullable ViewBox viewBox(@NotNull RenderContext context) {
        return viewBox != null ? viewBox : new ViewBox(size(context));
    }

    public @NotNull ViewBox staticViewBox(@NotNull FloatSize fallbackSize) {
        return viewBox != null ? viewBox : new ViewBox(fallbackSize);
    }

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        viewBox = attributeNode.getViewBox();
        preserveAspectRatio = PreserveAspectRatio.parse(
                attributeNode.getValue("preserveAspectRatio"), attributeNode.parser());
        overflow = attributeNode.getEnum("overflow", defaultOverflow());
    }

    protected boolean inheritAttributes() {
        return true;
    }

    private @NotNull RenderContext createInnerContext(@NotNull RenderContext context,
            @NotNull ViewBox viewBox) {
        return NodeRenderer.setupInnerViewRenderContext(viewBox, context, inheritAttributes());
    }

    private @NotNull ViewBox computeOuterViewBox(@NotNull RenderContext context, @NotNull FloatSize useSiteSize) {
        MeasureContext measureContext = context.measureContext();
        Point2D outerPos = outerLocation(measureContext);
        ViewBox vb = new ViewBox(outerPos, useSiteSize);

        if (Length.isUnspecified(vb.width) || Length.isUnspecified(vb.height)) {
            FloatSize size = size(context);
            if (Length.isUnspecified(vb.width)) vb.width = size.width;
            if (Length.isUnspecified(vb.height)) vb.height = size.height;
        }

        return vb;
    }

    @Override
    public final @NotNull RenderContext createInnerContextForViewBox(@NotNull FloatSize useSiteSize,
            @Nullable ViewBox view, @NotNull RenderContext context, @NotNull Output output) {
        ViewBox outerViewBox = computeOuterViewBox(context, useSiteSize);
        ViewBox innerViewBox = view;

        // innerViewBox == null should behave as if it were (0,0,width,height).
        // If no viewBox is specified we can avoid the computation of the transform.
        AffineTransform viewTransform = innerViewBox != null
                ? preserveAspectRatio.computeViewportTransform(outerViewBox.size(), innerViewBox)
                : null;

        if (innerViewBox == null) {
            innerViewBox = new ViewBox(outerViewBox.size());
        }

        RenderContext innerContext = createInnerContext(context, innerViewBox);
        MeasureContext innerMeasure = innerContext.measureContext();
        Point2D anchorPos = anchorLocation(innerMeasure, viewTransform);

        // Clip the viewbox established at the use-site e.g. where an <svg> node is instantiated with <use>
        if (overflow.establishesClip()) {
            ViewBox clipViewBox = new ViewBox(outerViewBox);
            if (anchorPos != null) {
                clipViewBox.x += (float) anchorPos.getX();
                clipViewBox.y += (float) anchorPos.getY();
            }
            output.applyClip(clipViewBox);
        }

        innerContext.translate(output, outerViewBox.location());
        if (anchorPos != null) {
            innerContext.translate(output, anchorPos);
        }
        if (viewTransform != null) {
            // This also applies the translation to the inner viewbox location.
            innerContext.transform(output, viewTransform);
        }

        return innerContext;
    }
}
