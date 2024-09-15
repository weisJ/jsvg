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
package com.github.weisj.jsvg.animation.size;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.LengthValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public class AnimatedLength implements LengthValue {
    private final @NotNull Track track;
    private final @NotNull Length initial;
    private final @NotNull Length @NotNull [] values;

    public AnimatedLength(@NotNull Track track,
            @NotNull Length initial, @NotNull Length @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull Length initial() {
        return initial;
    }

    @Override
    public float resolveDimension(Dimension dimension, @NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        float progress = track.interpolationProgress(timestamp, values.length);

        if (progress < 0) return initial.resolveDimension(dimension, context);
        int i = (int) progress;

        float start = values[i].resolveDimension(dimension, context);
        float end = values[i + 1].resolveDimension(dimension, context);
        float t = progress - i;

        return start + (end - start) * t;
    }

    @Override
    public boolean isUnspecified() {
        return initial.isUnspecified();
    }

    @Override
    public @NotNull LengthValue coerceNonNegative() {
        Length[] newValues = new Length[values.length];
        for (int i = 0; i < newValues.length; i++) {
            newValues[i] = values[i].coerceNonNegative();
        }
        return new AnimatedLength(track, initial.coerceNonNegative(), newValues);
    }

    @Override
    public @NotNull LengthValue orElseIfUnspecified(float value) {
        return new AnimatedLength(track, initial.orElseIfUnspecified(value), values);
    }

    @Override
    public boolean isConstantlyZero() {
        for (Length value : values) {
            if (!value.isConstantlyZero()) {
                return false;
            }
        }
        return initial.isConstantlyZero();
    }

    @Override
    public float resolveWidth(@NotNull MeasureContext context) {
        return resolveDimension(Dimension.WIDTH, context);
    }

    @Override
    public float resolveHeight(@NotNull MeasureContext context) {
        return resolveDimension(Dimension.HEIGHT, context);
    }

    @Override
    public float resolveLength(@NotNull MeasureContext context) {
        return resolveDimension(Dimension.LENGTH, context);
    }
}
