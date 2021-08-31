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
import java.awt.font.FontRenderContext;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;

public class AWTSVGFont implements SVGFont {
    private final @NotNull Font font;
    private final @NotNull FontStyle style;
    private final @Percentage float stretch;
    private final int weight;

    public AWTSVGFont(@NotNull Font font, @NotNull FontStyle style,
            int weight, @Percentage float stretch) {
        this.font = font;
        this.style = style;
        this.weight = weight;
        this.stretch = stretch;
    }

    public @NotNull Font font() {
        return font;
    }

    @Override
    public @NotNull String family() {
        return font.getName();
    }

    @Override
    public @NotNull FontStyle style() {
        return style;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public @Percentage float stretch() {
        return stretch;
    }

    @Override
    public @NotNull Length size() {
        return Unit.Raw.valueOf(font.getSize2D());
    }

    @Override
    public float em(@NotNull FontRenderContext context) {
        return font.getSize2D();
    }

    @Override
    public float ex(@NotNull FontRenderContext frc) {
        return (float) font.createGlyphVector(frc, "x").getLogicalBounds().getHeight();
    }
}
