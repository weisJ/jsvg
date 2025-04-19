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
package com.github.weisj.jsvg.nodes.prototype;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.transform.TransformBox;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.ShapeUtil;

public interface Transformable {

    default boolean shouldTransform() {
        return true;
    }

    @Nullable
    TransformValue transform();

    @NotNull
    Coordinate<LengthValue> transformOrigin();

    TransformBox transformBox();

    default @Nullable AffineTransform effectiveTransform(@NotNull RenderContext context,
            @NotNull ElementBounds bounds) {
        TransformValue transformValue = transform();
        if (transformValue == null) return null;

        Coordinate<LengthValue> origin = transformOrigin();
        double xOffset = 0;
        double yOffset = 0;
        MeasureContext measureContext = context.measureContext();

        switch (transformBox()) {
            case FillBox:
                Rectangle2D fillBox = bounds.fillBox();
                measureContext = measureContext.derive((float) fillBox.getWidth(), (float) fillBox.getHeight());
                xOffset = fillBox.getX();
                yOffset = fillBox.getY();
                break;
            case StrokeBox:
                Rectangle2D strokeBox = bounds.strokeBox();
                measureContext = measureContext.derive((float) strokeBox.getWidth(), (float) strokeBox.getHeight());
                xOffset = strokeBox.getX();
                yOffset = strokeBox.getY();
                break;
            case ViewBox:
                break;
        }

        double xOrigin = origin.x().resolve(measureContext) + xOffset;
        double yOrigin = origin.y().resolve(measureContext) + yOffset;
        AffineTransform conjugate = AffineTransform.getTranslateInstance(xOrigin, yOrigin);
        conjugate.concatenate(transformValue.get(measureContext));
        conjugate.translate(-xOrigin, -yOrigin);
        return conjugate;
    }

    default void applyTransform(@NotNull Output output, @NotNull RenderContext context,
            @NotNull ElementBounds bounds) {
        AffineTransform transform = effectiveTransform(context, bounds);
        if (transform == null) return;
        output.applyTransform(transform);
        context.userSpaceTransform().concatenate(transform);
    }

    default Shape transformShape(@NotNull Shape shape, @NotNull RenderContext renderContext,
            @NotNull ElementBounds elementBounds) {
        AffineTransform transform = effectiveTransform(renderContext, elementBounds);

        if (transform == null) return shape;
        return ShapeUtil.transformShape(shape, transform);
    }
}
