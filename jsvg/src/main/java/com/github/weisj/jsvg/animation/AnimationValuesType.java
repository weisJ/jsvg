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
package com.github.weisj.jsvg.animation;

import static com.github.weisj.jsvg.geometry.util.GeometryUtil.lerp;

import java.awt.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.interpolation.FloatInterpolator;
import com.github.weisj.jsvg.animation.interpolation.FloatListInterpolator;
import com.github.weisj.jsvg.animation.interpolation.PaintInterpolator;
import com.github.weisj.jsvg.attributes.paint.AwtSVGPaint;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.util.ColorUtil;

public enum AnimationValuesType implements FloatInterpolator, FloatListInterpolator, PaintInterpolator {
    VALUES,
    FROM_TO,
    FROM_BY,
    BY,
    TO;

    @Override
    public float interpolate(float initial, float from, float to, float progress) {
        float a = from;
        float b = to;
        if (this == TO || this == BY) {
            a = initial;
        }
        if (this == BY || this == FROM_BY) {
            b = a + b;
        }
        return a + (b - a) * progress;
    }

    @Override
    public float[] interpolate(float @NotNull [] initial, float @NotNull [] from, float @Nullable [] to,
            float progress, float @Nullable [] cache) {
        float[] a = from;
        float t = progress;
        if (this == TO || this == BY) {
            a = initial;
        }

        float[] result = cache;
        if (result == null || result.length != a.length) {
            result = new float[a.length];
        }

        if (this == BY || this == FROM_BY) {
            if (to == null) {
                System.arraycopy(a, 0, result, 0, a.length);
            } else {
                for (int i = 0; i < a.length; i++) {
                    // Repeat b if it is shorter than a
                    result[i] = a[i] + to[i % to.length];
                }
            }
        } else {
            if (to != null && a.length != to.length) {
                // Use discrete animation
                t = 0;
            }

            if (to == null || GeometryUtil.approximatelyEqual(t, 0)) {
                System.arraycopy(a, 0, result, 0, a.length);
            } else if (GeometryUtil.approximatelyEqual(t, 1)) {
                System.arraycopy(to, 0, result, 0, to.length);
            } else {
                for (int i = 0; i < a.length; i++) {
                    result[i] = lerp(t, a[i], to[i]);
                }
            }
        }

        return result;
    }

    private static @Nullable Color extractColor(@NotNull SVGPaint p) {
        if (!(p instanceof SimplePaintSVGPaint)) return null;
        Paint paint = ((SimplePaintSVGPaint) p).paint();
        return paint instanceof Color ? (Color) paint : null;
    }

    @Override
    public @NotNull SVGPaint interpolate(@NotNull SVGPaint initial, @NotNull SVGPaint from, @NotNull SVGPaint to,
            float progress) {
        SVGPaint a = from;
        if (this == TO || this == BY) {
            a = initial;
        }

        Color colorA = extractColor(a);
        Color colorB = extractColor(to);

        if (colorA == null || colorB == null) {
            // Discrete animation
            if (this == BY) {
                // By with non-additive value is invalid
                return a;
            }
            return GeometryUtil.approximatelyEqual(progress, 1) ? to : a;
        }

        if (this == BY || this == FROM_BY) {
            // FIXME: This clamps the color values to 0-255 however only the final color should be clamped
            // This is important if multiple animations affect the value. Also the delta of by can have out of
            // bounds values.
            colorB = ColorUtil.add(colorA, colorB);
        }

        return new AwtSVGPaint(ColorUtil.interpolate(progress, colorA, colorB));
    }
}
