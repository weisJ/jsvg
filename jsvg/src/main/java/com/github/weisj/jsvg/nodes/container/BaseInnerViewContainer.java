/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
import com.github.weisj.jsvg.nodes.*;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

public abstract class BaseInnerViewContainer extends CommonRenderableContainerNode {

    protected ViewBox viewBox;
    protected PreserveAspectRatio preserveAspectRatio;
    private Overflow overflow;

    protected abstract @NotNull Point2D outerLocation(@NotNull MeasureContext context);

    protected abstract @Nullable Point2D anchorLocation(@NotNull MeasureContext context);

    public abstract @NotNull FloatSize size(@NotNull RenderContext context);

    protected abstract @NotNull Overflow defaultOverflow();

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

    protected void renderWithCurrentViewBox(@NotNull RenderContext context, @NotNull Output output) {
        super.render(context, output);
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Output output) {
        renderWithSize(size(context), viewBox(context), context, output);
    }

    protected @NotNull RenderContext createInnerContext(@NotNull RenderContext context, @NotNull ViewBox viewBox) {
        return NodeRenderer.setupInnerViewRenderContext(viewBox, context, true);
    }

    public final void renderWithSize(@NotNull FloatSize useSiteSize, @Nullable ViewBox view,
            @NotNull RenderContext context, @NotNull Output output) {
        MeasureContext measureContext = context.measureContext();

        Point2D outerPos = outerLocation(measureContext);

        if (Length.isUnspecified(useSiteSize.width) || Length.isUnspecified(useSiteSize.height)) {
            FloatSize size = size(context);
            if (Length.isUnspecified(useSiteSize.width)) useSiteSize.width = size.width;
            if (Length.isUnspecified(useSiteSize.height)) useSiteSize.height = size.height;
        }

        AffineTransform viewTransform = view != null
                ? this.preserveAspectRatio.computeViewportTransform(useSiteSize, view)
                : null;
        FloatSize viewSize = view != null
                ? view.size()
                : useSiteSize;

        RenderContext innerContext = createInnerContext(context, new ViewBox(viewSize));
        MeasureContext innerMeasure = innerContext.measureContext();

        innerContext.translate(output, outerPos);

        Point2D anchorPos = anchorLocation(innerMeasure);
        if (anchorPos != null) {
            if (viewTransform != null) {
                // This is safe to do as computeViewportTransform will never produce shear or rotation transforms.
                anchorPos.setLocation(
                        anchorPos.getX() * viewTransform.getScaleX() - viewTransform.getTranslateX(),
                        anchorPos.getY() * viewTransform.getScaleY() - viewTransform.getTranslateY());
            }
            innerContext.translate(output, anchorPos);
        }

        // Clip the viewbox established at the use-site e.g. where an <svg> node is instantiated with <use>
        if (overflow.establishesClip()) output.applyClip(new ViewBox(useSiteSize));

        if (viewTransform != null) {
            innerContext.transform(output, viewTransform);
        }

        if (this instanceof SVG && ((SVG) this).isTopLevel()) {
            // Needed for vector-effects to work properly.
            RenderContextAccessor.Accessor accessor = RenderContextAccessor.instance();
            accessor.setRootTransform(context, output.transform());
            accessor.setRootTransform(innerContext, output.transform());

            // If this element itself specifies a viewbox we have to respect its clipping rules.
            if (viewTransform != null && overflow.establishesClip()) output.applyClip(view);
        }
        renderWithCurrentViewBox(innerContext, output);
    }
}
