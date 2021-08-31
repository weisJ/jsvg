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

import com.github.weisj.jsvg.attributes.paint.SVGPaint;

public final class ShapeRenderer {
    private ShapeRenderer() {}

    public static void renderShape(@NotNull RenderContext context,
            @NotNull PaintContext paintContext, @NotNull Graphics2D g,
            @NotNull Shape shape, @NotNull Rectangle2D bounds,
            boolean allowFill, boolean allowOutline) {
        float absOpacity = context.opacity(paintContext.opacity);
        float fOpacity = context.fillOpacity(paintContext.fillOpacity) * absOpacity;
        SVGPaint fPaint = context.fillPaint(paintContext.fillPaint);

        float sOpacity = context.strokeOpacity(paintContext.strokeOpacity) * absOpacity;
        SVGPaint sPaint = context.strokePaint(paintContext.strokePaint);

        boolean doFill = allowFill && fOpacity > 0 && fPaint.isVisible();
        boolean doOutline = allowOutline && sOpacity > 0 && sPaint.isVisible();

        if (doFill || doOutline) {
            if (doFill) {
                g.setComposite(AlphaComposite.SrcOver.derive(fOpacity));
                g.setPaint(fPaint.paintForBounds(bounds));
                g.fill(shape);
            }
            if (doOutline) {
                g.setComposite(AlphaComposite.SrcOver.derive(sOpacity));
                g.setPaint(sPaint.paintForBounds(bounds));
                // Todo: Handle stroke in PaintContext if (stroke != null) g.setStroke(stroke);
                g.draw(shape);
            }
        }
    }
}
