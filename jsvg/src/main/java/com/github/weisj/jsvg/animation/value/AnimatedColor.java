/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;

public class AnimatedColor implements SVGPaint {

    private final Track track;
    private final @Nullable Color initial;
    private final Color[] values;

    private Color current;
    private long currentTimestamp = -1;

    public AnimatedColor(@NotNull Track track, @Nullable Color initial, @NotNull Color[] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull AnimatedColor derive(@NotNull SVGPaint value) {
        if (this.initial != null) return this;
        Color c = null;
        if (value instanceof SimplePaintSVGPaint) {
            Paint p = ((SimplePaintSVGPaint) value).paint();
            if (p instanceof Color) {
                c = (Color) p;
            }
        } else if (value instanceof AnimatedColor) {
            c = ((AnimatedColor) value).initial;
        }
        if (c == null) return this;
        return new AnimatedColor(track, c, values);
    }

    private @NotNull Color current(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        if (currentTimestamp == timestamp) return current;
        currentTimestamp = timestamp;
        current = computeCurrent(timestamp);
        return current;
    }

    private @NotNull Color computeCurrent(long timestamp) {
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);
        if (progress.isInitial()) return Objects.requireNonNull(initial);
        int i = progress.iterationIndex();

        if (i == values.length - 1) {
            return values[i];
        }

        Color start = values[i];
        Color end = values[i + 1];
        float t = progress.indexProgress();
        return ColorUtil.interpolate(t, start, end);
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
        return current(context.measureContext()).getAlpha() > 0;
    }
}
