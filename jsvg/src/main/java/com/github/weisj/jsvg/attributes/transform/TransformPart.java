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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.HasMatchName;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class TransformPart {

    public TransformPart(TransformType type, Length[] values) {
        this.type = type;
        this.values = values;
    }

    public boolean canBeFlattened() {
        for (Length value : values) {
            if (!value.isAbsolute()) return false;
        }
        return true;
    }

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
    }

    private final TransformType type;
    private final Length[] values;

    public void applyToTransform(@NotNull AffineTransform transform, @NotNull MeasureContext measureContext) {
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
                        values[0].resolve(measureContext),
                        values[1].resolve(measureContext));
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
    }
}
