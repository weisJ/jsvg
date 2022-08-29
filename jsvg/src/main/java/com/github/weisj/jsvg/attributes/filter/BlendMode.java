/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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
package com.github.weisj.jsvg.attributes.filter;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.HasMatchName;

public enum BlendMode implements HasMatchName {
    /**
     * The final color is the top color, regardless of what the bottom color is.
     * The effect is like two opaque pieces of paper overlapping.
     */
    Normal,
    /**
     * The final color is the result of multiplying the top and bottom colors.
     * A black layer leads to a black final layer, and a white layer leads to no change.
     * The effect is like two images printed on transparent film overlapping.
     */
    Multiply,
    /**
     * The final color is the result of inverting the colors, multiplying them, and inverting that value.
     * A black layer leads to no change, and a white layer leads to a white final layer.
     * The effect is like two images shone onto a projection screen.
     */
    Screen,
    /**
     * The final color is the result of multiply if the bottom color is darker,
     * or screen if the bottom color is lighter.
     * This blend mode is equivalent to hard-light but with the layers swapped.
     */
    Overlay,
    /**
     * The final color is composed of the darkest values of each color channel.
     */
    Darken,
    /**
     * The final color is composed of the lightest values of each color channel.
     */
    Lighten,
    /**
     * The final color is the result of dividing the bottom color by the inverse of the top color.
     * A black foreground leads to no change. A foreground with the inverse color of the backdrop
     * leads to a fully lit color. This blend mode is similar to screen, but the foreground need only be
     * as light as the inverse of the backdrop to create a fully lit color.
     */
    ColorDodge("color-dodge"),
    /**
     * The final color is the result of inverting the bottom color, dividing the value by the top color,
     * and inverting that value. A white foreground leads to no change.
     * A foreground with the inverse color of the backdrop leads to a black final image.
     * This blend mode is similar to multiply, but the foreground need only be as dark as the inverse of the backdrop
     * to make the final image black.
     */
    ColorBurn("color-burn"),
    /**
     * The final color is the result of multiply if the top color is darker, or screen if the top color is lighter.
     * This blend mode is equivalent to overlay but with the layers swapped.
     * The effect is similar to shining a harsh spotlight on the backdrop.
     */
    HardLight("hard-light"),
    /**
     * The final color is similar to hard-light, but softer. This blend mode behaves similar to hard-light.
     * The effect is similar to shining a diffused spotlight on the backdrop.
     */
    SoftLight("soft-light"),
    /**
     * The final color is the result of subtracting the darker of the two colors from the lighter one.
     * A black layer has no effect, while a white layer inverts the other layer's color.
     */
    Difference,
    /**
     * The final color is similar to difference, but with less contrast. As with difference, a black layer has no
     * effect, while a white layer inverts the other layer's color.
     */
    Exclusion,
    /**
     * The final color has the hue of the top color, while using the saturation and luminosity of the bottom color.
     */
    Hue,
    /**
     * The final color has the saturation of the top color, while using the hue and luminosity of the bottom color.
     * A pure gray backdrop, having no saturation, will have no effect.
     */
    Saturation,
    /**
     * The final color has the hue and saturation of the top color, while using the luminosity of the bottom color.
     * The effect preserves gray levels and can be used to colorize the foreground.
     */
    Color,
    /**
     * The final color has the luminosity of the top color, while using the hue and saturation of the bottom color.
     * This blend mode is equivalent to color, but with the layers swapped.
     */
    Luminosity;

    private final @NotNull String matchName;

    BlendMode(@NotNull String matchName) {
        this.matchName = matchName;
    }

    BlendMode() {
        this.matchName = name();
    }


    @Override
    public @NotNull String matchName() {
        return matchName;
    }
}
