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
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Percentage;

public final class AnimatedPercentage implements PercentageValue {

    private final @NotNull Track track;
    private final @NotNull PercentageValue initial;
    private final float @NotNull [] values;
    private final PercentageValue multiplier;

    public AnimatedPercentage(@NotNull Track track, @NotNull PercentageValue initial, float @NotNull [] values,
            PercentageValue multiplier) {
        this.track = track;
        this.initial = initial;
        this.values = values;
        this.multiplier = multiplier;
    }

    public @NotNull AnimatedPercentage derive(@NotNull PercentageValue initial) {
        if (this.initial != Percentage.INHERITED) return this;
        return new AnimatedPercentage(track, initial, values, multiplier);
    }

    @Override
    public @NotNull PercentageValue multiply(@NotNull PercentageValue other) {
        return new AnimatedPercentage(track, initial, values, multiplier.multiply(other));
    }

    public @NotNull PercentageValue initial() {
        return initial;
    }

    private float getBase(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);

        if (progress.isInitial()) return initial.get(context);
        int i = progress.iterationIndex();

        assert i >= 0;
        if (i >= values.length - 1) {
            return values[values.length - 1];
        }

        float start = values[i];
        float end = values[i + 1];

        return track.floatInterpolator().interpolate(initial.get(context), start, end, progress.indexProgress());
    }

    @Override
    public float get(@NotNull MeasureContext context) {
        return getBase(context) * multiplier.get(context);
    }

}
