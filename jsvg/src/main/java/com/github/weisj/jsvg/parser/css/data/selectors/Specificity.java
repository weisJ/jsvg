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

import java.util.Comparator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

/**
 * Selector specificity following <a href="https://www.w3.org/TR/selectors-4/#specificity">Selectors Level 4</a>:
 * the quadruple {@code (inline, id, class, type)} compared lexicographically, where {@code class} also counts
 * attribute and pseudo-class selectors and {@code type} counts pseudo-elements ({@code *} counts for nothing).
 * The leading {@code inline} flag ranks inline (style-attribute) declarations above all stylesheet rules.
 */
@Immutable
public final class Specificity implements Comparable<Specificity> {

    public static final @NotNull Specificity ZERO_IN_STYLESHEET = new Specificity(false, 0, 0, 0);
    public static final @NotNull Specificity ONE_ID = new Specificity(false, 1, 0, 0);
    public static final @NotNull Specificity ONE_CLASS = new Specificity(false, 0, 1, 0);
    public static final @NotNull Specificity ONE_TYPE = new Specificity(false, 0, 0, 1);
    public static final @NotNull Specificity INLINE = new Specificity(true, 0, 0, 0);

    private static final @NotNull Comparator<Specificity> CASCADE_ORDER =
            Comparator.comparing(Specificity::inline)
                    .thenComparing(Specificity::idSelectors)
                    .thenComparing(Specificity::classSelectors)
                    .thenComparing(Specificity::typeSelectors);

    private final boolean inline;
    private final int idSelectors;
    private final int classSelectors;
    private final int typeSelectors;

    public Specificity(boolean inline, int idSelectors, int classSelectors, int typeSelectors) {
        this.inline = inline;
        this.idSelectors = idSelectors;
        this.classSelectors = classSelectors;
        this.typeSelectors = typeSelectors;
    }

    public boolean inline() {
        return inline;
    }

    public int idSelectors() {
        return idSelectors;
    }

    public int classSelectors() {
        return classSelectors;
    }

    public int typeSelectors() {
        return typeSelectors;
    }

    @Override
    public int compareTo(@NotNull Specificity other) {
        return CASCADE_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Specificity)) return false;
        Specificity that = (Specificity) o;
        return inline == that.inline && idSelectors == that.idSelectors && classSelectors == that.classSelectors
                && typeSelectors == that.typeSelectors;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inline, idSelectors, classSelectors, typeSelectors);
    }

    @Override
    public String toString() {
        return "(" + inline + "," + idSelectors + "," + classSelectors + "," + typeSelectors + ")";
    }
}
