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
package com.github.weisj.jsvg.parser.css.data.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

/**
 * A <a href="https://www.w3.org/TR/selectors-4/#selector-list">selector list</a> (Selectors Level 4):
 * a comma-separated list of complex selectors. Each contributes independently to the cascade, so
 * {@code a, b { ... }} behaves like two rules sharing one declaration block. Its specificity is that of the
 * most specific matching selector, so it can only be computed at match time.
 */
@Immutable
public final class SelectorList {

    private final @NotNull List<@NotNull ComplexSelector> selectors;

    public SelectorList(@NotNull List<@NotNull ComplexSelector> selectors) {
        if (selectors.isEmpty()) {
            throw new IllegalArgumentException("selector list must contain at least one selector");
        }
        this.selectors = Collections.unmodifiableList(new ArrayList<>(selectors));
    }

    public @NotNull List<@NotNull ComplexSelector> selectors() {
        return selectors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectorList)) return false;
        return selectors.equals(((SelectorList) o).selectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectors);
    }

    @Override
    public String toString() {
        return selectors.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
}
