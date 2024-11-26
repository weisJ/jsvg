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
package com.github.weisj.jsvg.attributes.font;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.HasMatchName;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;

public enum PredefinedFontSize implements HasMatchName, FontSize {
    xxSmall("xx-small", 3f / 5f),
    xSmall("x-small", 3f / 4f),
    small(8f / 9f),
    medium(1f),
    large(6f / 5f),
    xLarge("x-large", 3f / 2f),
    xxLarge("xx-large", 2f),
    xxxLarge("xxx-large", 3f),
    larger(1.3f),
    smaller(0.7f),
    Number(0);

    private final @NotNull String matchName;
    private final float scalingFactor;

    PredefinedFontSize(@NotNull String matchName, float scalingFactor) {
        this.matchName = matchName;
        this.scalingFactor = scalingFactor;
    }

    PredefinedFontSize(float scalingFactor) {
        this.scalingFactor = scalingFactor;
        this.matchName = name();
    }

    @Override
    public @NotNull String matchName() {
        return matchName;
    }

    @Override
    public @NotNull Length size(@NotNull Length parentSize) {
        if (this == Number) throw new UnsupportedOperationException("Number font-size needs to parsed explicitly");
        if (this == smaller || this == larger) return parentSize.multiply(scalingFactor);
        return Unit.RAW.valueOf(SVGFont.defaultFontSize() * scalingFactor);
    }
}
