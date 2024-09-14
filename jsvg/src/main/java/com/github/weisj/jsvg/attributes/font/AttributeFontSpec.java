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
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.prototype.Mutator;

public final class AttributeFontSpec extends FontSpec implements Mutator<MeasurableFontSpec> {
    private final @Nullable FontSize size;
    private final @Nullable FontWeight weight;

    AttributeFontSpec(@NotNull String[] families, @Nullable FontStyle style, @Nullable Length sizeAdjust,
            @NotNull Percentage stretch, @Nullable FontSize size, @Nullable FontWeight weight) {
        super(families, style, sizeAdjust, stretch);
        this.size = size;
        this.weight = weight;
    }

    public @Nullable FontWeight weight() {
        return weight;
    }

    public @Nullable FontSize size() {
        return size;
    }

    @Override
    public @NotNull MeasurableFontSpec mutate(@NotNull MeasurableFontSpec element) {
        return element.derive(this);
    }

    @Override
    public String toString() {
        return "AttributeFontSpec{" +
                "families=" + Arrays.toString(families) +
                ", style=" + style +
                ", weight=" + weight +
                ", size=" + size +
                ", sizeAdjust=" + sizeAdjust +
                ", stretch=" + stretch +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeFontSpec)) return false;
        if (!super.equals(o)) return false;
        AttributeFontSpec fontSpec = (AttributeFontSpec) o;
        return Objects.equals(size, fontSpec.size) && Objects.equals(weight, fontSpec.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), size, weight);
    }
}
