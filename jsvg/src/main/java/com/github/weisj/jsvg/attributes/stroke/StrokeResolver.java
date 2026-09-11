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
package com.github.weisj.jsvg.attributes.stroke;

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.impl.context.StrokeContext;

public final class StrokeResolver {
    private StrokeResolver() {}

    // https://svgwg.org/svg2-draft/painting.html#StrokeMiterlimitProperty
    public static @NotNull Stroke resolve(float pathLengthFactor, @NotNull MeasureContext measureContext,
            @NotNull StrokeContext context) {
        BasicStroke solidStroke = resolveUndashed(measureContext, context);
        Length[] dashPattern = context.dashPattern;
        LengthValue dashOffset = context.dashOffset;

        assert dashOffset != null;
        assert dashPattern != null;

        if (dashPattern.length == 0) return solidStroke;

        float[] dashes = new float[dashPattern.length];
        float offsetLength = 0;
        for (int i = 0; i < dashes.length; i++) {
            float dash = dashPattern[i].resolve(measureContext) * pathLengthFactor;
            offsetLength += dash;
            dashes[i] = dash;
        }

        float phase = dashOffset.resolve(measureContext) * pathLengthFactor;
        if (phase < 0) phase += offsetLength;

        return new BasicStroke(solidStroke.getLineWidth(), solidStroke.getEndCap(), solidStroke.getLineJoin(),
                solidStroke.getMiterLimit(), dashes, phase);
    }

    /** The stroke used for SVG stroke bounds, which ignore dash arrays, offsets and pathLength. */
    public static @NotNull BasicStroke resolveUndashed(@NotNull MeasureContext measureContext,
            @NotNull StrokeContext context) {
        assert context.strokeWidth != null;
        assert context.lineCap != null;
        assert context.lineJoin != null;
        assert Length.isSpecified(context.miterLimit);

        // In practice, any miter join will exceed a miter limit between 0 and 1.
        return new BasicStroke(context.strokeWidth.resolve(measureContext), context.lineCap.awtCode(),
                context.lineJoin.awtCode(), Math.max(1, context.miterLimit));
    }
}
