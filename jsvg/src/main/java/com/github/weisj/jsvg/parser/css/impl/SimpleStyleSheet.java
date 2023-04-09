/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.ParsedElement;
import com.github.weisj.jsvg.parser.SeparatorMode;
import com.github.weisj.jsvg.parser.css.StyleProperty;
import com.github.weisj.jsvg.parser.css.StyleSheet;

public class SimpleStyleSheet implements StyleSheet {

    private final Map<String, @NotNull List<@NotNull StyleProperty>> classRules = new HashMap<>();
    private final Map<String, @NotNull List<@NotNull StyleProperty>> idRules = new HashMap<>();
    private final Map<String, @NotNull List<@NotNull StyleProperty>> tagNameRules = new HashMap<>();


    void addTagNameRules(@NotNull String tagName, @NotNull List<@NotNull StyleProperty> rule) {
        tagNameRules.computeIfAbsent(tagName, k -> new ArrayList<>()).addAll(rule);
    }

    void addClassRules(@NotNull String className, @NotNull List<@NotNull StyleProperty> rule) {
        classRules.computeIfAbsent(className, k -> new ArrayList<>()).addAll(rule);
    }

    void addIdRules(@NotNull String id, @NotNull List<@NotNull StyleProperty> rule) {
        idRules.computeIfAbsent(id, k -> new ArrayList<>()).addAll(rule);
    }

    @Override
    public void forEachMatchingRule(@NotNull ParsedElement element, @NotNull RuleConsumer ruleConsumer) {
        List<@NotNull StyleProperty> rules = tagNameRules.get(element.node().tagName());
        if (rules != null) rules.forEach(ruleConsumer::applyRule);
        if (element.id() != null) {
            rules = idRules.get(element.id());
            if (rules != null) rules.forEach(ruleConsumer::applyRule);
        }
        for (String className : element.attributeNode().getStringList("class", SeparatorMode.WHITESPACE_ONLY)) {
            rules = classRules.get(className);
            if (rules != null) rules.forEach(ruleConsumer::applyRule);
        }
    }
}
