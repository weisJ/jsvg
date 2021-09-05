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
import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.InnerViewContainer;
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
        try (Info info = createRenderInfo(node, context, g, false)) {
            if (info != null) info.renderable.render(info.context, info.g);
        }
    }

    public static @Nullable Info createRenderInfo(@NotNull SVGNode node, @NotNull RenderContext context,
            @NotNull Graphics2D g, boolean doInstantiate) {
        if (!(node instanceof Renderable)) return null;
        Renderable renderable = (Renderable) node;
        if (!doInstantiate && renderable.requiresInstantiation()) return null;
        if (!renderable.isVisible(context)) return null;
        RenderContext childContext = setupRenderContext(node, context);

        Graphics2D childGraphics = (Graphics2D) g.create();

        // Transform elements on a non top-level <svg> have no effect.
        if (!(renderable instanceof SVG) && renderable instanceof Transformable) {
            AffineTransform transform = ((Transformable) renderable).transform();
            if (transform != null) childGraphics.transform(transform);
        }

        if (renderable instanceof HasClip) {
            // Todo: When masks are implemented and we decide to paint to an off-screen buffer
            // we can handle clip shapes as if they were masks (with 1/0 values).
            ClipPath childClip = ((HasClip) renderable).clipPath();

            if (childClip != null) {
                // Elements with an invalid clip shouldn't be painted
                if (!childClip.isValid()) return null;
                // Todo: Is this using the correct measuring context?
                Shape childClipShape = childClip.shape(context);
                if (CLIP_DEBUG) {
                    childGraphics.setColor(Color.MAGENTA);
                    childGraphics.draw(childClipShape);
                }
                childGraphics.clip(childClipShape);
            }
        }

        return new Info(renderable, childContext, childGraphics);
    }

    public static @NotNull RenderContext setupRenderContext(@NotNull Object node, @NotNull RenderContext context) {
        // Inner views are excluded, as they have to establish their own context with a separate viewBox.
        if (node instanceof InnerViewContainer) return context;

        @Nullable PaintContext paintContext = null;
        @Nullable AttributeFontSpec fontSpec = null;
        @Nullable FontRenderContext fontRenderContext = null;
        if (node instanceof HasContext) {
            paintContext = ((HasContext) node).paintContext();
            fontSpec = ((HasContext) node).fontSpec();
            fontRenderContext = ((HasContext) node).fontRenderContext();
        }
        return context.deriveWith(paintContext, fontSpec, null, fontRenderContext);
    }

    public static @NotNull RenderContext setupInnerViewRenderContext(@NotNull InnerViewContainer node,
            @NotNull ViewBox viewBox, @NotNull RenderContext context) {
        PaintContext paintContext = node.paintContext();
        AttributeFontSpec fontSpec = node.fontSpec();
        FontRenderContext fontRenderContext = node.fontRenderContext();
        return context.deriveWith(paintContext, fontSpec, viewBox, fontRenderContext);
    }
}
