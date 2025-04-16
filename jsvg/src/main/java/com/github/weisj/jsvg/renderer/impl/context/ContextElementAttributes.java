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
package com.github.weisj.jsvg.renderer.impl.context;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.paint.SVGPaint;

/**
 * Holds values provided by the current context element.
 * Note that values here must always be absolute and can't refer themselves to any context element values.
 */
public final class ContextElementAttributes {
    public final @NotNull SVGPaint fillPaint;
    public final @NotNull SVGPaint strokePaint;

    public ContextElementAttributes(@NotNull SVGPaint fillPaint, @NotNull SVGPaint strokePaint) {
        this.fillPaint = fillPaint;
        this.strokePaint = strokePaint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextElementAttributes)) return false;
        ContextElementAttributes that = (ContextElementAttributes) o;
        return fillPaint.equals(that.fillPaint) && strokePaint.equals(that.strokePaint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fillPaint, strokePaint);
    }

    @Override
    public String toString() {
        return "ContextElementAttributes{" +
                "fillPaint=" + fillPaint +
                ", strokePaint=" + strokePaint +
                '}';
    }
}
