/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
import org.jetbrains.annotations.Nullable;


public enum AngleUnit {
    Deg,
    Grad,
    Rad,
    Turn,
    Raw("");

    private static final AngleUnit[] units = values();

    private static final double GRADIANS_TO_RADIANS = Math.PI * (2f / 400f);

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

    /** The unit for a CSS {@code <angle>} suffix (case-insensitive), or null if it isn't one. */
    public static @Nullable AngleUnit fromSuffix(@NotNull String suffix) {
        String lower = suffix.toLowerCase(Locale.ENGLISH);
        for (AngleUnit unit : units) {
            if (unit != Raw && unit.suffix.equals(lower)) return unit;
        }
        return null;
    }

    /** Converts a value in this unit to degrees. */
    public float toDegrees(float value) {
        if (this == Raw || this == Deg) return value; // avoid a lossy round trip through radians
        return (float) Math.toDegrees(toRadians(value));
    }

    public float toRadians(float value) {
        switch (this) {
            case Raw:
            case Deg:
                return (float) Math.toRadians(value);
            case Grad:
                return (float) (value * GRADIANS_TO_RADIANS);
            case Rad:
                return value;
            case Turn:
                return (float) (value * Math.PI * 2f);
            default:
                throw new IllegalArgumentException("Unknown angle unit " + this);
        }
    }
}
