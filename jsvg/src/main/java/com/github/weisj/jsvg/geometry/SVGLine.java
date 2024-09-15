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
package com.github.weisj.jsvg.geometry;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.LengthValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class SVGLine implements MeasurableShape {

    private final @NotNull Line2D.Float line = new Line2D.Float();
    private final LengthValue x1;
    private final LengthValue y1;
    private final LengthValue x2;
    private final LengthValue y2;

    public SVGLine(@NotNull LengthValue x1, @NotNull LengthValue y1, @NotNull LengthValue x2, @NotNull LengthValue y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public boolean canBeFilled() {
        return false;
    }

    private void validateShape(@NotNull MeasureContext measureContext) {
        line.setLine(
                x1.resolveWidth(measureContext), y1.resolveHeight(measureContext),
                x2.resolveWidth(measureContext), y2.resolveHeight(measureContext));
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return line;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return line.getBounds2D();
    }

    @Override
    public double pathLength(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        return GeometryUtil.lineLength(
                x1.resolveWidth(measureContext), y1.resolveHeight(measureContext),
                x2.resolveWidth(measureContext), y2.resolveHeight(measureContext));
    }
}
