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
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class SVGRectangle implements MeasurableShape {

    private final @NotNull Rectangle2D.Float rect = new Rectangle2D.Float();
    private final @NotNull LengthValue x;
    private final @NotNull LengthValue y;
    private final @NotNull LengthValue w;
    private final @NotNull LengthValue h;

    public SVGRectangle(@NotNull LengthValue x, @NotNull LengthValue y, @NotNull LengthValue w,
            @NotNull LengthValue h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    private void validateShape(@NotNull MeasureContext measureContext) {
        rect.setRect(
                x.resolve(measureContext),
                y.resolve(measureContext),
                w.resolve(measureContext),
                h.resolve(measureContext));
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return rect;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (validate) validateShape(context.measureContext());
        return rect;
    }

    @Override
    public double pathLength(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        return 2 * (w.resolve(measureContext) + h.resolve(measureContext));
    }
}
