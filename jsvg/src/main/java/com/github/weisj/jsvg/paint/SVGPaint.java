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
package com.github.weisj.jsvg.paint;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;

public interface SVGPaint {
    static @Nullable SVGPaint derive(@Nullable SVGPaint current, @Nullable SVGPaint other) {
        if (other == null) return current;
        if (current == null) return other;
        if (other instanceof AnimatedPaint) {
            return ((AnimatedPaint) other).derive(current);
        }
        if (other instanceof AnimatedColor) {
            return ((AnimatedColor) other).derive(current);
        }
        return other;
    }

    void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds);

    void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds);

    default boolean isVisible(@NotNull RenderContext context) {
        return this != PredefinedPaints.NONE;
    }
}
