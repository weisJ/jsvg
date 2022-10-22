/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.MeasureContext;

public interface Transformable {

    default boolean shouldTransform() {
        return true;
    }

    @Nullable
    AffineTransform transform();

    @NotNull
    Point2D transformOrigin(@NotNull MeasureContext context);

    default void applyTransform(@NotNull Graphics2D g, @NotNull MeasureContext measureContext) {
        AffineTransform transform = transform();
        if (transform != null) {
            Point2D transformOrigin = transformOrigin(measureContext);
            g.translate(transformOrigin.getX(), transformOrigin.getY());
            g.transform(transform);
            g.translate(-transformOrigin.getX(), -transformOrigin.getY());
        }
    }

    default Shape transformShape(@NotNull Shape shape, @NotNull MeasureContext measureContext) {
        AffineTransform transform = transform();
        if (transform != null) {
            Point2D transformOrigin = transformOrigin(measureContext);
            AffineTransform at = new AffineTransform();
            at.translate(transformOrigin.getX(), transformOrigin.getY());
            at.concatenate(transform);
            at.translate(-transformOrigin.getX(), -transformOrigin.getY());
            return at.createTransformedShape(shape);
        }
        return shape;
    }
}
