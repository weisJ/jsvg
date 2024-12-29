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
package com.github.weisj.jsvg.attributes.transform;

import java.awt.geom.AffineTransform;
import java.util.Arrays;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.HasMatchName;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;

// TODO: This needs to use transform-box for transform origin measure resolution
public final class TransformPart {

    public enum TransformType implements HasMatchName {
        MATRIX,
        TRANSLATE,
        TRANSLATE_X("translateX"),
        TRANSLATE_Y("translateY"),
        SCALE,
        SCALE_X("scaleX"),
        SCALE_Y("scaleY"),
        ROTATE,
        SKEW,
        SKEW_X("skewX"),
        SKEW_Y("skewY");

        private final @NotNull String matchName;

        TransformType(@NotNull String matchName) {
            this.matchName = matchName;
        }

        TransformType() {
            this.matchName = name();
        }

        @Override
        public @NotNull String matchName() {
            return matchName;
        }

        TransformType interpolationType() {
            switch (this) {
                case TRANSLATE_X:
                case TRANSLATE_Y:
                    return TRANSLATE;
                case SCALE_X:
                case SCALE_Y:
                    return SCALE;
                case SKEW_X:
                case SKEW_Y:
                    return SKEW;
                default:
                    return this;
            }
        }
    }

    private static final TransformPart IDENTITY_MATRIX = new TransformPart(TransformType.MATRIX, new Length[] {
            Length.ONE, Length.ZERO, Length.ZERO,
            Length.ONE, Length.ZERO, Length.ZERO
    });
    private static final TransformPart IDENTITY_TRANSLATE =
            new TransformPart(TransformType.TRANSLATE, new Length[] {Length.ZERO, Length.ZERO});
    private static final TransformPart IDENTITY_SCALE =
            new TransformPart(TransformType.SCALE, new Length[] {Length.ONE, Length.ONE});

    private static final TransformPart IDENTITY_ROTATE =
            new TransformPart(TransformType.ROTATE, new Length[] {Length.ZERO, Length.ZERO, Length.ZERO});

    private static final TransformPart IDENTITY_SKEW =
            new TransformPart(TransformType.ROTATE, new Length[] {Length.ZERO, Length.ZERO});

    private final TransformType type;
    private final Length[] values;

    public TransformPart(TransformType type, @NotNull Length @NotNull [] values) {
        this.type = type;
        this.values = values;
    }

    public static @NotNull TransformPart identityOfType(@NotNull TransformType type) {
        switch (type) {
            case MATRIX:
                return IDENTITY_MATRIX;
            case TRANSLATE:
            case TRANSLATE_X:
            case TRANSLATE_Y:
                return IDENTITY_TRANSLATE;
            case SCALE:
            case SCALE_X:
            case SCALE_Y:
                return IDENTITY_SCALE;
            case ROTATE:
                return IDENTITY_ROTATE;
            case SKEW:
            case SKEW_X:
            case SKEW_Y:
                return IDENTITY_SKEW;
            default:
                throw new IllegalArgumentException("Unknown transform type: " + type);
        }
    }

    private static float getEntry(@NotNull TransformPart part, int index, float fallback,
            @NotNull MeasureContext context) {
        if (part.values.length > index) return part.values[index].resolve(context);
        return fallback;
    }

    public static @NotNull AffineTransform interpolate(@NotNull TransformPart a, @NotNull TransformPart b,
            @NotNull MeasureContext measureContext, float t) {
        TransformType aType = a.type.interpolationType();
        TransformType bType = b.type.interpolationType();
        if (aType != bType) {
            return GeometryUtil.interpolate(a.toTransform(measureContext), b.toTransform(measureContext), t);
        }
        switch (aType) {
            case MATRIX:
                return GeometryUtil.interpolate(a.toTransform(measureContext), b.toTransform(measureContext), t);
            case TRANSLATE:
            case TRANSLATE_X:
            case TRANSLATE_Y:
                return AffineTransform.getTranslateInstance(
                        GeometryUtil.lerp(t,
                                a.values[0].resolve(measureContext),
                                b.values[0].resolve(measureContext)),
                        GeometryUtil.lerp(t,
                                getEntry(a, 1, 0f, measureContext),
                                getEntry(b, 1, 0f, measureContext)));
            case SCALE:
            case SCALE_X:
            case SCALE_Y:
                float aScaleX = a.values[0].resolve(measureContext);
                float aScaleY = getEntry(a, 1, aScaleX, measureContext);
                float bScaleX = b.values[0].resolve(measureContext);
                float bScaleY = getEntry(a, 1, bScaleX, measureContext);
                return AffineTransform.getScaleInstance(
                        GeometryUtil.lerp(t, aScaleX, bScaleX),
                        GeometryUtil.lerp(t, aScaleY, bScaleY));
            case ROTATE:
                return AffineTransform.getRotateInstance(
                        Math.toRadians(GeometryUtil.lerp(t,
                                a.values[0].resolve(measureContext),
                                b.values[0].resolve(measureContext))),
                        GeometryUtil.lerp(t,
                                getEntry(a, 1, 0, measureContext),
                                getEntry(b, 1, 0, measureContext)),
                        GeometryUtil.lerp(t,
                                getEntry(a, 2, 0, measureContext),
                                getEntry(b, 2, 0, measureContext)));
            case SKEW:
            case SKEW_X:
            case SKEW_Y:
                return AffineTransform.getShearInstance(
                        Math.tan(Math.toRadians(
                                GeometryUtil.lerp(t,
                                        a.values[0].resolve(measureContext),
                                        b.values[0].resolve(measureContext)))),
                        Math.tan(Math.toRadians(
                                GeometryUtil.lerp(t,
                                        getEntry(a, 1, 0, measureContext),
                                        getEntry(b, 1, 0, measureContext)))));
            default:
                throw new IllegalStateException();
        }
    }

    public boolean canBeFlattened() {
        for (Length value : values) {
            if (!value.isAbsolute()) return false;
        }
        return true;
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull AffineTransform toTransform(@NotNull MeasureContext measureContext) {
        return applyToTransform(new AffineTransform(), measureContext);
    }

    @Contract(value = "_,_ -> param1", pure = true)
    public @NotNull AffineTransform applyToTransform(@NotNull AffineTransform transform,
            @NotNull MeasureContext measureContext) {
        return applyToTransform(transform, measureContext, 1);
    }

    @Contract(value = "_,_,_ -> param1", pure = true)
    public @NotNull AffineTransform applyToTransform(@NotNull AffineTransform transform,
            @NotNull MeasureContext measureContext,
            float progress) {
        switch (type) {
            case MATRIX:
                transform.concatenate(new AffineTransform(
                        values[0].resolve(measureContext),
                        values[1].resolve(measureContext),
                        values[2].resolve(measureContext),
                        values[3].resolve(measureContext),
                        values[4].resolve(measureContext),
                        values[5].resolve(measureContext)));
                break;
            case TRANSLATE:
                transform.translate(
                        progress * values[0].resolve(measureContext),
                        progress * values[1].resolve(measureContext));
                break;
            case TRANSLATE_X:
                transform.translate(values[0].resolve(measureContext), 0);
                break;
            case TRANSLATE_Y:
                transform.translate(0, values[0].resolve(measureContext));
                break;
            case SCALE:
                if (values.length == 1) {
                    float scale = values[0].resolve(measureContext);
                    transform.scale(scale, scale);
                } else {
                    transform.scale(values[0].resolve(measureContext), values[1].resolve(measureContext));
                }
                break;
            case SCALE_X:
                transform.scale(values[0].resolve(measureContext), 1);
                break;
            case SCALE_Y:
                transform.scale(1, values[0].resolve(measureContext));
                break;
            case ROTATE:
                if (values.length == 1) {
                    transform.rotate(Math.toRadians(values[0].resolve(measureContext)));
                } else {
                    transform.rotate(Math.toRadians(values[0].resolve(measureContext)),
                            values[1].resolve(measureContext),
                            values[2].resolve(measureContext));
                }
                break;
            case SKEW:
                if (values.length == 1) {
                    transform.shear(Math.tan(Math.toRadians(values[0].resolve(measureContext))), 0);
                } else {
                    transform.shear(Math.tan(Math.toRadians(values[0].resolve(measureContext))),
                            Math.tan(Math.toRadians(values[1].resolve(measureContext))));
                }
                break;
            case SKEW_X:
                transform.shear(Math.tan(values[0].resolve(measureContext)), 0);
                break;
            case SKEW_Y:
                transform.shear(0, Math.tan(values[0].resolve(measureContext)));
                break;
        }
        return transform;
    }

    @Override
    public String toString() {
        return "TransformPart{" +
                "type=" + type +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
