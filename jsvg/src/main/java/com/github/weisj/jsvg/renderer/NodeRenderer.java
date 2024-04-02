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
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.BaseInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.*;

public final class NodeRenderer {
    private static final boolean CLIP_DEBUG = false;

    private NodeRenderer() {}

    public static void renderNode(@NotNull SVGNode node, @NotNull RenderContext context, @NotNull Output output) {
        renderNode(node, context, output, null);
    }

    public static void renderNode(@NotNull SVGNode node, @NotNull RenderContext context, @NotNull Output output,
            @Nullable Instantiator instantiator) {
        try (Info info = createRenderInfo(node, context, output, instantiator)) {
            if (info != null) info.renderable().render(info.context(), info.output());
        }
    }

    public static void renderWithSize(@NotNull BaseInnerViewContainer node, @NotNull FloatSize size,
            @NotNull RenderContext context, @NotNull Output output,
            @Nullable Instantiator instantiator) {
        try (Info info = createRenderInfo(node, context, output, instantiator)) {
            if (info != null) node.renderWithSize(size, node.viewBox(info.context()), info.context(), info.output());
        }
    }

    public static @NotNull RenderContext createChildContext(@NotNull Renderable node, @NotNull RenderContext context,
            @Nullable Instantiator instantiator) {
        if (!node.shouldEstablishChildContext()) return context;
        return setupRenderContext(instantiator, node, context);
    }

    private static @Nullable Info createRenderInfo(@NotNull SVGNode node, @NotNull RenderContext context,
            @NotNull Output output, @Nullable Instantiator instantiator) {
        if (!(node instanceof Renderable)) return null;

        Renderable renderable = (Renderable) node;

        if (!checkInstantiation(node, instantiator, renderable)) return null;
        if (!renderable.isVisible(context)) return null;

        RenderContext childContext = createChildContext(renderable, context, instantiator);
        Output childOutput = output.createChild();
        ElementBounds elementBounds = new ElementBounds(renderable, childContext);

        applyTransform(renderable, childOutput, childContext);

        if (renderable instanceof HasClip) {
            setupMask((HasClip) renderable, elementBounds, childOutput, childContext);
            if (!setupClip((HasClip) renderable, elementBounds, childContext, childOutput)) return null;
        }

        Info info = tryCreateFilterInfo(renderable, childContext, childOutput, elementBounds);
        if (info != null) return info;

        return new Info(renderable, childContext, childOutput);
    }

    private static void applyTransform(@NotNull Renderable renderable, @NotNull Output childOutput,
            @NotNull RenderContext childContext) {
        if (renderable instanceof Transformable && ((Transformable) renderable).shouldTransform()) {
            ((Transformable) renderable).applyTransform(childOutput, childContext);
        }
    }

    private static boolean checkInstantiation(@NotNull SVGNode node, @Nullable Instantiator instantiator,
            @NotNull Renderable renderable) {
        boolean instantiated = renderable.requiresInstantiation();
        return !instantiated || (instantiator != null && instantiator.canInstantiate(node));
    }

    private static boolean setupClip(@NotNull HasClip renderable, @NotNull ElementBounds elementBounds,
            @NotNull RenderContext childContext, @NotNull Output childOutput) {
        ClipPath childClip = renderable.clipPath();

        if (childClip != null) {
            // Elements with an invalid clip shouldn't be painted
            if (!childClip.isValid()) return false;

            if (childOutput.isSoftClippingEnabled()) {
                Rectangle2D bounds = elementBounds.geometryBox();
                if (!bounds.isEmpty()) {
                    Shape childClipShape = childClip.clipShape(childContext, elementBounds, true);

                    childOutput.setPaint(() -> childClip.createPaintForSoftClipping(
                            childOutput, childContext, elementBounds, childClipShape));
                }
            } else {
                Shape childClipShape = childClip.clipShape(childContext, elementBounds, false);

                if (CLIP_DEBUG) {
                    childOutput.debugPaint(g -> {
                        g.setClip(null);
                        g.setPaint(Color.MAGENTA);
                        g.draw(childClipShape);
                    });
                }

                childOutput.applyClip(childClipShape);
            }
        }
        return true;
    }

    private static void setupMask(HasClip renderable, ElementBounds elementBounds, Output childOutput,
            RenderContext childContext) {
        Mask mask = renderable.mask();
        if (mask != null) {
            // Todo: Proper object bounding box

            Rectangle2D bounds = elementBounds.geometryBox();
            if (!bounds.isEmpty()) {
                childOutput.setPaint(() -> mask.createMaskPaint(childOutput, childContext, elementBounds));
            }
        }
    }

    private static @Nullable Info.InfoWithFilter tryCreateFilterInfo(
            @NotNull Renderable renderable, @NotNull RenderContext childContext, @NotNull Output childOutput,
            @NotNull ElementBounds elementBounds) {
        Filter filter = renderable instanceof HasFilter
                ? ((HasFilter) renderable).filter()
                : null;

        if (filter != null && filter.hasEffect() && childOutput.supportsFilters()) {
            return Info.InfoWithFilter.create(renderable, childContext, childOutput, filter, elementBounds);
        }
        return null;
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
