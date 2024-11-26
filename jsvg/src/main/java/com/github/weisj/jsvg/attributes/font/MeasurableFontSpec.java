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

import java.util.Arrays;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.geometry.size.Unit;

public final class MeasurableFontSpec extends FontSpec {
    public static final @NotNull String DEFAULT_FONT_FAMILY_NAME = "Default";
    private final int currentWeight;
    private final @NotNull Length currentSize;

    MeasurableFontSpec(@NotNull String[] families, @Nullable FontStyle style, @Nullable Length sizeAdjust,
            @NotNull Percentage stretch, int currentWeight, @NotNull Length currentSize) {
        super(families, style, sizeAdjust, stretch);
        this.currentWeight = currentWeight;
        this.currentSize = currentSize;
    }

    public static @NotNull MeasurableFontSpec createDefault() {
        return new MeasurableFontSpec(
                new String[] {DEFAULT_FONT_FAMILY_NAME},
                FontStyle.normal(),
                null,
                FontStretch.Normal.percentage(),
                PredefinedFontWeight.NORMAL_WEIGHT,
                Unit.RAW.valueOf(SVGFont.defaultFontSize()));
    }

    public @NotNull String[] families() {
        return families;
    }

    public @NotNull FontStyle style() {
        assert style != null;
        return style;
    }

    public @NotNull Percentage stretch() {
        return stretch;
    }

    public int currentWeight() {
        return currentWeight;
    }

    public @NotNull Length currentSize() {
        return currentSize;
    }

    public float effectiveSize(@NotNull MeasureContext context) {
        float emSize = currentSize().resolveFontSize(context);
        if (sizeAdjust != null) {
            return SVGFont.emFromEx(emSize * sizeAdjust.resolveFontSize(context));
        }
        return emSize;
    }

    public @NotNull MeasurableFontSpec withFontSize(@Nullable FontSize size, @Nullable Length sizeAdjust) {
        if (size == null && sizeAdjust == null) return this;
        return new MeasurableFontSpec(families, style, sizeAdjust != null ? sizeAdjust : this.sizeAdjust, stretch,
                currentWeight, size != null ? size.size(currentSize) : this.currentSize);
    }

    public @NotNull MeasurableFontSpec derive(@Nullable AttributeFontSpec other) {
        if (other == null) return this;
        String[] newFamilies = other.families != null && other.families.length > 0
                ? other.families
                : this.families;
        FontStyle newStyle = other.style != null
                ? other.style
                : this.style;
        int newWeight = other.weight() != null
                ? other.weight().weight(currentWeight)
                : this.currentWeight;
        Length newSize = other.size() != null
                ? other.size().size(currentSize)
                : this.currentSize;
        Length newSizeAdjust = other.sizeAdjust != null
                ? other.sizeAdjust
                : this.sizeAdjust;
        Percentage newStretch = other.stretch.isSpecified()
                ? other.stretch
                : this.stretch;
        return new MeasurableFontSpec(newFamilies, newStyle, newSizeAdjust, newStretch, newWeight, newSize);
    }

    @Override
    public String toString() {
        return "MeasurableFontSpec{" +
                "families=" + Arrays.toString(families) +
                ", style=" + style +
                ", sizeAdjust=" + sizeAdjust +
                ", stretch=" + stretch +
                ", currentWeight=" + currentWeight +
                ", currentSize=" + currentSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeasurableFontSpec)) return false;
        if (!super.equals(o)) return false;
        MeasurableFontSpec fontSpec = (MeasurableFontSpec) o;
        return currentWeight == fontSpec.currentWeight && currentSize.equals(fontSpec.currentSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), currentWeight, currentSize);
    }
}
