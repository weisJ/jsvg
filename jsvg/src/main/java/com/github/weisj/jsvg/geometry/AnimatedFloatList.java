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
package com.github.weisj.jsvg.geometry;

import static com.github.weisj.jsvg.geometry.util.GeometryUtil.lerp;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.Value;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;

public final class AnimatedFloatList implements Value<float[]> {

    private final @NotNull Track track;

    private final float[] initial;
    private final float[][] values;
    private float[] cache;

    private long timestamp = 0;
    private int progressIndex = -1;
    private float progressFraction = -1;

    public AnimatedFloatList(@NotNull Track track, float @NotNull [] initial, float @NotNull [] @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public float @NotNull [] initial() {
        return initial;
    }

    public boolean isDirty(long timestamp) {
        return this.timestamp != timestamp;
    }

    @Override
    public float @NotNull [] get(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        if (timestamp != this.timestamp) {
            this.timestamp = timestamp;
            float progress = track.interpolationProgress(timestamp, values.length);

            if (progress < 0) {
                cache = initial;
                return cache;
            }

            int index = (int) progress;
            float[] start = values[index];
            float[] end = values[index + 1];

            float fraction;
            if (start.length != end.length) {
                // Use discrete animation
                fraction = 0;
            } else {
                fraction = progress - index;
            }

            if (fraction == progressFraction && index == progressIndex) {
                return cache;
            }

            progressIndex = index;
            progressFraction = fraction;

            if (cache == null || cache.length != start.length) {
                cache = new float[start.length];
            }

            if (GeometryUtil.approximatelyEqual(fraction, 0)) {
                System.arraycopy(start, 0, cache, 0, start.length);
            } else if (GeometryUtil.approximatelyEqual(fraction, 1)) {
                System.arraycopy(end, 0, cache, 0, end.length);
            } else {
                for (int i = 0; i < start.length; i++) {
                    cache[i] = lerp(fraction, start[i], end[i]);
                }
            }
        }
        return cache;
    }
}
