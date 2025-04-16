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
package com.github.weisj.jsvg.renderer.animation;

import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

@Immutable
public final class AnimationState {
    public static final AnimationState NO_ANIMATION = new AnimationState(0, 0);
    private final long startTime;
    private final long currentTime;

    public AnimationState(long startTime, long currentTime) {
        this.startTime = startTime;
        this.currentTime = currentTime;
    }

    public long timestamp() {
        return currentTime - startTime;
    }

    @Override
    public String toString() {
        return "AnimationState{" +
                "startTime=" + startTime +
                ", currentTime=" + currentTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnimationState that = (AnimationState) o;
        return startTime == that.startTime && currentTime == that.currentTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, currentTime);
    }
}
