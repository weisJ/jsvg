/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.impl.bridge;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.effect.BlendMode;

public class FXBlendModeBridge {

    static final Logger LOGGER = Logger.getLogger(FXBlendModeBridge.class.getName());

    public static BlendMode toBlendMode(com.github.weisj.jsvg.attributes.filter.BlendMode jsvgBlendMode) {
        switch (jsvgBlendMode) {
            case Normal:
                return BlendMode.SRC_OVER;
            case Multiply:
                return BlendMode.MULTIPLY;
            case Screen:
                return BlendMode.SCREEN;
            case Overlay:
                return BlendMode.OVERLAY;
            case Darken:
                return BlendMode.DARKEN;
            case Lighten:
                return BlendMode.LIGHTEN;
            case ColorDodge:
                return BlendMode.COLOR_DODGE;
            case ColorBurn:
                return BlendMode.COLOR_BURN;
            case HardLight:
                return BlendMode.HARD_LIGHT;
            case SoftLight:
                return BlendMode.SOFT_LIGHT;
            case Difference:
                return BlendMode.DIFFERENCE;
            case Exclusion:
                return BlendMode.EXCLUSION;
            case Hue:
            case Saturation:
            case Color:
            case Luminosity:
                LOGGER.log(Level.WARNING, "Unsupported BlendMode, JavaFX doesn't support: " + jsvgBlendMode);
                return BlendMode.SRC_OVER;
            default:
                return BlendMode.SRC_OVER;
        }
    }


}
