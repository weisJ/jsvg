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

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.time.Duration;
import com.github.weisj.jsvg.parser.AttributeNode;

public final class Track {
    private final @NotNull Duration duration;
    private final float repeatCount;
    private final Fill fill;

    private Track(@NotNull Duration duration, float repeatCount, Fill fill) {
        this.duration = duration;
        this.repeatCount = repeatCount;
        this.fill = fill;
    }

    public static @Nullable Track parse(@NotNull AttributeNode attributeNode) {
        Duration duration = attributeNode.getDuration("dur", Duration.INDEFINITE);
        String repeatCountStr = attributeNode.getValue("repeatCount");
        float repeatCount;
        if ("indefinite".equals(repeatCountStr)) {
            repeatCount = Integer.MAX_VALUE;
        } else {
            repeatCount = attributeNode.parser().parseFloat(repeatCountStr, 1);
        }

        if (duration.isIndefinite()
                || duration.milliseconds() < 0
                || repeatCount <= 0) {
            return null;
        }

        return new Track(duration, repeatCount, attributeNode.getEnum("fill", Fill.REMOVE));
    }

    private int iterationCount(long timestampMillis) {
        long durationMillis = duration.milliseconds();
        return (int) (timestampMillis / durationMillis);
    }

    private float iterationProgress(long timestampMillis) {
        long durationMillis = duration.milliseconds();
        return (float) (timestampMillis % durationMillis) / durationMillis;
    }

    public @NotNull InterpolationProgress interpolationProgress(long timestamp, int valueCount) {
        if (valueCount == 0) return InterpolationProgress.INITIAL;
        int iterationCount = iterationCount(timestamp);
        float iterationProgress = iterationProgress(timestamp);
        float totalIteration = iterationCount + iterationProgress;

        if (totalIteration > repeatCount) {
            if (fill == Fill.FREEZE) {
                return new InterpolationProgress(valueCount - 1, 0);
            } else {
                return InterpolationProgress.INITIAL;
            }
        }

        int i = (int) Math.floor(iterationProgress * (valueCount - 1));
        float t = (valueCount - 1) * iterationProgress - i;
        return new InterpolationProgress(i, t);
    }

    public static final class InterpolationProgress {
        public static final InterpolationProgress INITIAL = new InterpolationProgress(-1, 0);

        private final int iterationIndex;
        private final float indexProgress;

        public InterpolationProgress(int iterationIndex, float indexProgress) {
            this.iterationIndex = iterationIndex;
            this.indexProgress = indexProgress;
        }

        public boolean isInitial() {
            return iterationIndex == -1;
        }

        public int iterationIndex() {
            return iterationIndex;
        }

        public float indexProgress() {
            return indexProgress;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InterpolationProgress that = (InterpolationProgress) o;
            return iterationIndex == that.iterationIndex && Float.compare(indexProgress, that.indexProgress) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(iterationIndex, indexProgress);
        }
    }
}
