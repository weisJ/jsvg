/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.animation.time;

import org.jetbrains.annotations.NotNull;

public final class Interval {
    private final @NotNull Duration begin;
    private final @NotNull Duration end;

    public Interval(@NotNull Duration begin, @NotNull Duration end) {
        this.begin = begin;
        this.end = end;
    }

    public @NotNull Duration begin() {
        return begin;
    }

    public @NotNull Duration end() {
        return begin;
    }

    public @NotNull Duration duration() {
        return end.minus(begin);
    }

    @Override
    public String toString() {
        return "Interval{" +
                "begin=" + begin +
                ", end=" + end +
                '}';
    }

    public boolean isValid() {
        return !begin.isIndefinite() && !end.isIndefinite() && begin.milliseconds() < end.milliseconds();
    }
}
