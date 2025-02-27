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
package com.github.weisj.jsvg.geometry;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class SVGRoundRectangle implements MeasurableShape {

    private final @NotNull RoundRectangle2D.Float rect = new RoundRectangle2D.Float();
    private final @NotNull LengthValue x;
    private final @NotNull LengthValue y;
    private final @NotNull LengthValue w;
    private final @NotNull LengthValue h;
    private final @NotNull LengthValue rx;
    private final @NotNull LengthValue ry;

    public SVGRoundRectangle(@NotNull LengthValue x, @NotNull LengthValue y, @NotNull LengthValue w,
            @NotNull LengthValue h,
            @NotNull LengthValue rx, @NotNull LengthValue ry) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.rx = rx;
        this.ry = ry;
    }

    private void validateShape(@NotNull MeasureContext measureContext) {
        rect.setRoundRect(
                x.resolve(measureContext),
                y.resolve(measureContext),
                w.resolve(measureContext),
                h.resolve(measureContext),
                Math.max(0, rx.resolve(measureContext) * 2),
                Math.max(0, ry.resolve(measureContext) * 2));
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return rect;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return rect.getBounds2D();
    }

    @Override
    public double pathLength(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        float a = rx.resolve(measureContext);
        float b = ry.resolve(measureContext);
        double l = 2 * ((w.resolve(measureContext) - 2 * a) + (h.resolve(measureContext) - 2 * b));
        if (a == b) {
            // All 4 corners together form a circle
            return l + SVGCircle.circumference(a);
        } else {
            // All 4 corners together form an ellipse
            return l + SVGEllipse.ellipseCircumference(a, b);
        }
    }

    @Override
    public boolean isClosed(@NotNull RenderContext context) {
        return true;
    }
}
