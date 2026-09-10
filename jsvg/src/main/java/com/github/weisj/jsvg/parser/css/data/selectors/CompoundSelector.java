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

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.impl.ParsedElement;
import com.google.errorprone.annotations.Immutable;

/**
 * A <a href="https://www.w3.org/TR/selectors-4/#compound">compound selector</a> (Selectors Level 4):
 * simple selectors not separated by a combinator, all matching the same element (e.g. {@code a.bar#baz}).
 */
@Immutable
public final class CompoundSelector {

    private final @NotNull List<? extends @NotNull SimpleSelector> simpleSelectors;

    /** Takes ownership of {@code simpleSelectors}. */
    public CompoundSelector(@NotNull List<? extends @NotNull SimpleSelector> simpleSelectors) {
        if (simpleSelectors.isEmpty()) {
            throw new IllegalArgumentException("compound selector must contain at least one simple selector");
        }
        this.simpleSelectors = simpleSelectors;
    }

    public @NotNull List<? extends @NotNull SimpleSelector> simpleSelectors() {
        return simpleSelectors;
    }

    public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
        boolean usesPosition = false;
        for (SimpleSelector simple : simpleSelectors) {
            MatchResult result = simple.matches(targetElement);
            usesPosition |= result.selectorsUseElementPositionInDom;
            if (!result.matches) return new MatchResult(false, usesPosition);
        }
        return new MatchResult(true, usesPosition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompoundSelector)) return false;
        return simpleSelectors.equals(((CompoundSelector) o).simpleSelectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simpleSelectors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SimpleSelector simple : simpleSelectors) {
            sb.append(simple);
        }
        return sb.toString();
    }
}
