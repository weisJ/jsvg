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
package com.github.weisj.jsvg.geometry.size;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.Radian;

public enum AngleUnit {
    Deg,
    Grad,
    Rad,
    Turn,
    Raw("");

    static AngleUnit[] units = values();

    private static final double GRADIANS_TO_RADIANS = Math.PI * (2f / 400f);
    private static final double TURNS_TO_RADIANS = Math.PI * (2f / 400f);

    public static AngleUnit[] units() {
        return units;
    }

    private final @NotNull String suffix;

    AngleUnit(@NotNull String suffix) {
        this.suffix = suffix;
    }

    AngleUnit() {
        this.suffix = name().toLowerCase(Locale.ENGLISH);
    }

    public @NotNull String suffix() {
        return suffix;
    }

    public @Radian float toRadians(float value, @NotNull AngleUnit rawReplacement) {
        if (rawReplacement == Raw) throw new IllegalArgumentException("Cant replace raw unit with raw");
        switch (this) {
            case Deg:
                return (float) Math.toRadians(value);
            case Grad:
                return (float) (value * GRADIANS_TO_RADIANS);
            case Rad:
                return value;
            case Turn:
                return (float) (value * Math.PI * 2f);
            case Raw:
                return rawReplacement.toRadians(value, rawReplacement);
            default:
                throw new IllegalArgumentException("Unknown angle unit " + this);
        }
    }
}
