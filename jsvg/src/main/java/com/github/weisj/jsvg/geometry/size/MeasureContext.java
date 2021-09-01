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

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ViewBox;
import com.google.errorprone.annotations.Immutable;

@Immutable
public class MeasureContext {
    private final float vw;
    private final float vh;
    private final float em;
    private final float ex;

    public MeasureContext(float vw, float vh, float em, float ex) {
        this.vw = vw;
        this.vh = vh;
        this.em = em;
        this.ex = ex;
    }

    public @NotNull MeasureContext derive(@Nullable ViewBox viewBox, float em, float ex) {
        if (viewBox == null && Length.isUnspecified(em) && Length.isUnspecified(ex)) return this;
        float newVw = vw;
        float newVh = vh;
        if (viewBox != null) {
            // If any width or height are unspecified keep the width/height of the parent viewPort.
            if (!viewBox.hasUnspecifiedWidth()) newVw = viewBox.width;
            if (!viewBox.hasUnspecifiedHeight()) newVh = viewBox.height;
        }
        float effectiveEm = Length.isUnspecified(em) ? this.em : em;
        float effectiveEx = Length.isUnspecified(ex) ? this.ex : ex;
        return new MeasureContext(newVw, newVh, effectiveEm, effectiveEx);
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

    public float ex() {
        return ex;
    }

    @Override
    public String toString() {
        return "MeasureContext{" +
                "vw=" + vw +
                ", vh=" + vh +
                ", em=" + em +
                ", ex=" + ex +
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
                && Float.compare(that.ex, ex) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vw, vh, em, ex);
    }
}
