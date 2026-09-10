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
    private final float defaultEm;
    private final float parentEm;
    private final @NotNull AnimationState animationState;

    public MeasureContext(float vw, float vh, float em, float ex, float rem, float defaultEm, float parentEm,
            @NotNull AnimationState animationState) {
        this.vw = vw;
        this.vh = vh;
        this.em = em;
        this.rem = rem;
        this.ex = ex;
        this.defaultEm = defaultEm;
        this.parentEm = parentEm;
        this.animationState = animationState;
    }

    public static @NotNull MeasureContext createInitial(@NotNull FloatSize viewBoxSize, float em, float ex,
            @NotNull AnimationState animationState) {
        // At the root the current font size is the user-agent default; there is no parent, so parentEm ==
        // em.
        return new MeasureContext(viewBoxSize.width, viewBoxSize.height, em, ex, em, em, em, animationState);
    }

    public @NotNull MeasureContext deriveRoot(float rem) {
        return new MeasureContext(vw, vh, em, ex, rem, defaultEm, parentEm, animationState);
    }

    public @NotNull MeasureContext derive(float viewWidth, float viewHeight) {
        return new MeasureContext(viewWidth, viewHeight, em, ex, rem, defaultEm, parentEm, animationState);
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
        // A specified em means we enter a new font scope: the previous em becomes the parent em for
        // relative font-size units. When the font size is unchanged inherit the existing parentEm.
        float newParentEm = Length.isUnspecified(em) ? this.parentEm : this.em;
        return new MeasureContext(newVw, newVh, effectiveEm, effectiveEx, rem, defaultEm, newParentEm, animationState);
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

    /** Current element font size; basis for relative length units for all attributes except font-size */
    public float em() {
        return em;
    }

    /** Root element font size; basis for rem units. It is the font-size of the <svg> root if specified, otherwise
     * falls back to the {@link #defaultEm} */
    public float rem() {
        return rem;
    }

    /** User-agent default font size; basis for absolute font-size keywords */
    public float defaultEm() {
        return defaultEm;
    }

    /** Parent element font size; basis for relative font-size units (em, %, ex) */
    public float parentEm() {
        return parentEm;
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
                ", defaultEm=" + defaultEm +
                ", parentEm=" + parentEm +
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
                && Float.compare(that.defaultEm, defaultEm) == 0
                && Float.compare(that.parentEm, parentEm) == 0
                && animationState.equals(that.animationState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vw, vh, em, ex, rem, defaultEm, parentEm, animationState);
    }

}
