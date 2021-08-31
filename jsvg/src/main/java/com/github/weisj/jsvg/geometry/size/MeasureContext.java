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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ViewBox;

public class MeasureContext {
    private final @NotNull ViewBox viewBox;
    private final float em;
    private final float ex;

    public MeasureContext(@NotNull ViewBox viewBox, float em, float ex) {
        this.viewBox = viewBox;
        this.em = em;
        this.ex = ex;
    }

    public @NotNull MeasureContext derive(@Nullable ViewBox viewBox, float em, float ex) {
        if (viewBox == null && Length.isUnspecified(em) && Length.isUnspecified(ex)) return this;
        ViewBox vb = viewBox;
        if (vb != null) {
            // If any width or height are unspecified use the width/height of the parent viewPort.
            boolean unspecifiedWidth = vb.hasUnspecifiedWidth();
            boolean unspecifiedHeight = vb.hasUnspecifiedHeight();
            if (unspecifiedWidth || unspecifiedHeight) {
                vb = new ViewBox(vb.x, vb.y,
                        unspecifiedWidth ? this.viewBox.width : vb.width,
                        unspecifiedHeight ? this.viewBox.height : vb.height);
            }
        } else {
            vb = this.viewBox;
        }
        float effectiveEm = Length.isUnspecified(em) ? this.em : em;
        float effectiveEx = Length.isUnspecified(ex) ? this.ex : ex;
        return new MeasureContext(vb, effectiveEm, effectiveEx);
    }

    public @NotNull ViewBox viewBox() {
        return viewBox;
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
                "viewBox=" + viewBox +
                ", em=" + em +
                ", ex=" + ex +
                '}';
    }
}
