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

import java.awt.geom.AffineTransform;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.ConstantLengthTransform;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class AnimatedTransform implements TransformValue {

    private final @NotNull Track track;
    private final @NotNull TransformValue initial;
    private final @NotNull TransformPart @NotNull [] values;

    private AffineTransform current;
    private long currentTimestamp = -1;

    public AnimatedTransform(@NotNull Track track, @NotNull TransformValue initial,
            @NotNull TransformPart @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull TransformValue initial() {
        return initial;
    }

    public @NotNull AnimatedTransform derive(@NotNull TransformValue initialValue) {
        if (this.initial != ConstantLengthTransform.INHERITED) return this;
        return new AnimatedTransform(track, initialValue, values);
    }

    private @NotNull AffineTransform current(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        if (currentTimestamp == timestamp) return current;
        currentTimestamp = timestamp;
        current = computeCurrent(context, timestamp);
        return current;
    }

    private @NotNull AffineTransform computeCurrent(@NotNull MeasureContext context, long timestamp) {
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);
        if (progress.isInitial()) return Objects.requireNonNull(initial).get(context);
        int i = progress.iterationIndex();

        assert i >= 0;
        if (i >= values.length - 1) {
            AffineTransform transform = new AffineTransform();
            values[values.length - 1].applyToTransform(transform, context);
            return transform;
        }

        TransformPart start = values[i];
        TransformPart end = values[i + 1];
        float t = progress.indexProgress();
        return track.transformInterpolator().interpolate(context, initial, start, end, t);
    }

    @Override
    public @NotNull AffineTransform get(@NotNull MeasureContext context) {
        return current(context);
    }
}
