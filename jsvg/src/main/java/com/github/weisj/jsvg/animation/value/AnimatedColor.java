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
package com.github.weisj.jsvg.animation.value;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;

public final class AnimatedColor implements SVGPaint, ColorValue {

    private final @NotNull Track track;
    private final @NotNull ColorValue initial;
    private final @NotNull RGBColor @NotNull [] values;

    private Color current;
    private long currentTimestamp = -1;

    public AnimatedColor(@NotNull Track track, @NotNull ColorValue initial, @NotNull RGBColor @NotNull [] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull AnimatedColor derive(@NotNull SVGPaint value) {
        if (this.initial != RGBColor.INHERITED) return this;
        ColorValue c = null;
        if (value instanceof ColorValue) {
            c = (ColorValue) value;
        } else if (value instanceof SimplePaintSVGPaint) {
            Paint p = ((SimplePaintSVGPaint) value).paint();
            if (p instanceof Color) {
                c = new RGBColor((Color) p);
            } else if (p instanceof RGBColor) {
                c = (RGBColor) p;
            }
        } else {
            c = RGBColor.DEFAULT;
        }
        if (c == null) return this;
        return new AnimatedColor(track, c, values);
    }

    @Override
    public @NotNull Color get(@NotNull MeasureContext context) {
        return current(context);
    }

    private @NotNull Color current(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        if (currentTimestamp == timestamp) return current;
        currentTimestamp = timestamp;
        current = computeCurrent(context, timestamp);
        return current;
    }

    private @NotNull Color computeCurrent(@NotNull MeasureContext context, long timestamp) {
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);
        if (progress.isInitial()) return Objects.requireNonNull(initial).get(context);
        int i = progress.iterationIndex();

        assert i >= 0;
        assert values.length > 0;
        if (i >= values.length - 1) {
            return values[values.length - 1].toColor();
        }

        RGBColor start = values[i];
        RGBColor end = values[i + 1];
        float t = progress.indexProgress();
        return RGBColor.interpolate(t, start, end).toColor();
    }

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        output.setPaint(current(context.measureContext()));
        output.fillShape(shape);
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        output.setPaint(current(context.measureContext()));
        output.drawShape(shape);
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return RGBColor.isVisible(current(context.measureContext()));
    }
}
