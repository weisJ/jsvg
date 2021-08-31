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
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.MaybeHasViewBox;
import com.github.weisj.jsvg.nodes.prototype.Renderable;

public final class NodeRenderer {
    private static final boolean CLIP_DEBUG = false;

    private NodeRenderer() {}

    public static void renderNode(@NotNull SVGNode node, @NotNull RenderContext context, @NotNull Graphics2D g) {
        if (!(node instanceof Renderable)) return;
        Renderable renderable = (Renderable) node;
        if (!renderable.isVisible(context)) return;
        RenderContext childContext = setupRenderContext(node, context);

        Graphics2D childGraphics = (Graphics2D) g.create();

        // Transform elements on a non top-level <svg> have no effect.
        if (!(renderable instanceof SVG)) {
            AffineTransform transform = renderable.transform();
            if (transform != null) childGraphics.transform(transform);
        }

        // Todo: When masks are implemented and we decide to paint to an off-screen buffer
        // we can handle clip shapes as if they were masks (with 1/0 values).
        Shape childClip = renderable.clipShape(context.measureContext());
        if (childClip != null) {
            if (CLIP_DEBUG) {
                childGraphics.setColor(Color.MAGENTA);
                childGraphics.draw(childClip);
            }
            childGraphics.clip(childClip);
        }

        renderable.render(childContext, childGraphics);
        childGraphics.dispose();
    }

    private static RenderContext setupRenderContext(@NotNull SVGNode node, @NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        @Nullable PaintContext paintContext = null;
        @Nullable AttributeFontSpec fontSpec = null;
        @Nullable FontRenderContext fontRenderContext = null;
        if (node instanceof HasContext) {
            paintContext = ((HasContext) node).paintContext();
            fontSpec = ((HasContext) node).fontSpec();
            fontRenderContext = ((HasContext) node).fontRenderContext();
        }
        @Nullable ViewBox viewBox = node instanceof MaybeHasViewBox
                ? ((MaybeHasViewBox) node).viewBox(measureContext)
                : null;
        return context.deriveWith(paintContext, fontSpec, viewBox, fontRenderContext);
    }
}
