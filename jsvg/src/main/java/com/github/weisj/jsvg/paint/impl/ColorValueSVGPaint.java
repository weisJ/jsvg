/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.paint.impl;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;

/** An {@link SVGPaint} view of a {@link ColorValue} which evaluates the value at render time. */
public final class ColorValueSVGPaint implements SVGPaint {
    private final @NotNull ColorValue colorValue;

    private ColorValueSVGPaint(@NotNull ColorValue colorValue) {
        this.colorValue = colorValue;
    }

    public static @NotNull SVGPaint create(@NotNull ColorValue value) {
        if (value instanceof SVGPaint) return (SVGPaint) value;
        return new ColorValueSVGPaint(value);
    }

    public @NotNull ColorValue colorValue() {
        return colorValue;
    }

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        output.setPaint(color(context));
        output.fillShape(shape);
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        output.setPaint(color(context));
        output.drawShape(shape);
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return RGBColor.isVisible(color(context));
    }

    private @NotNull Color color(@NotNull RenderContext context) {
        return colorValue.get(context.measureContext());
    }
}
