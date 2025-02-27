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
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class SVGEllipse implements MeasurableShape {

    private final @NotNull Ellipse2D.Float ellipse = new Ellipse2D.Float();
    private final @NotNull LengthValue cx;
    private final @NotNull LengthValue cy;
    private final @NotNull LengthValue rx;
    private final @NotNull LengthValue ry;

    public SVGEllipse(@NotNull LengthValue cx, @NotNull LengthValue cy, @NotNull LengthValue rx,
            @NotNull LengthValue ry) {
        this.cx = cx;
        this.cy = cy;
        this.rx = rx;
        this.ry = ry;
    }

    private void validateShape(@NotNull MeasureContext measureContext) {
        float x = cx.resolve(measureContext);
        float y = cy.resolve(measureContext);
        float rrx = rx.resolve(measureContext);
        float rry = ry.resolve(measureContext);
        ellipse.setFrame(x - rrx, y - rry, 2 * rrx, 2 * rry);
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return ellipse;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return ellipse.getBounds2D();
    }

    @Override
    public double pathLength(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        float a = rx.resolve(measureContext);
        float b = ry.resolve(measureContext);
        if (a == b) return SVGCircle.circumference(a);
        return ellipseCircumference(a, b);
    }

    @Override
    public boolean isClosed(@NotNull RenderContext context) {
        return true;
    }

    static double ellipseCircumference(double a, double b) {
        // This cannot be computed exactly as the circumference is given by an elliptic integral which
        // doesn't have a solution in terms of elementary functions.
        // The following approximation is due to Hudson
        double h = ((a - b) * (a - b)) / ((a + b) * (a + b));
        double h4 = h / 4f;
        return 0.25f * Math.PI * (a + b) * (3 * (1 + h4) + (1 / (1f - h4)));
    }
}
