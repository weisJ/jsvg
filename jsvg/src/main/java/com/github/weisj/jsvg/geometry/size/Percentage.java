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
package com.github.weisj.jsvg.geometry.size;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.google.errorprone.annotations.Immutable;

@Immutable
public final class Percentage implements Comparable<Percentage>, PercentageValue {
    public static final float UNSPECIFIED_RAW = Float.NaN;
    public static final @NotNull Percentage UNSPECIFIED = new Percentage(UNSPECIFIED_RAW);
    public static final @NotNull Percentage ZERO = new Percentage(0);
    public static final @NotNull Percentage ONE = new Percentage(1);
    public static final @NotNull Percentage INHERITED = new Percentage(1);

    private final float value;

    public Percentage(float value) {
        this.value = value;
    }

    public static boolean isUnspecified(float value) {
        return Float.isNaN(value);
    }

    public static boolean isSpecified(float value) {
        return !isUnspecified(value);
    }

    public float value() {
        return value;
    }

    @Override
    public float get(@NotNull MeasureContext context) {
        return value;
    }

    @Override
    public @NotNull PercentageValue multiply(@NotNull PercentageValue other) {
        if (this.value == 1) return other;
        if (other instanceof Percentage) {
            float otherValue = ((Percentage) other).value;
            if (otherValue == 0 || this.value == 0) return ZERO;
            if (otherValue == 1) return this;
            return new Percentage(this.value * otherValue);
        }
        return other.multiply(this);
    }

    public boolean isUnspecified() {
        return isUnspecified(value);
    }

    public boolean isSpecified() {
        return !isUnspecified();
    }

    public @NotNull Percentage orElseIfUnspecified(float value) {
        if (isUnspecified()) return new Percentage(value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Percentage percentage = (Percentage) o;
        return Float.compare(value, percentage.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return (value * 100) + "%";
    }

    @Override
    public int compareTo(@NotNull Percentage o) {
        return Float.compare(value, o.value);
    }
}
