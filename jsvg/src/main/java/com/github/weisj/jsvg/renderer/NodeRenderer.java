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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.*;

public final class NodeRenderer {
    private static final boolean CLIP_DEBUG = false;

    private NodeRenderer() {}

    public static class Info implements AutoCloseable {
        public final Renderable renderable;
        public final RenderContext context;
        public final Graphics2D g;

        public Info(Renderable renderable, RenderContext context, Graphics2D g) {
            this.renderable = renderable;
            this.context = context;
            this.g = g;
        }

        @Override
        public void close() {
            g.dispose();
        }
    }

    public static void renderNode(@NotNull SVGNode node, @NotNull RenderContext context, @NotNull Graphics2D g) {
        try (Info info = createRenderInfo(node, context, g, null)) {
            if (info != null) info.renderable.render(info.context, info.g);
        }
    }

    public static @NotNull RenderContext createChildContext(@NotNull SVGNode node, @NotNull RenderContext context,
            @Nullable Instantiator instantiator) {
        return setupRenderContext(instantiator, node, context);
    }

    public static @Nullable Info createRenderInfo(@NotNull SVGNode node, @NotNull RenderContext context,
            @NotNull Graphics2D g, @Nullable Instantiator instantiator) {
        if (!(node instanceof Renderable)) return null;
        Renderable renderable = (Renderable) node;
        boolean instantiated = renderable.requiresInstantiation();
        if (instantiated && (instantiator == null || !instantiator.canInstantiate(node))) {
            return null;
        }
        if (!renderable.isVisible(context)) return null;
        RenderContext childContext = createChildContext(node, context, instantiator);

        Graphics2D childGraphics = (Graphics2D) g.create();

        if (renderable instanceof Transformable) {
            ((Transformable) renderable).applyTransform(childGraphics, childContext.measureContext());
        }

        if (renderable instanceof HasClip) {
            Rectangle2D elementBounds = null;
            // Todo: When masks are implemented and we decide to paint to an off-screen buffer
            // we can handle clip shapes as if they were masks (with 1/0 values).
            ClipPath childClip = ((HasClip) renderable).clipPath();

            if (childClip != null) {
                // Elements with an invalid clip shouldn't be painted
                if (!childClip.isValid()) return null;
                elementBounds = elementBounds(renderable, childContext);

                Shape childClipShape = childClip.clipShape(childContext, elementBounds);
                if (CLIP_DEBUG) {
                    Paint paint = childGraphics.getPaint();
                    Shape clip = childGraphics.getClip();
                    childGraphics.setClip(null);
                    childGraphics.setPaint(Color.MAGENTA);
                    childGraphics.draw(childClipShape);
                    childGraphics.setPaint(paint);
                    childGraphics.setClip(clip);
                }
                childGraphics.clip(childClipShape);
            }

            Mask mask = ((HasClip) renderable).mask();
            if (mask != null) {
                // Todo: Proper object bounding box
                if (elementBounds == null) elementBounds = elementBounds(renderable, childContext);
                GraphicsUtil.safelySetPaint(childGraphics, mask.createMaskPaint(g, childContext, elementBounds));
            }
        }

        return new Info(renderable, childContext, childGraphics);
    }

    private static @NotNull Rectangle2D elementBounds(@NotNull Object node, @NotNull RenderContext childContext) {
        Rectangle2D elementBounds;
        if (node instanceof HasShape) {
            elementBounds = ((HasShape) node).elementBounds(childContext);
        } else {
            MeasureContext measureContext = childContext.measureContext();
            elementBounds = new ViewBox(measureContext.viewWidth(), measureContext.viewHeight());
        }
        return elementBounds;
    }


    public static @NotNull RenderContext setupRenderContext(@NotNull Object node, @NotNull RenderContext context) {
        return setupRenderContext(null, node, context);
    }

    private static @NotNull RenderContext setupRenderContext(@Nullable Instantiator instantiator, @NotNull Object node,
            @NotNull RenderContext context) {
        @Nullable Mutator<PaintContext> paintContext = null;
        @Nullable Mutator<MeasurableFontSpec> fontSpec = null;
        @Nullable FontRenderContext fontRenderContext = null;
        @Nullable FillRule fillRule = null;

        if (node instanceof HasPaintContext) paintContext = ((HasPaintContext) node).paintContext();
        if (node instanceof HasFontContext) fontSpec = ((HasFontContext) node).fontSpec();
        if (node instanceof HasFontRenderContext) fontRenderContext = ((HasFontRenderContext) node).fontRenderContext();
        if (node instanceof HasFillRule) fillRule = ((HasFillRule) node).fillRule();

        @Nullable ContextElementAttributes contextElementAttributes = null;
        if (instantiator != null) contextElementAttributes = instantiator.createContextAttributes(context);

        return context.derive(paintContext, fontSpec, null, fontRenderContext, fillRule, contextElementAttributes);
    }

    public static @NotNull RenderContext setupInnerViewRenderContext(@NotNull ViewBox viewBox,
            @NotNull RenderContext context, boolean inheritAttributes) {
        if (inheritAttributes) {
            return context.derive(null, null, viewBox, null, null, null);
        } else {
            MeasureContext newMeasure = context.measureContext().derive(viewBox,
                    Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW, FontRenderContext.createDefault());
            return new RenderContext(
                    context.targetComponent(),
                    PaintContext.createDefault(),
                    newMeasure,
                    MeasurableFontSpec.createDefault(),
                    context.fillRule(),
                    context.contextElementAttributes());
        }
    }
}
