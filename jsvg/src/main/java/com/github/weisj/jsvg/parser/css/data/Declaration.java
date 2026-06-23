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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

/**
 * Declaration following <a href="https://www.w3.org/TR/css-syntax-3/#parsing">CSS Syntax Module Level 3, §5</a>:
 * a property name, a raw component-value {@link #value()}, and the {@link #important()} flag that §5.4.6 strips
 * from the trailing {@code !important} (cascade effect per
 * <a href="https://www.w3.org/TR/css-cascade-4/#importance">CSS Cascade Level 4, §6.3</a>).
 */
@Immutable
public final class Declaration implements DeclarationListItem {
    private final @NotNull String name;
    private final @NotNull List<@NotNull ComponentValue> value;
    private final boolean important;

    public Declaration(@NotNull String name, @NotNull List<@NotNull ComponentValue> value, boolean important) {
        this.name = name;
        this.value = Collections.unmodifiableList(new ArrayList<>(value));
        this.important = important;
    }

    /** Lowercased, normalized property name (e.g. {@code "fill"}, {@code "font-size"}). */
    public @NotNull String name() {
        return name;
    }

    /** Declaration value, after ":" and before {@code !important}, with trimmed leading and trailing whitespaces */
    public @NotNull List<@NotNull ComponentValue> value() {
        return value;
    }

    /** {@code true} iff the declaration was suffixed with {@code !important}. */
    public boolean important() {
        return important;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Declaration)) return false;
        Declaration that = (Declaration) o;
        return important == that.important && name.equals(that.name) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, important);
    }

    @Override
    public String toString() {
        return "Declaration{name='" + name + "', value=" + value + (important ? ", !important" : "") + "}";
    }
}
