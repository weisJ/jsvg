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
package com.github.weisj.jsvg.parser.css.impl.phase3ruleparse;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Declaration;
import com.github.weisj.jsvg.parser.css.data.DeclarationListItem;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.Rule;
import com.github.weisj.jsvg.parser.css.data.StyleRule;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.parser.css.data.selectors.ComplexSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.CompoundSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.SelectorList;
import com.github.weisj.jsvg.parser.css.data.selectors.SimpleSelector;
import com.github.weisj.jsvg.parser.css.impl.FullCssParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.renderer.CssHints;

/**
 * Resolves a CSS stylesheet into a {@link StyleRuleList} or a style attribute into a list of {@link Declaration}s.
 */
public final class CssNormalizer {

    private CssNormalizer() {}

    /**
     * {@code List<QualifiedRule|AtRule> -> List<StyleRule>}
     * <p>
     * Resolves top-level rules to {@link StyleRule}s: qualified rules via {@link SelectorParser}
     * (invalid selector ⇒ dropped + logged); {@code @media} at-rules whose prelude matches the active
     * {@link CssHints} are flattened in source order; all other at-rules dropped.
     */
    public static @NotNull StyleRuleList normalizeStyleSheet(@NotNull List<Rule> parsedRules, @NotNull CssHints hints) {
        Stream<StyleRule> styleRules = normalizeStyleSheet(parsedRules.stream(), hints, true);
        return new StyleRuleList(styleRules.collect(Collectors.toList()));
    }

    /**
     * {@code List<Declaration|AtRule> -> List<NormalizedProperty>}
     * <p>
     * Normalizes a style attribute into a list of longhand properties; skips at-rules.
     */
    public static @NotNull List<@NotNull NormalizedProperty> normalizeAttribute(
            List<@NotNull DeclarationListItem> input) {
        return removeAtRulesAndExpandShorthand(input, "style attribute");
    }

    private static @NotNull Stream<@NotNull StyleRule> normalizeStyleSheet(
            @NotNull Stream<Rule> parsedRules, @NotNull CssHints hints, boolean allowAtRules) {
        return parsedRules.flatMap(rule -> rule instanceof Rule.QualifiedRule
                ? normalizeStyleRule((Rule.QualifiedRule) rule)
                : (allowAtRules ? normalizeAtRule((Rule.AtRule) rule, hints) : Stream.empty()));
    }

    /** Each selector in the comma-separated list produces its own {@link StyleRule}. */
    private static @NotNull Stream<@NotNull StyleRule> normalizeStyleRule(@NotNull Rule.QualifiedRule qr) {
        SelectorList selectors = new SelectorParser(qr.prelude()).parse();
        if (selectors == null) {
            FullCssParser.logParseEvent(MessageFormat.format(
                    "Invalid selector, rule dropped: {0}", summarize(qr.prelude())));
            return Stream.empty();
        }
        List<DeclarationListItem> parsedDeclarations = BasicParser.parseListOfDeclarations(
                BasicParserInput.fromComponentValues(qr.block().value()));

        List<NormalizedProperty> supportedDeclarations =
                removeAtRulesAndExpandShorthand(parsedDeclarations, "declaration block");
        if (supportedDeclarations.isEmpty()) {
            return Stream.empty();
        }

        Stream<ComplexSelector> supportedSelectors = selectors.selectors().stream()
                .filter(CssNormalizer::isSupported);

        return supportedSelectors.map(complexSelector -> new StyleRule(complexSelector, supportedDeclarations));
    }

    private static @NotNull Stream<@NotNull StyleRule> normalizeAtRule(@NotNull Rule.AtRule atRule,
            @NotNull CssHints hints) {
        if (!"media".equalsIgnoreCase(atRule.name())) {
            // only @media rules are supported
            return Stream.empty();
        }

        boolean conditionMatches = MediaAtRuleConditionEvaluator.matches(atRule.prelude(), hints);
        if (!conditionMatches) {
            // @media condition did not match or is invalid
            FullCssParser.logParseEvent(MessageFormat.format(
                    "@media query did not match, block dropped: {0}", summarize(atRule.prelude())));
            return Stream.empty();
        }
        if (atRule.block() == null) {
            // @media block is empty
            return Stream.empty();
        }
        // recurse into @media block contents
        List<ComponentValue> blockContents = atRule.block().value();
        List<Rule> parsedRules = BasicParser.parseListOfRules(BasicParserInput.fromComponentValues(blockContents));
        return normalizeStyleSheet(parsedRules.stream(), hints, false);
    }

    /** Pseudo-elements are not supported in CSS selectors */
    private static boolean isSupported(ComplexSelector complexSelector) {
        for (CompoundSelector compoundSelector : complexSelector.sequences()) {
            if (!isSupported(compoundSelector)) {
                return false;
            }
        }
        return true;
    }

    /** Pseudo-elements are not supported in CSS selectors */
    private static boolean isSupported(CompoundSelector compoundSelector) {
        for (SimpleSelector simpleSelector : compoundSelector.simpleSelectors()) {
            if (simpleSelector instanceof SimpleSelector.PseudoElement) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code List<Declaration|AtRule> -> List<NormalizedProperty>}
     * <p>
     * Post-processes a §5.4.5 declaration list: drops at-rules (logged) and expands shorthands.
     * Shorthands must be expanded here, before cascading and inheritance.
     */
    private static @NotNull List<@NotNull NormalizedProperty> removeAtRulesAndExpandShorthand(
            @NotNull List<@NotNull DeclarationListItem> items, @NotNull String context) {
        List<NormalizedProperty> out = new ArrayList<>();
        for (DeclarationListItem item : items) {
            if (item instanceof Declaration) {
                out.addAll(ShorthandExpander.expand((Declaration) item));
            } else {
                FullCssParser.logParseEvent(MessageFormat.format("At-rule in {0} dropped: {1}", context, item));
            }
        }
        return out;
    }

    /** Short, single-line description of a component-value list, for log messages. */
    private static @NotNull String summarize(@NotNull List<@NotNull ComponentValue> values) {
        StringBuilder sb = new StringBuilder();
        for (ComponentValue cv : values) {
            sb.append(cv);
            if (sb.length() > 80) {
                sb.setLength(80);
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }
}
