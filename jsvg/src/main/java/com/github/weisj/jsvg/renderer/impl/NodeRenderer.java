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
package com.github.weisj.jsvg.renderer.impl;

import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.BaseInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.*;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.impl.context.ContextElementAttributes;
import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

public final class NodeRenderer {
    private NodeRenderer() {}

    public static void renderRootSVG(@NotNull SVG svgRoot, @NotNull RenderContext context, @NotNull Output output) {
        RenderContext viewContext = svgRoot.createInnerContextForViewBox(
                svgRoot.size(context), svgRoot.viewBox(context), context, output);
        try (Info info = createRenderInfo(svgRoot, viewContext, output, null)) {
            if (info != null) ((SVG) info.renderable()).renderWithEstablishedViewBox(info.context(), info.output());
        }
    }

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
        ElementBounds elementBounds = new ElementBounds(node, childContext);

        applyTransform(renderable, childOutput, childContext, elementBounds);

        Mask maskForIsolation = null;
        ClipPath clipPathForIsolation = null;
        if (renderable instanceof HasClip) {
            maskForIsolation = setupMask((HasClip) renderable, elementBounds, childOutput, childContext);

            ClipPath clipPath = setupClip((HasClip) renderable, elementBounds, childContext, childOutput);
            // Elements with an invalid clip shouldn't be painted
            if (clipPath != null && !clipPath.isValid()) return null;

            if (useAccurateMasking(childOutput)) {
                clipPathForIsolation = clipPath;
            }
        }

        Filter filter = null;
        if (renderable instanceof HasFilter) {
            filter = setupFilter((HasFilter) renderable, childOutput);
        }

        Info info = Info.InfoWithIsolation.create(renderable, childContext, childOutput, elementBounds,
                new IsolationEffects(filter, maskForIsolation, clipPathForIsolation));
        if (info != null) return info;

        return new Info(renderable, childContext, childOutput);
    }

    private static void applyTransform(@NotNull Renderable renderable, @NotNull Output childOutput,
            @NotNull RenderContext childContext, @NotNull ElementBounds elementBounds) {
        if (renderable instanceof Transformable && ((Transformable) renderable).shouldTransform()) {
            ((Transformable) renderable).applyTransform(childOutput, childContext, elementBounds);
        }
    }

    private static boolean checkInstantiation(@NotNull SVGNode node, @Nullable Instantiator instantiator,
            @NotNull Renderable renderable) {
        boolean instantiated = renderable.requiresInstantiation();
        return !instantiated || (instantiator != null && instantiator.canInstantiate(node));
    }

    private static @Nullable ClipPath setupClip(@NotNull HasClip renderable, @NotNull ElementBounds elementBounds,
            @NotNull RenderContext childContext, @NotNull Output childOutput) {
        ClipPath childClip = renderable.clipPath();
        if (childClip == null) return null;
        if (!childClip.isValid()) return childClip;

        childClip.applyClip(childOutput, childContext, elementBounds);

        return childClip;
    }

    private static @Nullable Mask setupMask(HasClip renderable, ElementBounds elementBounds, Output childOutput,
            RenderContext childContext) {
        Mask mask = renderable.mask();
        if (mask == null) return null;

        Rectangle2D bounds = elementBounds.geometryBox();
        if (bounds.isEmpty()) return null;

        if (useAccurateMasking(childOutput)) return mask;

        childOutput.setPaint(() -> mask.createMaskPaint(childOutput, childContext, elementBounds));
        return null;
    }

    private static @Nullable Filter setupFilter(@NotNull HasFilter hasFilter, @NotNull Output childOutput) {
        Filter filter = hasFilter.filter();

        if (filter != null && !(filter.hasEffect() && childOutput.supportsFilters())) {
            filter = null;
        }
        return filter;
    }

    private static boolean useAccurateMasking(@NotNull Output output) {
        return output.renderingHint(
                SVGRenderingHints.KEY_MASK_CLIP_RENDERING) == SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_ACCURACY;
    }

    public static @NotNull RenderContext setupRenderContext(@NotNull Object node, @NotNull RenderContext context) {
        return setupRenderContext(null, node, context);
    }

    private static @NotNull RenderContext setupRenderContext(@Nullable Instantiator instantiator, @NotNull Object node,
            @NotNull RenderContext context) {
        @Nullable Mutator<PaintContext> paintContext = null;
        @Nullable Mutator<MeasurableFontSpec> fontSpec = null;
        @Nullable FontRenderContext fontRenderContext = null;

        if (node instanceof HasPaintContext) paintContext = ((HasPaintContext) node).paintContext();
        if (node instanceof HasFontContext) fontSpec = ((HasFontContext) node).fontSpec();
        if (node instanceof HasFontRenderContext) fontRenderContext = ((HasFontRenderContext) node).fontRenderContext();

        @Nullable ContextElementAttributes contextElementAttributes = null;
        if (instantiator != null) contextElementAttributes = instantiator.createContextAttributes(context);

        return RenderContextAccessor.instance().deriveForNode(
                context, paintContext, fontSpec, fontRenderContext, contextElementAttributes, node);
    }

    public static @NotNull RenderContext setupInnerViewRenderContext(@NotNull ViewBox viewBox,
            @NotNull RenderContext context, boolean inheritAttributes) {
        return RenderContextAccessor.instance().setupInnerViewRenderContext(viewBox, context, inheritAttributes);
    }
}
