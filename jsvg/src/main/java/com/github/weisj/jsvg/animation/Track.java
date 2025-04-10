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
package com.github.weisj.jsvg.animation;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.interpolation.*;
import com.github.weisj.jsvg.animation.time.Duration;
import com.github.weisj.jsvg.animation.time.Interval;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.SeparatorMode;

public final class Track {
    private final @NotNull List<@NotNull Interval> intervals;
    private final float repeatCount;
    private final Fill fill;
    private final DefaultInterpolator interpolator;

    private Track(@NotNull List<@NotNull Interval> intervals, float repeatCount, Fill fill,
            AnimationValuesType valuesType, Additive additive) {
        this.intervals = intervals;
        this.repeatCount = repeatCount;
        this.fill = fill;
        this.interpolator = new DefaultInterpolator(valuesType, additive);
    }

    public static @Nullable Track parse(@NotNull AttributeNode attributeNode,
            @NotNull AnimationValuesType valuesType, @NotNull Additive additive) {
        List<Duration> begins = parseBegin(attributeNode);
        Duration duration = attributeNode.getDuration("dur", Duration.INDEFINITE);

        List<Interval> intervals = begins.stream()
                .map(b -> new Interval(b, b.plus(duration)))
                .filter(Interval::isValid)
                .sorted(Comparator.comparing(Interval::begin))
                .collect(Collectors.toList());

        String repeatCountStr = attributeNode.getValue("repeatCount");
        float repeatCount;
        if ("indefinite".equals(repeatCountStr)) {
            repeatCount = Float.POSITIVE_INFINITY;
        } else {
            repeatCount = attributeNode.parser().parseFloat(repeatCountStr, 1);
        }
        if (intervals.isEmpty() || repeatCount <= 0) {
            return null;
        }

        return new Track(intervals, repeatCount, attributeNode.getEnum("fill", Fill.REMOVE),
                valuesType, additive);
    }

    @NotNull
    private static List<Duration> parseBegin(@NotNull AttributeNode attributeNode) {
        String[] beginsRaw = attributeNode.getStringList("begin", SeparatorMode.SEMICOLON_ONLY);
        List<Duration> begins;

        if (beginsRaw.length > 0) {
            begins = new ArrayList<>(beginsRaw.length);
            for (String s : beginsRaw) {
                Duration b = attributeNode.parser().parseDuration(s, null);
                if (b != null) begins.add(b);
            }
        } else {
            begins = Collections.singletonList(Duration.ZERO);
        }
        return begins;
    }

    public @NotNull List<@NotNull Interval> intervals() {
        return intervals;
    }

    public float repeatCount() {
        return repeatCount;
    }

    public @NotNull Fill fill() {
        return fill;
    }

    private int iterationCount(@NotNull Duration duration, long timestampMillis) {
        long durationMillis = duration.milliseconds();
        return (int) (timestampMillis / durationMillis);
    }

    private float iterationProgress(@NotNull Duration duration, long timestampMillis) {
        long durationMillis = duration.milliseconds();
        return (float) (timestampMillis % durationMillis) / durationMillis;
    }

    private @Nullable Interval currentInterval(long timestamp) {
        ListIterator<@NotNull Interval> iterator = intervals.listIterator(intervals.size() - 1);
        while (iterator.hasPrevious()) {
            Interval interval = iterator.previous();
            if (interval.end().milliseconds() <= timestamp) {
                return interval;
            }
        }
        return null;
    }

    public @NotNull InterpolationProgress interpolationProgress(long timestamp, int valueCount) {
        if (valueCount == 0) return InterpolationProgress.INITIAL;
        Interval currentInterval = currentInterval(timestamp);
        if (currentInterval == null) return InterpolationProgress.INITIAL;

        long time = timestamp - currentInterval.begin().milliseconds();
        Duration duration = currentInterval.duration();
        int iterationCount = iterationCount(duration, time);
        float iterationProgress = iterationProgress(duration, time);
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

    public @NotNull FloatInterpolator floatInterpolator() {
        return interpolator;
    }

    public @NotNull FloatListInterpolator floatListInterpolator() {
        return interpolator;
    }

    public @NotNull PaintInterpolator paintInterpolator() {
        return interpolator;
    }

    public @NotNull TransformInterpolator transformInterpolator() {
        return interpolator;
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
