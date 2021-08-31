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
package com.github.weisj.jsvg.geometry;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public class SVGRectangle implements SVGShape {

    private final @NotNull Rectangle2D.Float rect = new Rectangle2D.Float();
    private final @NotNull Length x;
    private final @NotNull Length y;
    private final @NotNull Length w;
    private final @NotNull Length h;

    public SVGRectangle(@NotNull Length x, @NotNull Length y, @NotNull Length w, @NotNull Length h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    private void validateShape(@NotNull MeasureContext measureContext) {
        rect.setRect(
                x.resolveWidth(measureContext),
                y.resolveHeight(measureContext),
                w.resolveWidth(measureContext),
                h.resolveHeight(measureContext));
    }

    @Override
    public @NotNull Shape shape(@NotNull MeasureContext measureContext, boolean validate) {
        if (validate) validateShape(measureContext);
        return rect;
    }

    @Override
    public Rectangle2D bounds(@NotNull MeasureContext measureContext, boolean validate) {
        if (validate) validateShape(measureContext);
        return rect;
    }
}
