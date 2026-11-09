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

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;

/**
 * The build-time value of an element's inherited {@code color} property.
 *
 * <p>The locally specified value may be absent, in which case it is completed from the parent
 * render context. Paint properties whose computed value is {@code currentColor} hold this object
 * and are replaced with the completed color while deriving their {@code PaintContext}.</p>
 */
public final class ColorPropertyPaint implements SVGPaint {
    private final @Nullable SVGPaint specifiedValue;

    public ColorPropertyPaint(@Nullable SVGPaint specifiedValue) {
        this.specifiedValue = specifiedValue;
    }

    public @NotNull SVGPaint resolve(@Nullable SVGPaint inheritedValue) {
        SVGPaint inherited = inheritedValue != null ? inheritedValue : PredefinedPaints.DEFAULT_PAINT;
        SVGPaint resolved = SVGPaint.derive(inherited, specifiedValue);
        return resolved != null ? resolved : PredefinedPaints.DEFAULT_PAINT;
    }

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        throw unresolved();
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        throw unresolved();
    }

    private static @NotNull IllegalStateException unresolved() {
        return new IllegalStateException("The color property must be resolved before painting");
    }
}
