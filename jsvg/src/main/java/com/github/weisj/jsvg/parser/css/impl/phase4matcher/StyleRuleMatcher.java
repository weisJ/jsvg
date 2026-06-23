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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.StyleRule;
import com.github.weisj.jsvg.parser.css.data.selectors.MatchResult;
import com.github.weisj.jsvg.parser.css.data.selectors.SimpleSelector;
import com.github.weisj.jsvg.parser.impl.AttributeValue;
import com.github.weisj.jsvg.parser.impl.ParsedElement;
import com.github.weisj.jsvg.util.PrefixTrie;

public class StyleRuleMatcher {
    private final @NotNull List<@NotNull StyleRule> universalRules = new ArrayList<>();
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull StyleRule>> idRules = new HashMap<>();
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull StyleRule>> classRules = new HashMap<>();
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull StyleRule>> tagNameRules = new HashMap<>();
    private final @NotNull AttributeRules attributeRules = new AttributeRules();

    // Rules indexed by attribute name (always case-sensitive in SVG) and attribute selector.
    // Cs = case-sensitive value, Ci = case-insensitive (attribute selector stored lowercased).
    private static class AttributeRules {
        private final @NotNull Map<String, List<StyleRule>> hasAttributeName = new HashMap<>(); // [name]

        private final @NotNull Map<String, Map<String, List<StyleRule>>> equalsCs = new HashMap<>(); // [name=value]
        private final @NotNull Map<String, Map<String, List<StyleRule>>> equalsCi = new HashMap<>();

        private final @NotNull Map<String, Map<String, List<StyleRule>>> includesCs = new HashMap<>(); // [name~=value]
        private final @NotNull Map<String, Map<String, List<StyleRule>>> includesCi = new HashMap<>();

        private final @NotNull Map<String, Map<String, List<StyleRule>>> dashMatchCs = new HashMap<>(); // [name|=value]
        private final @NotNull Map<String, Map<String, List<StyleRule>>> dashMatchCi = new HashMap<>();

        private final @NotNull Map<String, PrefixTrie<StyleRule>> hasPrefixCs = new HashMap<>(); // [name^=value]
        private final @NotNull Map<String, PrefixTrie<StyleRule>> hasPrefixCi = new HashMap<>();

        // [name$=value], values stored reversed
        private final @NotNull Map<String, PrefixTrie<StyleRule>> hasSuffixCs = new HashMap<>();
        private final @NotNull Map<String, PrefixTrie<StyleRule>> hasSuffixCi = new HashMap<>();

        // [name*=value]; not cached since substring testing is expensive
        private final @NotNull Map<String, List<StyleRule>> hasSubstringOperator = new HashMap<>();
    }

    /** Buckets the rule by the most selective simple selector of its rightmost compound selector. */
    public void add(@NotNull StyleRule rule) {
        List<SimpleSelector> lastCompoundSelector = rule.selector().rightmostSequence().simpleSelectors();
        // lastCompoundSelector list is non-empty, so it always has a maximum
        SimpleSelector mostSelective = lastCompoundSelector.stream().max(MOST_SELECTIVE_SELECTOR_COMPARATOR).get();
        getOrCreateBucket(mostSelective).add(rule);
    }

    private @NotNull List<StyleRule> getOrCreateBucket(@NotNull SimpleSelector selector) {
        if (selector instanceof SimpleSelector.Id) {
            return idRules.computeIfAbsent(
                    ((SimpleSelector.Id) selector).id(), k -> new ArrayList<>());
        }
        if (selector instanceof SimpleSelector.Class) {
            return classRules.computeIfAbsent(
                    ((SimpleSelector.Class) selector).name(), k -> new ArrayList<>());
        }
        if (selector instanceof SimpleSelector.Type) {
            return tagNameRules.computeIfAbsent(
                    ((SimpleSelector.Type) selector).name(), k -> new ArrayList<>());
        }
        if (selector instanceof SimpleSelector.Attribute) {
            return getOrCreateBucket((SimpleSelector.Attribute) selector);
        }
        if (selector instanceof SimpleSelector.Universal) {
            return universalRules;
        }
        if (selector instanceof SimpleSelector.PseudoClass) {
            // not indexable by any element attribute; must be tested against every element
            return universalRules;
        }
        throw new IllegalArgumentException("Unknown selector type: " + selector);
    }

    private @NotNull List<StyleRule> getOrCreateBucket(@NotNull SimpleSelector.Attribute selector) {
        @NotNull String selectorName = selector.name();

        if (selector.operator() == null) {
            return attributeRules.hasAttributeName.computeIfAbsent(selectorName, k -> new ArrayList<>());
        }

        boolean caseSensitive = selector.caseSensitive() != null ? selector.caseSensitive()
                : !ATTRIBUTES_WITH_CASE_INSENSITIVE_VALUES.contains(selector.name());
        // if Selector.operator is set, then Selector.value is also set (otherwise parsing would have
        // failed)
        @NotNull String selectorValue = caseSensitive
                ? selector.value()
                : selector.value().toLowerCase(Locale.ENGLISH);

        switch (selector.operator()) {
            case EQUALS: {
                return (caseSensitive ? attributeRules.equalsCs : attributeRules.equalsCi)
                        .computeIfAbsent(selectorName, k -> new HashMap<>())
                        .computeIfAbsent(selectorValue, k -> new ArrayList<>());
            }
            case INCLUDES: {
                return (caseSensitive ? attributeRules.includesCs : attributeRules.includesCi)
                        .computeIfAbsent(selectorName, k -> new HashMap<>())
                        .computeIfAbsent(selectorValue, k -> new ArrayList<>());
            }
            case DASH_MATCH: {
                return (caseSensitive ? attributeRules.dashMatchCs : attributeRules.dashMatchCi)
                        .computeIfAbsent(selectorName, k -> new HashMap<>())
                        .computeIfAbsent(selectorValue, k -> new ArrayList<>());
            }
            case PREFIX: {
                return (caseSensitive ? attributeRules.hasPrefixCs : attributeRules.hasPrefixCi)
                        .computeIfAbsent(selectorName, k -> new PrefixTrie<>())
                        .insert(selectorValue);
            }
            case SUFFIX: {
                return (caseSensitive ? attributeRules.hasSuffixCs : attributeRules.hasSuffixCi)
                        .computeIfAbsent(selectorName, k -> new PrefixTrie<>())
                        .insert(new StringBuilder(selectorValue).reverse().toString());
            }
            case SUBSTRING: {
                return attributeRules.hasSubstringOperator.computeIfAbsent(selectorName, k -> new ArrayList<>());
            }
            default: {
                throw new IllegalArgumentException("Unsupported selector operator: " + selector.operator());
            }
        }
    }

    public @NotNull CascadeResult matchAndCascade(
            @NotNull List<@NotNull NormalizedProperty> inlineDeclarations, @NotNull ParsedElement targetElement) {
        // keeps the highest priority value according to the cascade for each attribute
        // and updates it for every match
        Map<String, MatchedValue> mostSpecificValuePerAttributeName = new HashMap<>();
        // set if any evaluated selector's outcome depends on the element's position in the DOM
        boolean[] selectorsUseElementPositionInDom = {false};

        // add inline declarations (they always match)
        for (NormalizedProperty property : inlineDeclarations) {
            MatchedValue max = mostSpecificValuePerAttributeName.get(property.name());
            MatchedValue match = MatchedValue.fromInlineDeclaration(property);
            if (max == null || match.compareTo(max) > 0) {
                mostSpecificValuePerAttributeName.put(property.name(), match);
            }
        }

        // add matching <style> rules
        forEachCandidateMatch(targetElement, candidates -> {
            for (StyleRule candidateRule : candidates) {
                MatchResult matchResult = candidateRule.selector().matches(targetElement);
                selectorsUseElementPositionInDom[0] |= matchResult.selectorsUseElementPositionInDom;
                if (matchResult.matches) {
                    for (NormalizedProperty property : candidateRule.declarations()) {
                        MatchedValue max = mostSpecificValuePerAttributeName.get(property.name());
                        MatchedValue match = MatchedValue.fromStylesheetDeclaration(candidateRule.selector(), property);
                        if (max == null || match.compareTo(max) > 0) {
                            mostSpecificValuePerAttributeName.put(property.name(), match);
                        }
                    }
                }
            }
        });

        Map<String, AttributeValue.Parsed> results = mostSpecificValuePerAttributeName.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().value()));
        return new CascadeResult(results, selectorsUseElementPositionInDom[0]);
    }

    private void forEachCandidateMatch(@NotNull ParsedElement targetElement,
            @NotNull Consumer<@NotNull List<StyleRule>> consumer) {
        if (targetElement.id() != null) {
            consumer.accept(idRules.getOrDefault(targetElement.id(), Collections.emptyList()));
        }
        for (String className : targetElement.classNames()) {
            consumer.accept(classRules.getOrDefault(className, Collections.emptyList()));
        }
        consumer.accept(tagNameRules.getOrDefault(targetElement.tagName(), Collections.emptyList()));
        consumer.accept(universalRules);
        for (Map.Entry<String, String> declaredAttr : targetElement.attributeNode().declaredAttributes().entrySet()) {
            String name = declaredAttr.getKey();
            String value = declaredAttr.getValue();
            String valueLc = value.toLowerCase();

            consumer.accept(attributeRules.hasAttributeName.getOrDefault(name, Collections.emptyList()));

            consumer.accept(attributeRules.equalsCs.getOrDefault(name, Collections.emptyMap())
                    .getOrDefault(value, Collections.emptyList()));
            consumer.accept(attributeRules.equalsCi.getOrDefault(name, Collections.emptyMap())
                    .getOrDefault(valueLc, Collections.emptyList()));

            Map<String, List<StyleRule>> testRules = attributeRules.includesCs.get(name);
            if (testRules != null) {
                for (String word : value.split("\\s+")) {
                    consumer.accept(testRules.getOrDefault(word, Collections.emptyList()));
                }
            }
            testRules = attributeRules.includesCi.get(name);
            if (testRules != null) {
                for (String word : valueLc.split("\\s+")) {
                    consumer.accept(testRules.getOrDefault(word, Collections.emptyList()));
                }
            }

            testRules = attributeRules.dashMatchCs.get(name);
            if (testRules != null) {
                int dashIndex = value.indexOf('-');
                String language = dashIndex != -1 ? value.substring(0, dashIndex) : value;
                consumer.accept(testRules.getOrDefault(language, Collections.emptyList()));
            }
            testRules = attributeRules.dashMatchCi.get(name);
            if (testRules != null) {
                int dashIndex = valueLc.indexOf('-');
                String languageLc = dashIndex != -1 ? valueLc.substring(0, dashIndex) : valueLc;
                consumer.accept(testRules.getOrDefault(languageLc, Collections.emptyList()));
            }

            PrefixTrie<StyleRule> prefixTrie = attributeRules.hasPrefixCs.get(name);
            if (prefixTrie != null) {
                consumer.accept(prefixTrie.matchPrefixes(value));
            }
            prefixTrie = attributeRules.hasPrefixCi.get(name);
            if (prefixTrie != null) {
                consumer.accept(prefixTrie.matchPrefixes(valueLc));
            }

            PrefixTrie<StyleRule> suffixTrie = attributeRules.hasSuffixCs.get(name);
            if (suffixTrie != null) {
                consumer.accept(suffixTrie.matchPrefixes(new StringBuilder(value).reverse().toString()));
            }
            suffixTrie = attributeRules.hasSuffixCi.get(name);
            if (suffixTrie != null) {
                consumer.accept(suffixTrie.matchPrefixes(new StringBuilder(valueLc).reverse().toString()));
            }

            consumer.accept(attributeRules.hasSubstringOperator.getOrDefault(name, Collections.emptyList()));
        }
    }

    // Ranks how good a selector is as an index/bucket key (NOT its CSS specificity). A pseudo-class is
    // not indexable, so despite its (0,1,0) specificity it must rank lowest: any concrete selector in
    // the same compound is preferred, and a pseudo-only compound falls through to universalRules.
    private static final @NotNull Comparator<SimpleSelector> MOST_SELECTIVE_SELECTOR_COMPARATOR =
            Comparator.comparingInt(s -> {
                if (s instanceof SimpleSelector.Id) return 5;
                if (s instanceof SimpleSelector.Class) return 4;
                if (s instanceof SimpleSelector.Attribute) return 3;
                if (s instanceof SimpleSelector.Type) return 2;
                if (s instanceof SimpleSelector.Universal) return 1;
                if (s instanceof SimpleSelector.PseudoClass) return 0;
                return 0;
            });

    /**
     * Attributes whose <em>value</em> is case-insensitive by default in attribute selectors (all others are
     * case-sensitive; attribute names are always case-sensitive). Per the
     * <a href="https://html.spec.whatwg.org/multipage/semantics-other.html#case-sensitivity-of-selectors">HTML Standard</a>.
     */
    public static final @NotNull Set<String> ATTRIBUTES_WITH_CASE_INSENSITIVE_VALUES = new HashSet<>(Arrays.asList(
            "accept",
            "accept-charset",
            "align",
            "alink",
            "axis",
            "bgcolor",
            "charset",
            "checked",
            "clear",
            "codetype",
            "color",
            "compact",
            "declare",
            "defer",
            "dir",
            "direction",
            "disabled",
            "enctype",
            "face",
            "frame",
            "hreflang",
            "http-equiv",
            "lang",
            "language",
            "link",
            "media",
            "method",
            "multiple",
            "nohref",
            "noresize",
            "noshade",
            "nowrap",
            "readonly",
            "rel",
            "rev",
            "rules",
            "scope",
            "scrolling",
            "selected",
            "shape",
            "target",
            "text",
            "type",
            "valign",
            "valuetype",
            "vlink"));
}
