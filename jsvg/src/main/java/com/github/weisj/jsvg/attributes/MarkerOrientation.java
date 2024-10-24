/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.attributes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.Angle;

public abstract class MarkerOrientation {

    private MarkerOrientation() {}

    public enum MarkerType {
        START,
        MID,
        END
    }

    public static @NotNull MarkerOrientation parse(@Nullable String value, @NotNull AttributeParser parser) {
        if (value == null) return AngleOrientation.DEFAULT;
        if ("auto".equals(value)) return AutoOrientation.INSTANCE;
        if ("auto-start-reverse".equals(value)) return AutoStartReverseOrientation.INSTANCE;
        Angle angle = parser.parseAngle(value, Angle.UNSPECIFIED);
        if (angle.isSpecified()) return new AngleOrientation(angle);
        return AngleOrientation.DEFAULT;
    }

    public abstract float orientationFor(@NotNull MarkerType type, float dxIn, float dyIn, float dxOut,
            float dyOut);

    private static final class AutoOrientation extends MarkerOrientation {
        private static final @NotNull AutoOrientation INSTANCE = new AutoOrientation();

        @Override
        public float orientationFor(@NotNull MarkerType type, float dxIn, float dyIn, float dxOut,
                float dyOut) {
            switch (type) {
                case START:
                    return (float) Math.atan2(dyOut, dxOut);
                case END:
                    return (float) Math.atan2(dyIn, dxIn);
                case MID:
                    return (float) Math.atan2((dyIn + dyOut) / 2f, (dxIn + dxOut) / 2f);
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private static final class AutoStartReverseOrientation extends MarkerOrientation {
        private static final @NotNull AutoStartReverseOrientation INSTANCE = new AutoStartReverseOrientation();

        @Override
        public float orientationFor(@NotNull MarkerType type, float dxIn, float dyIn, float dxOut,
                float dyOut) {
            switch (type) {
                case START:
                    return (float) Math.atan2(-dyOut, -dxOut);
                case END:
                    return (float) Math.atan2(dyIn, dxIn);
                case MID:
                    return (float) Math.atan2((dyIn + dyOut) / 2f, (dxIn + dxOut) / 2f);
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private static final class AngleOrientation extends MarkerOrientation {
        private static final @NotNull AngleOrientation DEFAULT = new AngleOrientation(Angle.ZERO);
        private final @NotNull Angle angle;

        private AngleOrientation(@NotNull Angle angle) {
            this.angle = angle;
        }

        @Override
        public float orientationFor(@NotNull MarkerType type, float dxIn, float dyIn, float dxOut,
                float dyOut) {
            return angle.radians();
        }
    }
}
