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
import com.github.weisj.jsvg.geometry.size.Percentage;

public enum FontStretch implements HasMatchName {
    Normal(1f),
    UltraCondensed(0.5f, "ultra-condensed"),
    ExtraCondensed(0.625f, "extra-condensed"),
    Condensed(0.75f, "condensed"),
    SemiCondensed(0.875f, "semi-condensed"),
    SemiExpanded(1.125f, "semi-expanded"),
    Expanded(1.25f),
    ExtraExpanded(1.5f, "extra-expanded"),
    UltraExpanded(2f, "ultra-expanded"),
    /**
     * Allowed values range from 50% to 200%.
     */
    Percentage(-1);

    private final @NotNull Percentage percentage;
    private final @NotNull String matchName;

    FontStretch(float percentage, @NotNull String matchName) {
        this.percentage = new Percentage(percentage);
        this.matchName = matchName;
    }

    FontStretch(float percentage) {
        this.percentage = new Percentage(percentage);
        this.matchName = name();
    }

    @Override
    public @NotNull String matchName() {
        return matchName;
    }

    public @NotNull Percentage percentage() {
        if (this == Percentage) {
            throw new UnsupportedOperationException("Percentage needs to be computed manually");
        }
        return percentage;
    }
}
