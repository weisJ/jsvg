/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import com.google.errorprone.annotations.Immutable;

@Immutable
public final class MeasureContext {
    private final float vw;
    private final float vh;
    private final float em;
    private final float rem;
    private final float ex;
    private final @NotNull AnimationState animationState;

    public MeasureContext(float vw, float vh, float em, float ex, float rem, @NotNull AnimationState animationState) {
        this.vw = vw;
        this.vh = vh;
        this.em = em;
        this.rem = rem;
        this.ex = ex;
        this.animationState = animationState;
    }

    public static @NotNull MeasureContext createInitial(@NotNull FloatSize viewBoxSize, float em, float ex,
            @NotNull AnimationState animationState) {
        return new MeasureContext(viewBoxSize.width, viewBoxSize.height, em, ex, em, animationState);
    }

    public @NotNull MeasureContext deriveRoot(float rem) {
        return new MeasureContext(vw, vh, em, ex, rem, animationState);
    }

    public @NotNull MeasureContext derive(float viewWidth, float viewHeight) {
        return new MeasureContext(viewWidth, viewHeight, em, ex, rem, animationState);
    }

    public @NotNull MeasureContext derive(@Nullable ViewBox viewBox, float em, float ex) {
        if (viewBox == null && Length.isUnspecified(em) && Length.isUnspecified(ex)) return this;
        float newVw = vw;
        float newVh = vh;
        if (viewBox != null) {
            // If any width or height are unspecified keep the width/height of the parent viewPort.
            if (viewBox.hasSpecifiedWidth()) newVw = viewBox.width;
            if (viewBox.hasSpecifiedHeight()) newVh = viewBox.height;
        }
        float effectiveEm = Length.isUnspecified(em) ? this.em : em;
        float effectiveEx = Length.isUnspecified(ex) ? this.ex : ex;
        return new MeasureContext(newVw, newVh, effectiveEm, effectiveEx, rem, animationState);
    }

    public float viewWidth() {
        return vw;
    }

    public float viewHeight() {
        return vh;
    }

    public float normedDiagonalLength() {
        return (float) Math.sqrt((vw * vw + vh * vh) / 2);
    }

    public float em() {
        return em;
    }

    public float rem() {
        return rem;
    }

    public float ex() {
        return ex;
    }

    public long timestamp() {
        return animationState.timestamp();
    }

    @Override
    public String toString() {
        return "MeasureContext{" +
                "vw=" + vw +
                ", vh=" + vh +
                ", em=" + em +
                ", rem=" + rem +
                ", ex=" + ex +
                ", animationState=" + animationState +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeasureContext)) return false;
        MeasureContext that = (MeasureContext) o;
        return Float.compare(that.vw, vw) == 0
                && Float.compare(that.vh, vh) == 0
                && Float.compare(that.em, em) == 0
                && Float.compare(that.rem, rem) == 0
                && Float.compare(that.ex, ex) == 0
                && animationState.equals(that.animationState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vw, vh, em, ex, rem, animationState);
    }

}
