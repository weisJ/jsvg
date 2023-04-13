/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
package com.github.weisj.jsvg.geometry.size;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

public enum Unit {
    PX,
    CM,
    MM,
    IN,
    EM,
    REM,
    EX,
    PT,
    PC,
    PERCENTAGE("%"),
    Raw("");

    private static final Unit[] units = values();

    public static Unit[] units() {
        return units;
    }

    private final @NotNull String suffix;

    Unit(@NotNull String suffix) {
        this.suffix = suffix;
    }

    Unit() {
        this.suffix = name().toLowerCase(Locale.ENGLISH);
    }

    @NotNull
    public Length valueOf(float value) {
        if (value == 0) return Length.ZERO;
        return new Length(this, value);
    }

    public @NotNull String suffix() {
        return suffix;
    }
}
