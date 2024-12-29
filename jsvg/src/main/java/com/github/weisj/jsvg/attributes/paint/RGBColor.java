/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.attributes.paint;

import static com.github.weisj.jsvg.util.ColorUtil.clampColor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.util.Objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class RGBColor implements Paint, ColorValue {
    public static final RGBColor INHERITED = new RGBColor(0, 0, 0, 0);
    public static final RGBColor DEFAULT = new RGBColor(0, 0, 0, 255);
    private final int r;
    private final int g;
    private final int b;
    private final int a;
    private Color color;

    public RGBColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public RGBColor(@NotNull Color color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        this.color = color;
    }

    public static @NotNull RGBColor interpolate(float t, @NotNull RGBColor a, @NotNull RGBColor b) {
        return new RGBColor(
                Math.round(a.r + (b.r - a.r) * t),
                Math.round(a.g + (b.g - a.g) * t),
                Math.round(a.b + (b.b - a.b) * t),
                Math.round(a.a + (b.a - a.a) * t));
    }

    public static @NotNull RGBColor saxpy(float t, @NotNull RGBColor a, @NotNull RGBColor b) {
        return new RGBColor(
                Math.round(a.r + t * b.r),
                Math.round(a.g + t * b.g),
                Math.round(a.b + t * b.b),
                Math.round(a.a + t * b.a));
    }

    public static @NotNull RGBColor add(@NotNull RGBColor a, @NotNull RGBColor b) {
        return new RGBColor(
                a.r + b.r,
                a.g + b.g,
                a.b + b.b,
                a.a + b.a);
    }

    @NotNull
    public Color toColor() {
        if (color == null) {
            color = new Color(clampColor(r), clampColor(g), clampColor(b), clampColor(a));
        }
        return color;
    }

    public boolean isVisible() {
        return a > 0;
    }

    public static boolean isVisible(@NotNull Color c) {
        return c.getAlpha() > 0;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String toString() {
        return "ColorValue{" +
                "r=" + r +
                ", g=" + g +
                ", b=" + b +
                ", a=" + a +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RGBColor that = (RGBColor) o;
        return r == that.r && g == that.g && b == that.b && a == that.a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a);
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
            AffineTransform xform, RenderingHints hints) {
        return toColor().createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    @Override
    public int getTransparency() {
        return toColor().getTransparency();
    }

    @Override
    public @NotNull Color get(@NotNull MeasureContext context) {
        return toColor();
    }
}
