/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.css.data;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * A normalized longhand property: the product of phase-3 normalization (shorthands expanded, at-rules
 * dropped) that feeds the cascade
 * (<a href="https://www.w3.org/TR/css-cascade-4/#cascade">CSS Cascading and Inheritance Level 4, §6</a>).
 * <p>
 * Unlike the syntactic {@link Declaration} it carries a {@link #sourceOrder}, assigned once when the
 * containing stylesheet is registered and used as the final cascade tie-breaker.
 */
public final class NormalizedProperty {
    private final @NotNull String name;
    private final @NotNull List<@NotNull ComponentValue> value;
    private final boolean important;
    private int sourceOrder;

    /** Takes ownership of {@code value}. */
    public NormalizedProperty(@NotNull String name, @NotNull List<@NotNull ComponentValue> value, boolean important) {
        this.name = name;
        this.value = value;
        this.important = important;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<@NotNull ComponentValue> value() {
        return value;
    }

    public boolean important() {
        return important;
    }

    /**
     * Position across all stylesheets, incrementing in document order; inline declarations restart from 0
     * since their higher specificity already wins.
     */
    public int sourceOrder() {
        return sourceOrder;
    }

    public void setSourceOrder(int sourceOrder) {
        this.sourceOrder = sourceOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NormalizedProperty)) return false;
        NormalizedProperty that = (NormalizedProperty) o;
        return important == that.important && name.equals(that.name) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, important);
    }

    @Override
    public String toString() {
        return "NormalizedProperty{name='" + name + "', value=" + value + (important ? ", !important" : "") + "}";
    }
}
