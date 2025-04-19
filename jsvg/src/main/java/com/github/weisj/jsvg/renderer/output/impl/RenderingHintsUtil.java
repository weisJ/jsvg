/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.output.impl;

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.SVGRenderingHints;

public final class RenderingHintsUtil {

    private RenderingHintsUtil() {}

    public static void setupSVGRenderingHints(@NotNull Graphics2D g) {
        Object aaHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (aaHint != RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
            setSVGRenderingHint(g,
                    SVGRenderingHints.KEY_IMAGE_ANTIALIASING,
                    aaHint == RenderingHints.VALUE_ANTIALIAS_ON
                            ? SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON
                            : SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if (g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL) == RenderingHints.VALUE_STROKE_DEFAULT) {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        setSVGRenderingHint(g,
                SVGRenderingHints.KEY_MASK_CLIP_RENDERING,
                SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_DEFAULT);
    }

    private static void setSVGRenderingHint(@NotNull Graphics2D g, @NotNull RenderingHints.Key key, @NotNull Object o) {
        if (g.getRenderingHint(key) == null) {
            g.setRenderingHint(key, o);
        }
    }
}
