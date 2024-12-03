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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;

public class AnimatedPaint implements SVGPaint {

    private final Track track;
    private final SVGPaint initial;
    private final SVGPaint[] values;

    private SVGPaint current;
    private long currentTimestamp = -1;

    public AnimatedPaint(Track track, SVGPaint initial, SVGPaint[] values) {
        this.track = track;
        this.initial = initial;
        this.values = values;
    }

    public @NotNull AnimatedPaint derive(@NotNull SVGPaint value) {
        if (this.initial != SVGPaint.INHERITED) return this;
        return new AnimatedPaint(track, value, values);
    }

    private @NotNull SVGPaint current(@NotNull MeasureContext context) {
        long timestamp = context.timestamp();
        if (currentTimestamp == timestamp) return current;
        currentTimestamp = timestamp;
        current = computeCurrent(timestamp);
        return current;
    }

    private @NotNull SVGPaint computeCurrent(long timestamp) {
        Track.InterpolationProgress progress = track.interpolationProgress(timestamp, values.length);
        if (progress.isInitial()) return initial;
        int i = progress.iterationIndex();

        assert i >= 0;
        if (i >= values.length - 1) {
            return values[values.length - 1];
        }

        SVGPaint start = values[i];
        SVGPaint end = values[i + 1];

        return track.paintInterpolator().interpolate(initial, start, end, progress.indexProgress());
    }

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        current(context.measureContext()).fillShape(output, context, shape, bounds);
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        current(context.measureContext()).drawShape(output, context, shape, bounds);
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return current(context.measureContext()).isVisible(context);
    }
}
