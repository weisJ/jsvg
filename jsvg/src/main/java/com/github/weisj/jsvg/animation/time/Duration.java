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
package com.github.weisj.jsvg.animation.time;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

@Immutable
public final class Duration implements Comparable<Duration> {
    public static final long INDEFINITE_RAW = Long.MAX_VALUE;
    public static final @NotNull Duration INDEFINITE = new Duration(INDEFINITE_RAW);
    public static final Duration ZERO = new Duration(0);

    private final long milliseconds;

    public Duration(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public long milliseconds() {
        return milliseconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Duration duration = (Duration) o;
        return milliseconds == duration.milliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(milliseconds);
    }

    public boolean isIndefinite() {
        return milliseconds == INDEFINITE_RAW;
    }

    public @NotNull Duration plus(@NotNull Duration duration) {
        if (duration == INDEFINITE || this == INDEFINITE) return INDEFINITE;
        return new Duration(milliseconds + duration.milliseconds);
    }

    public @NotNull Duration minus(@NotNull Duration duration) {
        if (duration == INDEFINITE || this == INDEFINITE) return INDEFINITE;
        return new Duration(milliseconds - duration.milliseconds);
    }

    @Override
    public int compareTo(@NotNull Duration o) {
        return Long.compare(milliseconds, o.milliseconds);
    }
}
