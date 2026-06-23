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

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.StyleRule;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.parser.impl.ParsedElement;

public class StyleSheets {
    private final @NotNull StyleRuleMatcher rules = new StyleRuleMatcher();
    private int sourceOrder = 0;

    public void add(@NotNull StyleRuleList styleRuleList) {
        for (StyleRule rule : styleRuleList.rules()) {
            updateSourceOrders(rule);
            rules.add(rule);
        }
    }

    /**
     * Computes the matched CSS rules and returns the cascaded value for each CSS attribute.
     * This step does not include inheritance from parents.
     */
    public @NotNull CascadeResult matchAndCascade(
            @NotNull List<@NotNull NormalizedProperty> inlineDeclarations,
            @NotNull ParsedElement targetElement) {

        return rules.matchAndCascade(inlineDeclarations, targetElement);
    }

    private void updateSourceOrders(@NotNull StyleRule rule) {
        // done here because parsing of @media rules as well as shorthand attribute expansions create
        // declarations after the rest of the parsing is done, so they would get incorrect source orders
        rule.declarations().forEach(property -> property.setSourceOrder(sourceOrder++));
    }
}
