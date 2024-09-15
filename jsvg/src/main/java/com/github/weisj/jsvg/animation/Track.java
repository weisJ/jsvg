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
package com.github.weisj.jsvg.animation;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.time.Duration;

public final class Track {
    private final @NotNull Duration duration;
    private final float repeatCount;

    public Track(@NotNull Duration duration, float repeatCount) {
        this.duration = duration;
        this.repeatCount = repeatCount;
    }

    private int iterationCount(long timestampMillis) {
        long durationMillis = duration.milliseconds();
        return (int) (timestampMillis / durationMillis);
    }

    private float iterationProgress(long timestampMillis) {
        long durationMillis = duration.milliseconds();
        return (float) (timestampMillis % durationMillis) / durationMillis;
    }

    public float interpolationProgress(long timestamp, int valueCount) {
        if (valueCount == 0) return -1;
        int iterationCount = iterationCount(timestamp);
        float iterationProgress = iterationProgress(timestamp);
        float totalIteration = iterationCount + iterationProgress;

        if (totalIteration > repeatCount) return -1;

        int i = (int) Math.floor(iterationProgress * (valueCount - 1));
        float t = (valueCount - 1) * iterationProgress - i;
        return i + t;
    }
}
