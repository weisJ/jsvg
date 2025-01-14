/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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
package com.github.weisj.jsvg.animation.value;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class AnimatedLength implements LengthValue {
    private final @NotNull Track track;
    private final @NotNull LengthValue initial;
    private final @NotNull Length @NotNull [] values;

    public AnimatedLength(@NotNull Track track,
            @NotNull LengthValue initial, @NotNull Length @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull AnimatedLength derive(@NotNull LengthValue initialValue) {
        if (this.initial != Length.INHERITED) return this;
        return new AnimatedLength(track, initialValue, values);
    }

    public @NotNull LengthValue initial() {
        return initial;
    }

    @Override
    public float resolve(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);

        if (progress.isInitial()) return initial.resolve(context);
        int i = progress.iterationIndex();

        assert i >= 0;
        assert values.length > 0;
        if (i >= values.length - 1) {
            return values[values.length - 1].resolve(context);
        }

        float start = values[i].resolve(context);
        float end = values[i + 1].resolve(context);

        return track.floatInterpolator().interpolate(
                initial.resolve(context),
                start, end, progress.indexProgress());
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
    public boolean isConstantlyNonNegative() {
        for (Length value : values) {
            if (!value.isConstantlyNonNegative()) {
                return false;
            }
        }
        return initial.isConstantlyNonNegative();
    }
}
