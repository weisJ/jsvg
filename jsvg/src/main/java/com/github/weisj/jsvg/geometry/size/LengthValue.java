/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.geometry.size;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.ViewBox;

public interface LengthValue {

    boolean isUnspecified();

    default boolean isSpecified() {
        return !isUnspecified();
    }

    @NotNull
    LengthValue coerceNonNegative();

    @NotNull
    LengthValue orElseIfUnspecified(float value);

    boolean isConstantlyZero();

    /**
     * Used for resolving lengths which are used as x-coordinates or width values.
     * @param context the measuring context.
     * @return the resolved size.
     */
    float resolveWidth(@NotNull MeasureContext context);

    /**
     * Used for resolving lengths which are used as y-coordinates or height values.
     * @param context the measuring context.
     * @return the resolved size.
     */
    float resolveHeight(@NotNull MeasureContext context);

    /**
     * Used for resolving lengths which are neither used as y/x-coordinates nor width/height values.
     * Relative sizes are relative to the {@link ViewBox#normedDiagonalLength()}.
     * @param context the measuring context.
     * @return the resolved size.
     */
    float resolveLength(@NotNull MeasureContext context);

    default float resolveDimension(Dimension dimension, @NotNull MeasureContext context) {
        switch (dimension) {
            case WIDTH:
                return resolveWidth(context);
            case HEIGHT:
                return resolveHeight(context);
            case LENGTH:
                return resolveLength(context);
            default:
                throw new IllegalArgumentException("Unknown dimension: " + dimension);
        }
    }

    enum Dimension {
        WIDTH,
        HEIGHT,
        LENGTH
    }
}
