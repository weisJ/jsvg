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
package com.github.weisj.jsvg.animation.value;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.annotations.Sealed;
import com.github.weisj.jsvg.attributes.value.FloatValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

@Sealed(permits = {AnimatedPercentage.class})
public class AnimatedFloat implements FloatValue {

    protected final @NotNull Track track;
    protected final float initial;
    protected final float[] values;

    AnimatedFloat(@NotNull Track track, float initial, float[] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public float get(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);

        if (progress.isInitial()) return initial;
        int i = progress.iterationIndex();

        if (i == values.length - 1) {
            return values[i];
        }

        float start = values[i];
        float end = values[i + 1];

        return start + (end - start) * progress.indexProgress();
    }
}
