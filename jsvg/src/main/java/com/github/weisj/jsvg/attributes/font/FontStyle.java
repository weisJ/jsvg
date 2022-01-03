/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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

import com.github.weisj.jsvg.attributes.Radian;

/**
 * This abstract class shouldn't be extended besides those constants.
 */
abstract class FontStyle {

    private FontStyle() {}

    public @Nullable AffineTransform transform() {
        return null;
    }

    public static @NotNull FontStyle normal() {
        return Normal.INSTANCE;
    }

    public static @NotNull FontStyle italic() {
        return Italic.INSTANCE;
    }

    public static @NotNull FontStyle oblique() {
        return Oblique.DEFAULT;
    }

    public static @NotNull FontStyle oblique(@Radian float angle) {
        return new Oblique(angle);
    }

    static final class Normal extends FontStyle {
        private static final @NotNull FontStyle.Normal INSTANCE = new Normal();

        @Override
        public String toString() {
            return "Normal";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Normal;
        }

        @Override
        public int hashCode() {
            return Normal.class.hashCode();
        }
    }

    static final class Italic extends FontStyle {
        private static final @NotNull FontStyle.Italic INSTANCE = new Italic();

        @Override
        public String toString() {
            return "Italic";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Italic;
        }

        @Override
        public int hashCode() {
            return Italic.class.hashCode();
        }
    }

    static final class Oblique extends FontStyle {
        public static final @Radian float DEFAULT_ANGLE = (float) Math.toRadians(14);
        public static final @NotNull FontStyle.Oblique DEFAULT = new Oblique(DEFAULT_ANGLE);

        private final @Radian float angle;

        public Oblique(@Radian float angle) {
            this.angle = angle;
        }

        @Override
        public @NotNull AffineTransform transform() {
            return AffineTransform.getShearInstance(-angle, 0);
        }

        @Override
        public String toString() {
            return "Oblique{" + angle + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Oblique)) return false;
            Oblique that = (Oblique) o;
            return Float.compare(that.angle, angle) == 0;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(angle);
        }
    }
}
