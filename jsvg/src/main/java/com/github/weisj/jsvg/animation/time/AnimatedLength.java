package com.github.weisj.jsvg.animation.time;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import org.jetbrains.annotations.NotNull;

public class AnimatedLength extends Length {

    private Length[] values;
    public AnimatedLength(@NotNull Unit unit, float value) {
        super(unit, value);
    }

    @Override
    public @NotNull Unit unit(@NotNull MeasureContext context) {
        return super.unit(context);
    }

    @Override
    public float raw(@NotNull MeasureContext context) {
        return super.raw(context);
    }

    @Override
    public boolean isUnspecified() {
        return false;
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public @NotNull Length coerceNonNegative() {
        return super.coerceNonNegative();
    }

    @Override
    public @NotNull Length multiply(float scalingFactor) {
        throw new UnsupportedOperationException("Multiplying animated lengths is currently not supported");
    }
}
