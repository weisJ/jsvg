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

import java.util.Arrays;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Percentage;

public class AttributeFontSpec extends FontSpec {
    protected final @Nullable FontSize size;
    protected final @Nullable FontWeight weight;

    AttributeFontSpec(@NotNull String[] families, @Nullable FontStyle style, @Percentage float stretch,
            @Nullable FontSize size, @Nullable FontWeight weight) {
        super(families, style, stretch);
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
    public String toString() {
        return "AttributeFontSpec{" +
                "families=" + Arrays.toString(families) +
                ", style=" + style +
                ", weight=" + weight +
                ", size=" + size +
                ", stretch=" + stretch +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeFontSpec)) return false;
        if (!super.equals(o)) return false;
        AttributeFontSpec that = (AttributeFontSpec) o;
        return Objects.equals(size, that.size) && Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), size, weight);
    }
}
