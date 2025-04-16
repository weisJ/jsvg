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
package com.github.weisj.jsvg.animation.interpolation;

import static com.github.weisj.jsvg.geometry.util.GeometryUtil.lerp;

import java.awt.*;
import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.Additive;
import com.github.weisj.jsvg.animation.AnimationValuesType;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.AwtSVGPaint;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.paint.impl.SimplePaintSVGPaint;
import com.github.weisj.jsvg.renderer.MeasureContext;

public final class DefaultInterpolator
        implements FloatInterpolator, FloatListInterpolator, PaintInterpolator, TransformInterpolator {
    private final Additive additive;
    private final AnimationValuesType valuesType;

    public DefaultInterpolator(AnimationValuesType valuesType, Additive additive) {
        this.valuesType = valuesType;
        this.additive = additive;
    }

    @Override
    public float interpolate(float initial, float a, float b, float progress) {
        switch (valuesType) {
            case FROM_BY: {
                return a + b * progress;
            }
            case BY: {
                return initial + b * progress;
            }
            case TO: {
                return initial + (b - initial) * progress;
            }
            case FROM_TO:
            case VALUES:
            default: {
                float result = a + (b - a) * progress;

                if (additive == Additive.SUM) {
                    result += initial;
                }
                return result;
            }
        }
    }

    private static float @NotNull [] ensureCacheCapacity(float @Nullable [] cache, int length) {
        if (cache == null || cache.length != length) {
            return new float[length];
        }
        return cache;
    }

    private static float @NotNull [] arrayLerp(float @Nullable [] cache, float @NotNull [] from, float @Nullable [] to,
            float progress) {
        float t = progress;

        boolean isEndTime = GeometryUtil.approximatelyEqual(t, 1);
        if (to == null || (from.length != to.length && !isEndTime)) {
            // Use discrete animation
            t = 0;
        }

        float[] result = cache;
        if (to == null || GeometryUtil.approximatelyEqual(t, 0)) {
            result = ensureCacheCapacity(result, from.length);
            System.arraycopy(from, 0, result, 0, from.length);
        } else if (isEndTime) {
            result = ensureCacheCapacity(result, to.length);
            System.arraycopy(to, 0, result, 0, to.length);
        } else {
            result = ensureCacheCapacity(result, from.length);
            for (int i = 0; i < from.length; i++) {
                result[i] = lerp(t, from[i], to[i]);
            }
        }
        return result;
    }

    /**
     * This method is used to calculate the result of the following operation: result_i = a * x_i + b_i
     * <ul>
     *  <li> If b_i is shorter than a_i, b_i is repeated.
     *  <li> If b_i is longer than a_i, the extra values are ignored.
     * </ul>
     */
    private static float @NotNull [] saxpy(float @Nullable [] cache, float @NotNull [] b, float @Nullable [] x,
            float a) {
        float[] result = ensureCacheCapacity(cache, b.length);
        System.arraycopy(b, 0, result, 0, b.length);

        if (x == null) return b;

        int n = Math.min(result.length, x.length);
        for (int i = 0; i < n; i++) {
            result[i] += a * x[i];
        }
        for (int i = n; i < result.length; i++) {
            result[i] += a * x[i % n];
        }
        return result;
    }

    @Override
    public float @NotNull [] interpolate(float @NotNull [] initial, float @NotNull [] a, float @Nullable [] b,
            float progress, float @Nullable [] cache) {
        switch (valuesType) {
            case FROM_BY: {
                return saxpy(cache, a, b, progress);
            }
            case BY: {
                return saxpy(cache, initial, b, progress);
            }
            case TO: {
                return arrayLerp(cache, initial, b, progress);
            }
            case FROM_TO:
            case VALUES:
            default: {
                float[] result = arrayLerp(cache, a, b, progress);

                if (additive == Additive.SUM) {
                    result = saxpy(result, result, initial, 1);
                }
                return result;
            }
        }
    }

    private static @Nullable RGBColor extractColor(@NotNull SVGPaint p) {
        if (!(p instanceof SimplePaintSVGPaint)) return null;
        Paint paint = ((SimplePaintSVGPaint) p).paint();
        if (paint instanceof Color) return new RGBColor((Color) paint);
        if (paint instanceof RGBColor) return (RGBColor) paint;
        return null;
    }

    @Override
    public @NotNull SVGPaint interpolate(@NotNull SVGPaint initial, @NotNull SVGPaint a, @NotNull SVGPaint b,
            float progress) {
        RGBColor colorA = extractColor(a);
        RGBColor colorB = extractColor(b);

        if (colorA == null || colorB == null) {
            return discreteAnimation(initial, a, b, progress);
        }

        switch (valuesType) {
            case FROM_BY: {
                return new AwtSVGPaint(RGBColor.saxpy(progress, colorA, colorB));
            }
            case BY: {
                RGBColor initialColor = extractColor(initial);
                if (initialColor == null) return initial;
                return new AwtSVGPaint(RGBColor.saxpy(progress, initialColor, colorB));
            }
            case TO: {
                RGBColor initialColor = extractColor(initial);
                if (initialColor == null) return initial;
                return new AwtSVGPaint(RGBColor.interpolate(progress, initialColor, colorB));
            }
            case FROM_TO:
            case VALUES:
            default: {
                RGBColor result = RGBColor.interpolate(progress, colorA, colorB);

                if (additive == Additive.SUM) {
                    RGBColor initialColor = extractColor(initial);
                    if (initialColor == null) return initial;
                    result = RGBColor.add(initialColor, result);
                }
                return new AwtSVGPaint(result);
            }
        }
    }

    private SVGPaint discreteAnimation(@NotNull SVGPaint initial, @NotNull SVGPaint a, @NotNull SVGPaint b,
            float progress) {
        if (additive != Additive.REPLACE) {
            return initial;
        }
        return GeometryUtil.approximatelyEqual(progress, 1) ? b : a;
    }

    @Override
    public @NotNull AffineTransform interpolate(@NotNull MeasureContext context, @NotNull TransformValue initial,
            @NotNull TransformPart a, @NotNull TransformPart b, float progress) {
        switch (valuesType) {
            case FROM_BY: {
                return b.applyToTransform(a.applyToTransform(new AffineTransform(), context), context, progress);
            }
            case BY: {
                return b.applyToTransform(initial.get(context), context, progress);
            }
            case TO: {
                // NOTE: This is undefined by the specification
                return GeometryUtil.interpolate(initial.get(context), b.toTransform(context), progress);
            }
            case FROM_TO:
            case VALUES:
            default: {
                AffineTransform result = TransformPart.interpolate(a, b, context, progress);

                if (additive == Additive.SUM) {
                    result.preConcatenate(initial.get(context));
                }
                return result;
            }
        }
    }
}
