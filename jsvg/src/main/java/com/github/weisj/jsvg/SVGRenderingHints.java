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
package com.github.weisj.jsvg;

import java.awt.*;

import org.jetbrains.annotations.Nullable;

public final class SVGRenderingHints {
    private SVGRenderingHints() {}

    private static final int P_KEY_IMAGE_ANTIALIASING = 1;
    private static final int P_KEY_SOFT_CLIPPING = 2;

    public static final RenderingHints.Key KEY_IMAGE_ANTIALIASING = new Key(P_KEY_IMAGE_ANTIALIASING);
    public static final Object VALUE_IMAGE_ANTIALIASING_ON = Value.ON;
    public static final Object VALUE_IMAGE_ANTIALIASING_OFF = Value.OFF;

    public static final RenderingHints.Key KEY_SOFT_CLIPPING = new Key(P_KEY_SOFT_CLIPPING);
    public static final Object VALUE_SOFT_CLIPPING_ON = Value.ON;
    public static final Object VALUE_SOFT_CLIPPING_OFF = Value.OFF;

    private static final class Key extends RenderingHints.Key {
        /**
         * Construct a key using the indicated private key.  Each
         * subclass of Key maintains its own unique domain of integer
         * keys.  No two objects with the same integer key and of the
         * same specific subclass can be constructed.  An exception
         * will be thrown if an attempt is made to construct another
         * object of a given class with the same integer key as a
         * pre-existing instance of that subclass of Key.
         *
         * @param privateKey the specified key
         */
        private Key(int privateKey) {
            super(privateKey);
        }

        @Override
        public boolean isCompatibleValue(@Nullable Object val) {
            return val instanceof Value;
        }
    }

    private enum Value {
        ON,
        OFF
    }
}
