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
import org.jetbrains.annotations.Nullable;

public final class AnimationPeriod {
    private final long start;
    private final long end;
    private final boolean freeze;

    public AnimationPeriod(long start, long end, boolean freeze) {
        this.start = start;
        this.end = end;
        this.freeze = freeze;
    }

    public @NotNull AnimationPeriod derive(@Nullable Track track) {
        if (track == null) return this;
        long begin = track.begin().milliseconds();
        long duration = track.duration().milliseconds();
        float repeatCount = track.repeatCount();

        long animationStartTime = Math.min(this.start, begin);
        long animationEndTime;
        if (Float.isFinite(repeatCount)) {
            animationEndTime = Math.max(this.end, (long) (begin + duration * repeatCount));
        } else {
            animationEndTime = Long.MAX_VALUE;
        }

        boolean animationFreezes = this.freeze || track.fill() == Fill.FREEZE;
        return new AnimationPeriod(animationStartTime, animationEndTime, animationFreezes);
    }

    public long duration() {
        return end - start;
    }

    public long startTime() {
        return start;
    }

    public long endTime() {
        return end;
    }
}
