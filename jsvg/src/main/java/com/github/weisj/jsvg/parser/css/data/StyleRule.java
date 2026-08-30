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

import com.github.weisj.jsvg.parser.css.data.selectors.ComplexSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.SelectorList;

/**
 * A resolved style rule: a single {@link ComplexSelector} (one entry of a qualified rule's
 * {@link SelectorList}) paired with its block normalized into {@link NormalizedProperty}s.
 */
public final class StyleRule {

    private final @NotNull ComplexSelector selector;
    // NormalizedProperty carries a mutable sourceOrder set once at registration.
    private final @NotNull List<? extends @NotNull NormalizedProperty> declarations;

    /** Takes ownership of {@code declarations}. */
    public StyleRule(@NotNull ComplexSelector selector,
            @NotNull List<? extends @NotNull NormalizedProperty> declarations) {
        this.selector = selector;
        this.declarations = declarations;
    }

    public @NotNull ComplexSelector selector() {
        return selector;
    }

    public @NotNull List<? extends @NotNull NormalizedProperty> declarations() {
        return declarations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StyleRule)) return false;
        StyleRule that = (StyleRule) o;
        return selector.equals(that.selector) && declarations.equals(that.declarations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, declarations);
    }

    @Override
    public String toString() {
        return "StyleRule{selector=" + selector + ", declarations=" + declarations + "}";
    }
}
