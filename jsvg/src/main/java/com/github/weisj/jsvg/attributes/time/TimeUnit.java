/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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

public enum TimeUnit {
    Milliseconds("ms", 1),
    Second("s", 1000),
    Minute("min", 1000 * 60),
    Hour("h", 1000 * 60),
    Raw("", 1);

    static TimeUnit[] units = values();

    public static TimeUnit[] units() {
        return units;
    }

    private final @NotNull String suffix;
    private final long conversionFactor;

    TimeUnit(@NotNull String suffix, long conversionFactor) {
        this.suffix = suffix;
        this.conversionFactor = conversionFactor;
    }

    @NotNull
    public TimeValue valueOf(long value) {
        if (value == 0) return TimeValue.ZERO;
        return new TimeValue(value * conversionFactor);
    }

    public @NotNull String suffix() {
        return suffix;
    }
}
