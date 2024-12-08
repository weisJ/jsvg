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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;

public final class Length implements LengthValue {
    public static final float UNSPECIFIED_RAW = Float.NaN;
    public static final @NotNull Length UNSPECIFIED = new Length(Unit.RAW, UNSPECIFIED_RAW);
    public static final @NotNull Length ZERO = new Length(Unit.RAW, 0);
    public static final @NotNull Length INHERITED = new Length(Unit.RAW, 0);

    private final @NotNull Unit unit;
    private final float value;

    private static final float PIXELS_PER_INCH = 96f;
    private static final float INCHES_PER_CM = .3936f;


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
        if (unit == Unit.RAW) {
            // If we are unspecified this will return UNSPECIFIED_RAW as intended.
            return value;
        }
        assert !unit.isPercentage();
        switch (unit) {
            case PX:
                return value;
            case IN:
                return PIXELS_PER_INCH * value;
            case CM:
                return INCHES_PER_CM * PIXELS_PER_INCH * value;
            case MM:
                return 0.1f * INCHES_PER_CM * PIXELS_PER_INCH * value;
            case Q:
                return 0.025f * INCHES_PER_CM * PIXELS_PER_INCH * value;
            case PT:
                return (1f / 72f) * PIXELS_PER_INCH * value;
            case PC:
                return (1f / 6f) * PIXELS_PER_INCH * value;
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
    public float resolve(@NotNull MeasureContext context) {
        float raw = raw();
        switch (unit) {
            case PERCENTAGE_LENGTH:
                return (raw * context.normedDiagonalLength()) / 100f;
            case VW:
            case VI:
            case PERCENTAGE_WIDTH:
                return (raw * context.viewWidth()) / 100f;
            case VH:
            case VB:
            case PERCENTAGE_HEIGHT:
                return (raw * context.viewHeight()) / 100f;
            case PERCENTAGE:
                return raw / 100f;
            case V_MIN:
                return (raw * Math.min(context.viewWidth(), context.viewHeight())) / 100f;
            case V_MAX:
                return (raw * Math.max(context.viewWidth(), context.viewHeight())) / 100f;
            default:
                return resolveNonPercentage(context, unit, raw);
        }
    }

    /**
     * Used for resolving font sizes. Relative values will be resolves with respect to the current font size.
     * This isn't dependent on the current viewBox.
     * @param context the measuring context.
     * @return the resolved size.
     */
    public float resolveFontSize(@NotNull MeasureContext context) {
        float raw = raw();
        switch (unit) {
            case PERCENTAGE_LENGTH:
            case PERCENTAGE_WIDTH:
            case PERCENTAGE_HEIGHT:
                throw new IllegalStateException("Can't resolve font size with geometric percentage unit");
            case PERCENTAGE:
                return (raw / 100f) * context.em();
            default:
                return resolveNonPercentage(context, unit, raw);
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

    @Override
    public boolean isConstantlyNonNegative() {
        return raw() >= 0;
    }

    public boolean isAbsolute() {
        switch (unit) {
            case RAW:
            case PX:
            case IN:
            case CM:
            case MM:
            case Q:
            case PT:
            case PC:
                return true;
            default:
                return false;
        }
    }

    public float raw() {
        return value;
    }

    public @NotNull Unit unit() {
        return unit;
    }

    public boolean isUnspecified() {
        return isUnspecified(value);
    }

    public boolean isSpecified() {
        return isSpecified(value);
    }

    public @NotNull Length coerceNonNegative() {
        if (isSpecified() && value <= 0) return ZERO;
        return this;
    }

    public @NotNull Length coercePercentageToCorrectUnit(@NotNull UnitType unitType,
            @NotNull PercentageDimension dimension) {
        if (unitType == UnitType.UserSpaceOnUse) return this;
        if (unit.isPercentage()) return this;
        Unit u;
        switch (dimension) {
            case WIDTH:
                u = Unit.PERCENTAGE_WIDTH;
                break;
            case HEIGHT:
                u = Unit.PERCENTAGE_HEIGHT;
                break;
            case LENGTH:
                u = Unit.PERCENTAGE_LENGTH;
                break;
            case CUSTOM:
                u = Unit.PERCENTAGE;
                break;
            default:
                throw new IllegalStateException("Can't coerce to percentage with no dimension");
        }
        return new Length(u, raw() * 100);
    }

    public @NotNull Length orElseIfUnspecified(float value) {
        if (isUnspecified()) return Unit.RAW.valueOf(value);
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
