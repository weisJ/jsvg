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
package com.github.weisj.jsvg.geometry.size;

import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;

public final class Length implements LengthValue {
    public static final float UNSPECIFIED_RAW = Float.NaN;
    public static final @NotNull Length UNSPECIFIED = new Length(Unit.Raw, UNSPECIFIED_RAW);
    public static final @NotNull Length ZERO = new Length(Unit.Raw, 0);

    private @NotNull Unit unit;
    private float value;

    private static final float pixelsPerInch = 96f;
    private static final float inchesPerCm = .3936f;


    public Length(@NotNull Unit unit, float value) {
        this.unit = unit;
        this.value = value;
    }

    public Length(@NotNull Length ry) {
        this.unit = ry.unit;
        this.value = ry.value;
    }

    public static boolean isUnspecified(float value) {
        return Float.isNaN(value);
    }

    public static boolean isSpecified(float value) {
        return !isUnspecified(value);
    }

    private static float resolveNonPercentage(@NotNull MeasureContext context, Unit unit, float value) {
        if (isUnspecified(value)) throw new IllegalStateException("Can't resolve size of unspecified length");
        // If we are unspecified this will return the raw unspecified value, which we want.

        if (unit == Unit.Raw) return value;
        assert unit != Unit.PERCENTAGE;
        switch (unit) {
            case PX:
                return value;
            case IN:
                return pixelsPerInch * value;
            case CM:
                return inchesPerCm * pixelsPerInch * value;
            case MM:
                return 0.1f * inchesPerCm * pixelsPerInch * value;
            case PT:
                return (1f / 72f) * pixelsPerInch * value;
            case PC:
                return (1f / 6f) * pixelsPerInch * value;
            case EM:
                return context.em() * value;
            case REM:
                return context.rem() * value;
            case CH:
            case EX:
                return context.ex() * value;
            default:
                throw new UnsupportedOperationException("Not implemented: Can't convert " + unit + " to pixel");
        }
    }

    @Override
    public float resolveWidth(@NotNull MeasureContext context) {
        Unit u = unit();
        float raw = raw();
        if (u == Unit.PERCENTAGE) {
            return (raw * context.viewWidth()) / 100f;
        } else {
            return resolveNonPercentage(context, u, raw);
        }
    }

    @Override
    public float resolveHeight(@NotNull MeasureContext context) {
        Unit u = unit();
        float raw = raw();
        if (u == Unit.PERCENTAGE) {
            return (raw * context.viewHeight()) / 100f;
        } else {
            return resolveNonPercentage(context, u, raw);
        }
    }

    @Override
    public float resolveLength(@NotNull MeasureContext context) {
        Unit u = unit();
        float raw = raw();
        if (u == Unit.PERCENTAGE) {
            return (raw / 100f) * context.normedDiagonalLength();
        } else {
            return resolveNonPercentage(context, u, raw);
        }
    }

    /**
     * Used for resolving font sizes. Relative values will be resolves with respect to the current font size.
     * This isn't dependent on the current viewBox.
     * @param context the measuring context.
     * @return the resolved size.
     */
    public float resolveFontSize(@NotNull MeasureContext context) {
        Unit u = unit();
        float raw = raw();
        if (u == Unit.PERCENTAGE) {
            return (raw / 100f) * context.em();
        } else {
            return resolveNonPercentage(context, u, raw);
        }
    }

    @Override
    public @NotNull String toString() {
        if (isUnspecified()) return "<unspecified>";
        return value + unit.suffix();
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public boolean isConstantlyZero() {
        return isZero();
    }

    public float raw() {
        return value;
    }

    @ApiStatus.Internal
    public void setValue(float value, @NotNull Unit unit) {
        this.value = value;
        this.unit = unit;
    }

    public @NotNull Unit unit() {
        return unit;
    }

    @Override
    public boolean isUnspecified() {
        return isUnspecified(value);
    }

    @Override
    public @NotNull Length coerceNonNegative() {
        if (isSpecified() && value <= 0) return ZERO;
        return this;
    }

    public @NotNull Length coercePercentageToCorrectUnit(@NotNull UnitType unitType) {
        if (unitType == UnitType.UserSpaceOnUse) return this;
        if (unit() == Unit.PERCENTAGE) return this;
        return new Length(Unit.PERCENTAGE, raw() * 100);
    }

    @Override
    public @NotNull Length orElseIfUnspecified(float value) {
        if (isUnspecified()) return Unit.Raw.valueOf(value);
        return this;
    }

    public @NotNull Length multiply(float scalingFactor) {
        if (scalingFactor == 0) return ZERO;
        if (scalingFactor == 1) return this;
        return new Length(unit, scalingFactor * value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Length)) return false;
        Length length = (Length) o;
        return unit == length.unit && Float.compare(length.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, value);
    }
}
