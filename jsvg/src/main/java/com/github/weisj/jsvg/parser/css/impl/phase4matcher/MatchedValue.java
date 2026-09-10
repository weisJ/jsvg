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
package com.github.weisj.jsvg.parser.css.impl.phase4matcher;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.selectors.ComplexSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.Specificity;
import com.github.weisj.jsvg.parser.impl.AttributeValue;

class MatchedValue implements Comparable<MatchedValue> {
    private final @NotNull AttributeValue.@NotNull Parsed value;
    private final boolean important;
    private final @NotNull Specificity specificity;
    private final int sourceOrder;

    public MatchedValue(@NotNull AttributeValue.@NotNull Parsed value, boolean important,
            @NotNull Specificity specificity, int sourceOrder) {
        this.value = value;
        this.important = important;
        this.specificity = specificity;
        this.sourceOrder = sourceOrder;
    }

    public static @NotNull MatchedValue fromInlineDeclaration(@NotNull NormalizedProperty property) {
        return new MatchedValue(new AttributeValue.Parsed(property.value()),
                property.important(), Specificity.INLINE, property.sourceOrder());
    }

    public static @NotNull MatchedValue fromStylesheetDeclaration(
            @NotNull ComplexSelector selector, @NotNull NormalizedProperty property) {
        return new MatchedValue(new AttributeValue.Parsed(property.value()),
                property.important(), selector.specificity(), property.sourceOrder());
    }

    public @NotNull AttributeValue.@NotNull Parsed value() {
        return value;
    }

    public boolean important() {
        return important;
    }

    public @NotNull Specificity specificity() {
        return specificity;
    }

    public int sourceOrder() {
        return sourceOrder;
    }

    private static final @NotNull Comparator<MatchedValue> COMPARATOR =
            Comparator.comparing(MatchedValue::important)
                    .thenComparing(MatchedValue::specificity)
                    .thenComparingInt(MatchedValue::sourceOrder);

    @Override
    public int compareTo(@NotNull MatchedValue o) {
        return COMPARATOR.compare(this, o);
    }
}
