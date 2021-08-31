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

import java.awt.*;
import java.awt.geom.AffineTransform;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Degrees;

public interface FontStyle {
    FontStyle Normal = new NormalStyle();
    FontStyle Italic = new ItalicStyle();
    FontStyle Oblique = new ObliqueStyle();

    @MagicConstant(intValues = {Font.PLAIN, Font.ITALIC})
    int awtCode();

    default @Nullable AffineTransform transform() {
        return null;
    }

    class NormalStyle implements FontStyle {
        @Override
        public int awtCode() {
            return Font.PLAIN;
        }
    }

    class ItalicStyle implements FontStyle {
        @Override
        public int awtCode() {
            return Font.ITALIC;
        }
    }

    class ObliqueStyle implements FontStyle {
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
        public @Nullable AffineTransform transform() {
            return AffineTransform.getRotateInstance(Math.toRadians(angle()));
        }

        @Override
        public int awtCode() {
            return Font.PLAIN;
        }
    }
}
