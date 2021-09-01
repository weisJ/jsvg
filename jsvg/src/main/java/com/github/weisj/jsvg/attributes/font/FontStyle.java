/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
package com.github.weisj.jsvg.attributes.font;

import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Degrees;

/**
 * This interface shouldn't be implemented besides those constants.
 */
interface FontStyle {
    FontStyle Normal = new NormalStyle();
    FontStyle Italic = new ItalicStyle();
    FontStyle Oblique = new ObliqueStyle();

    default @Nullable AffineTransform transform() {
        return null;
    }

    final class NormalStyle implements FontStyle {
        @Override
        public String toString() {
            return "Normal";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NormalStyle;
        }

        @Override
        public int hashCode() {
            return NormalStyle.class.hashCode();
        }
    }

    final class ItalicStyle implements FontStyle {
        @Override
        public String toString() {
            return "Italic";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ItalicStyle;
        }

        @Override
        public int hashCode() {
            return ItalicStyle.class.hashCode();
        }
    }

    final class ObliqueStyle implements FontStyle {
        public static @Degrees final float DEFAULT_ANGLE = 14;
        private final @Degrees float angle;

        public ObliqueStyle() {
            this(DEFAULT_ANGLE);
        }

        public ObliqueStyle(@Degrees float angle) {
            this.angle = angle;
        }

        public @Degrees float angle() {
            return angle;
        }

        @Override
        public @NotNull AffineTransform transform() {
            return AffineTransform.getShearInstance(-Math.toRadians(angle()), 0);
        }

        @Override
        public String toString() {
            return "Oblique{" + angle() + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ObliqueStyle)) return false;
            ObliqueStyle that = (ObliqueStyle) o;
            return Float.compare(that.angle, angle) == 0;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(angle);
        }
    }
}
