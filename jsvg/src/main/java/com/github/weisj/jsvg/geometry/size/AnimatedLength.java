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

import com.github.weisj.jsvg.attributes.time.TimeValue;

public class AnimatedLength extends Length {

    private final Length from;
    private final Length to;
    private final TimeValue begin;
    private final TimeValue end;

    public AnimatedLength(@NotNull Length from, @NotNull Length to, @NotNull TimeValue begin, @NotNull TimeValue end) {
        super(to.unit(), to.raw());
        this.from = from;
        this.to = to;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public float resolveWidth(@NotNull MeasureContext context) {
        return interpolate(context.time(), from.resolveWidth(context), to.resolveWidth(context));
    }

    @Override
    public float resolveHeight(@NotNull MeasureContext context) {
        return interpolate(context.time(), from.resolveHeight(context), to.resolveHeight(context));
    }

    @Override
    public float resolveLength(@NotNull MeasureContext context) {
        return interpolate(context.time(), from.resolveLength(context), to.resolveLength(context));
    }

    @Override
    public float resolveFontSize(@NotNull MeasureContext context) {
        return interpolate(context.time(), from.resolveFontSize(context), to.resolveFontSize(context));
    }

    private float interpolate(long timeInMillis, float from, float to) {
        if (timeInMillis <= begin.milliseconds()) return from;
        if (timeInMillis >= end.milliseconds()) return to;
        return from + (to - from) * timeScaleFactor(timeInMillis);
    }

    private float timeScaleFactor(long ms) {
        return Math.abs((ms - begin.milliseconds()) / (float) (end.milliseconds() - begin.milliseconds()));
    }
}
