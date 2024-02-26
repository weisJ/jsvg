/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.attributes.time;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.SuffixUnit;


public enum TimeUnit implements SuffixUnit<TimeUnit, Duration> {
    Hour(60 * 60 * 1000, "h"),
    Minute(60 * 1000, "min"),
    Second(1000, "s"),
    Millisecond(1, "ms"),
    Raw(1000, ""); // Same as seconds

    private static final TimeUnit[] units = values();

    @Override
    public @NotNull TimeUnit @NotNull [] units() {
        return units;
    }

    private final @NotNull String suffix;
    private final long milliseconds;

    TimeUnit(long milliseconds, @NotNull String suffix) {
        this.suffix = suffix;
        this.milliseconds = milliseconds;
    }

    @Override
    public @NotNull Duration valueOf(float value) {
        if (value == 0) return new Duration(0);
        return new Duration((long) (this.milliseconds * value));
    }

    @Override
    public @NotNull String suffix() {
        return suffix;
    }
}
