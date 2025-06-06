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
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.renderer.MeasureContext;

public final class AnimatedFloatList implements FloatListValue {

    private final @NotNull Track track;

    private final @NotNull FloatListValue initial;
    private final float @NotNull [] @NotNull [] values;
    private float[] cache;

    private long timestamp = 0;
    private Track.InterpolationProgress progressCacheKey = null;

    public AnimatedFloatList(@NotNull Track track, @NotNull FloatListValue initial,
            float @NotNull [] @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull FloatListValue initial() {
        return initial;
    }

    public boolean isDirty(long timestamp) {
        return this.timestamp != timestamp;
    }

    @Override
    public float @NotNull [] get(@NotNull MeasureContext context) {
        long ts = context.timestamp();
        if (ts != this.timestamp) {
            this.timestamp = ts;
            Track.InterpolationProgress progress = track.interpolationProgress(ts, values.length);

            if (progress.equals(progressCacheKey)) {
                return cache;
            }
            progressCacheKey = progress;

            if (progress.isInitial()) {
                cache = initial.get(context);
                return cache;
            }

            int index = progress.iterationIndex();
            float[] start = values[index];
            float[] end = index == values.length - 1 ? null : values[index + 1];

            float fraction = progress.indexProgress();
            cache = track.floatListInterpolator().interpolate(initial.get(context), start, end, fraction, cache);
        }
        return cache;
    }
}
