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
import com.github.weisj.jsvg.attributes.value.FloatValue;
import com.github.weisj.jsvg.renderer.MeasureContext;

public final class AnimatedFloat implements FloatValue {

    private final @NotNull Track track;
    private final @NotNull FloatValue initial;
    private final float @NotNull [] values;

    AnimatedFloat(@NotNull Track track, @NotNull FloatValue initial, float @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    @Override
    public float get(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);

        if (progress.isInitial()) return initial.get(context);
        int i = progress.iterationIndex();

        assert i >= 0;
        assert values.length > 0;
        if (i >= values.length - 1) {
            return values[i];
        }

        float start = values[i];
        float end = values[i + 1];

        return track.floatInterpolator().interpolate(initial.get(context), start, end, progress.indexProgress());
    }
}
