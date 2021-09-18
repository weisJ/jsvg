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
package com.github.weisj.jsvg.attributes;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public enum UnitType {
    UserSpaceOnUse,
    @Default
    ObjectBoundingBox;

    public @NotNull MeasureContext deriveMeasure(@NotNull MeasureContext measure) {
        return deriveMeasure(measure, 1, 1);
    }

    public @NotNull MeasureContext deriveMeasure(@NotNull MeasureContext measure, double objectWidth,
            double objectHeight) {
        if (this == ObjectBoundingBox) {
            return measure.derive((float) objectWidth, (float) objectHeight);
        } else {
            return measure;
        }
    }

    public @NotNull AffineTransform viewTransform(@NotNull Rectangle2D bounds) {
        if (this == ObjectBoundingBox) {
            AffineTransform at = AffineTransform.getTranslateInstance(bounds.getX(), bounds.getY());
            at.scale(bounds.getWidth(), bounds.getHeight());
            return at;
        } else {
            return new AffineTransform();
        }
    }

    public @NotNull Rectangle2D.Double computeViewBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds,
            @NotNull Length x, @NotNull Length y, @NotNull Length width, @NotNull Length height) {
        MeasureContext patternMeasure = deriveMeasure(measure, bounds.getWidth(), bounds.getHeight());
        return new Rectangle2D.Double(
                x.resolveWidth(patternMeasure), y.resolveHeight(patternMeasure),
                width.resolveWidth(patternMeasure), height.resolveHeight(patternMeasure));
    }
}
