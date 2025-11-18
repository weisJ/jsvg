/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer;

import java.awt.*;

import org.jetbrains.annotations.Nullable;

public final class SVGRenderingHints {
    private SVGRenderingHints() {}

    private static final int P_KEY_IMAGE_ANTIALIASING = 1;
    private static final int P_KEY_SOFT_CLIPPING = 2;
    private static final int P_KEY_CACHE_OFFSCREEN_IMAGE = 3;
    private static final int P_KEY_MASK_CLIP_RENDERING = 4;

    public static final RenderingHints.Key KEY_IMAGE_ANTIALIASING = new Key(P_KEY_IMAGE_ANTIALIASING);
    public static final Object VALUE_IMAGE_ANTIALIASING_ON = Value.ON;
    public static final Object VALUE_IMAGE_ANTIALIASING_OFF = Value.OFF;

    public static final RenderingHints.Key KEY_SOFT_CLIPPING = new Key(P_KEY_SOFT_CLIPPING);
    public static final Object VALUE_SOFT_CLIPPING_ON = Value.ON;
    public static final Object VALUE_SOFT_CLIPPING_OFF = Value.OFF;

    public static final RenderingHints.Key KEY_MASK_CLIP_RENDERING = new Key(P_KEY_MASK_CLIP_RENDERING);
    public static final Object VALUE_MASK_CLIP_RENDERING_FAST = Value.ON;
    public static final Object VALUE_MASK_CLIP_RENDERING_ACCURACY = Value.OFF;
    public static final Object VALUE_MASK_CLIP_RENDERING_DEFAULT = VALUE_MASK_CLIP_RENDERING_FAST;

    public static final RenderingHints.Key KEY_CACHE_OFFSCREEN_IMAGE = new Key(P_KEY_CACHE_OFFSCREEN_IMAGE);
    public static final Object VALUE_USE_CACHE = Value.ON;
    public static final Object VALUE_NO_CACHE = Value.OFF;

    private static final class Key extends RenderingHints.Key {
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
