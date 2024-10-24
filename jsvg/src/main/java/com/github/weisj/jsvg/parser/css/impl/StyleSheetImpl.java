/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.parser.css.impl;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.ParsedElement;
import com.github.weisj.jsvg.parser.css.StyleSheet;
import com.github.weisj.jsvg.util.AttributeUtil;

class StyleSheetImpl implements StyleSheet {
    private final @NotNull List<@NotNull CssRule> rules;

    StyleSheetImpl(@NotNull List<@NotNull CssRule> rules) {
        this.rules = rules;
        rules.sort(Comparator.comparing(CssRule::specificity));
    }

    public List<CssRule> rules() {
        return rules;
    }

    @Override
    public void forEachMatchingRule(@NotNull ParsedElement element, @NotNull RuleConsumer ruleConsumer) {
        for (CssRule rule : rules) {
            if (matches(element, rule)) {
                System.out.println("Matched rule: " + rule.selectorString() + " element: "
                        + element.attributeNode().tagName() + " id: " + element.id() + " class: "
                        + Arrays.toString(element.classList()));
                rule.properties().forEach((k, v) -> ruleConsumer.applyRule(v));
            }
        }
    }

    private boolean matches(@NotNull ParsedElement element, @NotNull CssRule rule) {
        // Process selector parts from right to left to account for combinators.
        List<@NotNull SelectorPart> selectorParts = rule.selectorParts();
        int partIndex = selectorParts.size() - 1;
        return matchesSelectorPart(element, selectorParts, partIndex);
    }

    private boolean matchesSelectorPart(
            @NotNull ParsedElement element,
            @NotNull List<@NotNull SelectorPart> selectorParts,
            int partIndex) {
        if (partIndex < 0) {
            // All selector parts have been successfully matched
            return true;
        }

        SelectorPart part = selectorParts.get(partIndex);

        // First, check if the current element matches the selector part
        if (!matchesSingleSelectorPart(element, part)) return false;

        // Handle the combinator between this part and the previous part
        if (partIndex > 0) {
            switch (selectorParts.get(partIndex - 1).combinator()) {
                case CURRENT:
                    // The current element must match the next selector part
                    return matchesSelectorPart(element, selectorParts, partIndex - 1);
                case DESCENDANT:
                    // Look for an ancestor that matches the next selector part
                    for (ParsedElement ancestor = element.parent(); ancestor != null; ancestor = ancestor.parent()) {
                        if (matchesSelectorPart(ancestor, selectorParts, partIndex - 1)) {
                            return true;
                        }
                    }
                    return false;
                case CHILD:
                    // The parent must match the next selector part
                    ParsedElement parent = element.parent();
                    return parent != null
                            && matchesSelectorPart(parent, selectorParts, partIndex - 1);
                case ADJACENT_SIBLING:
                    // The immediately preceding sibling must match the next selector part
                    ParsedElement previousSibling = element.previousSibling();
                    return previousSibling != null
                            && matchesSelectorPart(previousSibling, selectorParts, partIndex - 1);
                case GENERAL_SIBLING:
                    // Any preceding sibling must match the next selector part
                    for (ParsedElement sibling = element.previousSibling(); sibling != null; sibling =
                            sibling.previousSibling()) {
                        if (matchesSelectorPart(sibling, selectorParts, partIndex - 1)) {
                            return true;
                        }
                    }
                    return false;
            }
        }

        return true; // No more combinators, successful match
    }

    private boolean matchesSingleSelectorPart(@NotNull ParsedElement element, @NotNull SelectorPart part) {
        switch (part.type()) {
            case ID:
                return element.id() != null && element.id().equals(part.value().substring(1));
            case CLASS:
                return AttributeUtil.arrayContains(element.classList(), part.value().substring(1));
            case ELEMENT:
                return element.attributeNode().tagName().equalsIgnoreCase(part.value());
            case PSEUDO_CLASS:
                // Pseudo-classes (like :hover or :first-child) are more complex, so we’ll skip them here for now
                return false;
            case ATTRIBUTE:
                // Attribute selectors would need further parsing, we'll skip for simplicity
                return false;
            default:
                return false;
        }
    }
}
