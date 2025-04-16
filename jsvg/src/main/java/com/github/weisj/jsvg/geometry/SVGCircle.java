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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class SVGCircle implements SVGShape {

    private final @NotNull Ellipse2D.Float circle = new Ellipse2D.Float();
    private final @NotNull LengthValue cx;
    private final @NotNull LengthValue cy;
    private final @NotNull LengthValue r;

    public SVGCircle(@NotNull LengthValue cx, @NotNull LengthValue cy, @NotNull LengthValue r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
    }

    private void validateShape(@NotNull MeasureContext measureContext) {
        float x = cx.resolve(measureContext);
        float y = cy.resolve(measureContext);
        float rr = r.resolve(measureContext);
        circle.setFrame(x - rr, y - rr, 2 * rr, 2 * rr);
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return circle;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return circle.getBounds2D();
    }

    @Override
    public double pathLength(@NotNull RenderContext context) {
        return circumference(r.resolve(context.measureContext()));
    }

    @Override
    public boolean isClosed(@NotNull RenderContext context) {
        return true;
    }

    static double circumference(double radius) {
        return 2 * Math.PI * radius;
    }
}
