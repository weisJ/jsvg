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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
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
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.*;

public final class NodeRenderer {
    private static final boolean CLIP_DEBUG = false;

    private NodeRenderer() {}

    public static class Info implements AutoCloseable {
        public final @NotNull Renderable renderable;
        public final @NotNull RenderContext context;
        public final @NotNull Output output;

        Info(@NotNull Renderable renderable, @NotNull RenderContext context, @NotNull Output output) {
            this.renderable = renderable;
            this.context = context;
            this.output = output;
        }

        public @NotNull Output output() {
            return output;
        }

        @Override
        public void close() {
            output.dispose();
        }
    }

    private static final class InfoWithFilter extends Info {
        private final @NotNull Filter filter;
        private final @NotNull Filter.FilterInfo filterInfo;

        static @Nullable InfoWithFilter create(@NotNull Renderable renderable, @NotNull RenderContext context,
                @NotNull Output output, @NotNull Filter filter, @NotNull Rectangle2D elementBounds) {
            Filter.FilterInfo info = filter.createFilterInfo(output, context, elementBounds);
            if (info == null) return null;
            return new InfoWithFilter(renderable, context, output, filter, info);
        }

        private InfoWithFilter(@NotNull Renderable renderable, @NotNull RenderContext context, @NotNull Output output,
                @NotNull Filter filter, @NotNull Filter.FilterInfo filterInfo) {
            super(renderable, context, output);
            this.filter = filter;
            this.filterInfo = filterInfo;
        }

        @Override
        public @NotNull Output output() {
            return filterInfo.output();
        }

        @Override
        public void close() {
            filter.applyFilter(this.output, context, filterInfo);
            filterInfo.blitImage(this.output, context);
            filterInfo.close();
            super.close();
        }
    }

    public static void renderNode(@NotNull SVGNode node, @NotNull RenderContext context, @NotNull Output output) {
        try (Info info = createRenderInfo(node, context, output, null)) {
            if (info != null) info.renderable.render(info.context, info.output());
        }
    }

    public static @NotNull RenderContext createChildContext(@NotNull SVGNode node, @NotNull RenderContext context,
            @Nullable Instantiator instantiator) {
        return setupRenderContext(instantiator, node, context);
    }

    public static @Nullable Info createRenderInfo(@NotNull SVGNode node, @NotNull RenderContext context,
            @NotNull Output output, @Nullable Instantiator instantiator) {
        if (!(node instanceof Renderable)) return null;
        Renderable renderable = (Renderable) node;
        boolean instantiated = renderable.requiresInstantiation();
        if (instantiated && (instantiator == null || !instantiator.canInstantiate(node))) {
            return null;
        }
        if (!renderable.isVisible(context)) return null;
        RenderContext childContext = createChildContext(node, context, instantiator);

        Output childOutput = output.createChild();

        if (renderable instanceof Transformable && ((Transformable) renderable).shouldTransform()) {
            ((Transformable) renderable).applyTransform(childOutput, childContext);
        }

        Rectangle2D elementBounds = null;
        if (renderable instanceof HasClip) {

            Mask mask = ((HasClip) renderable).mask();
            if (mask != null) {
                // Todo: Proper object bounding box
                elementBounds = elementBounds(renderable, childContext);
                if (!elementBounds.isEmpty()) {
                    childOutput.setPaint(mask.createMaskPaint(childOutput, childContext, elementBounds));
                }
            }

            ClipPath childClip = ((HasClip) renderable).clipPath();

            if (childClip != null) {
                // Elements with an invalid clip shouldn't be painted
                if (!childClip.isValid()) return null;
                if (elementBounds == null) elementBounds = elementBounds(renderable, childContext);

                Shape childClipShape = childClip.clipShape(childContext, elementBounds);

                if (CLIP_DEBUG) {
                    output.debugPaint(g -> {
                        g.setClip(null);
                        g.setPaint(Color.MAGENTA);
                        g.draw(childClipShape);
                    });
                }

                output.applyClip(childClipShape);
            }
        }

        Info info = tryCreateFilterInfo(renderable, childContext, childOutput, elementBounds);
        if (info != null) return info;

        return new Info(renderable, childContext, childOutput);
    }

    private static @Nullable InfoWithFilter tryCreateFilterInfo(
            @NotNull Renderable renderable, @NotNull RenderContext childContext, @NotNull Output childOutput,
            @Nullable Rectangle2D elementBounds) {
        Filter filter = renderable instanceof HasFilter
                ? ((HasFilter) renderable).filter()
                : null;

        if (filter != null && filter.hasEffect()) {
            if (elementBounds == null) elementBounds = elementBounds(renderable, childContext);
            return InfoWithFilter.create(renderable, childContext, childOutput, filter, elementBounds);
        }
        return null;
    }

    private static @NotNull Rectangle2D elementBounds(@NotNull Object node, @NotNull RenderContext childContext) {
        Rectangle2D elementBounds;
        if (node instanceof HasShape) {
            elementBounds = ((HasShape) node).untransformedElementBounds(childContext);
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
                    Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW);
            return new RenderContext(
                    context.platformSupport(),
                    new AffineTransform(),
                    new AffineTransform(),
                    PaintContext.createDefault(),
                    newMeasure,
                    FontRenderContext.createDefault(),
                    MeasurableFontSpec.createDefault(),
                    context.fillRule(),
                    context.contextElementAttributes());
        }
    }
}
