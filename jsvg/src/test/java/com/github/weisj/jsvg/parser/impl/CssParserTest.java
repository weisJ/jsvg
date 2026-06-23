/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.impl;

import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.StyleRule;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;
import com.github.weisj.jsvg.parser.css.data.selectors.ComplexSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.CompoundSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.SimpleSelector;
import com.github.weisj.jsvg.parser.css.impl.FullCssParser;
import com.github.weisj.jsvg.renderer.CssHints;
import com.github.weisj.jsvg.util.RandomData;

class CssParserTest {

    private static @NotNull List<char[]> inputFromString(@NotNull String string) {
        return List.of(string.toCharArray());
    }

    @Test
    void invalidCssProducesNoRules() {
        FullCssParser cssParser = new FullCssParser();

        Consumer<String> assertNoRulesProduced = css -> {
            StyleRuleList sheet = cssParser.parseStyleSheet(inputFromString(css), CssHints.DEFAULT);
            assertEquals(sheet.rules(), List.of());
        };

        assertNoRulesProduced.accept(".a .b {}");
        assertNoRulesProduced.accept(".a {");
        assertNoRulesProduced.accept(".a .");
        assertNoRulesProduced.accept(".a }");
        assertNoRulesProduced.accept(".a .b");
        assertNoRulesProduced.accept(".a :");
        assertNoRulesProduced.accept(".a { .b }");
        assertNoRulesProduced.accept(".a { .b : a; }");

        var s = cssParser.parseStyleSheet(inputFromString("#rule { c : d; }"), CssHints.DEFAULT);
        var expected = new StyleRuleList(
                List.of(new StyleRule(
                        new ComplexSelector(
                                List.of(new CompoundSelector(
                                        List.of(new SimpleSelector.Id("rule")))),
                                List.of()),
                        List.of(new NormalizedProperty(
                                "c",
                                List.of(new Token.Ident("d")),
                                false)))));
        assertEquals(expected, s);

        var sheet = cssParser.parseStyleSheet(inputFromString("""
                .a .b {}
                #rule1 {
                    fill: orange;
                }
                .a {
                .a }
                #rule2 {
                    fill: orange;
                }
                .a .b
                .a : {}
                #rule3 {
                    fill: orange;
                }
                """), CssHints.DEFAULT);

        Function<String, StyleRule> styleRule = (String name) -> new StyleRule(
                new ComplexSelector(
                        List.of(new CompoundSelector(
                                List.of(new SimpleSelector.Id(name)))),
                        List.of()),
                List.of(new NormalizedProperty(
                        "fill",
                        List.of(new Token.Ident("orange")),
                        false)));
        assertEquals(new StyleRuleList(List.of(
                styleRule.apply("rule1"),
                styleRule.apply("rule2"),
                styleRule.apply("rule3"))), sheet);
    }

    @Test
    void newLinesAreSkipped() {
        FullCssParser cssParser = new FullCssParser();
        var s = cssParser.parseStyleSheet(inputFromString(".cls{\r\n \r \n \f fill:#6e6e6e}"), CssHints.DEFAULT);

        var expected = new StyleRuleList(
                List.of(new StyleRule(
                        new ComplexSelector(
                                List.of(new CompoundSelector(
                                        List.of(new SimpleSelector.Class("cls")))),
                                List.of()),
                        List.of(new NormalizedProperty(
                                "fill",
                                List.of(new Token.Hash("6e6e6e", Token.HashType.UNRESTRICTED)),
                                false)))));
        assertEquals(expected, s);
    }

    @Test
    void emptyInputSegmentsAreSkipped() {
        FullCssParser cssParser = new FullCssParser();
        var s = cssParser.parseStyleSheet(List.of(
                new char[0],
                ".cls{".toCharArray(),
                new char[0],
                "fill:#6e6e6e}".toCharArray()), CssHints.DEFAULT);

        var expected = new StyleRuleList(
                List.of(new StyleRule(
                        new ComplexSelector(
                                List.of(new CompoundSelector(
                                        List.of(new SimpleSelector.Class("cls")))),
                                List.of()),
                        List.of(new NormalizedProperty(
                                "fill",
                                List.of(new Token.Hash("6e6e6e", Token.HashType.UNRESTRICTED)),
                                false)))));
        assertEquals(expected, s);
    }

    @Test
    @Timeout(value = 10)
    void randomInput() {
        FullCssParser cssParser = new FullCssParser();
        Random r = new Random();
        for (int i = 0; i < 200; i++) {
            String[] inputStrings = RandomData.generateRandomStringArray(r, RandomData.CharType.ALPHA_NUMERIC_ONLY);
            List<char[]> input = new ArrayList<>();
            for (String inputString : inputStrings) {
                input.add(inputString.toCharArray());
            }
            String singleString = String.join("", inputStrings);

            assertDoesNotThrow(() -> {
                cssParser.parseStyleSheet(input, CssHints.DEFAULT);
            }, Arrays.toString(inputStrings));

            assertDoesNotThrow(() -> {
                cssParser.parseStyleAttribute(singleString, CssHints.DEFAULT);
            }, singleString);

            assertDoesNotThrow(() -> {
                cssParser.parseCssAttribute(singleString);
            }, singleString);

            assertDoesNotThrow(() -> {
                cssParser.parseCommaSeparatedCssAttribute(singleString);
            }, singleString);
        }
    }

    @Test
    void invalidIdentifiers() {
        FullCssParser parser = new FullCssParser();
        assertDoesNotThrow(() -> parser.parseStyleSheet(inputFromString("..{}"), CssHints.DEFAULT));
        assertDoesNotThrow(() -> parser.parseStyleSheet(inputFromString("#.{}"), CssHints.DEFAULT));
    }

    @Test
    void ruleWithoutSemicolon() {
        FullCssParser cssParser = new FullCssParser();
        var s = cssParser.parseStyleSheet(inputFromString(".cls{fill:#6e6e6e}"), CssHints.DEFAULT);

        var expected = new StyleRuleList(
                List.of(new StyleRule(
                        new ComplexSelector(
                                List.of(new CompoundSelector(
                                        List.of(new SimpleSelector.Class("cls")))),
                                List.of()),
                        List.of(new NormalizedProperty(
                                "fill",
                                List.of(new Token.Hash("6e6e6e", Token.HashType.UNRESTRICTED)),
                                false)))));
        assertEquals(expected, s);
    }

    @Test
    void precedence() {
        assertEquals(SUCCESS, compareImages("css/precedence.svg"));
    }

    @Test
    void brokenUpContent() {
        assertEquals(SUCCESS, compareImages("css/brokenUpCharContent.svg"));
    }

    @Test
    void multipleStyleSheets() {
        assertEquals(SUCCESS, compareImages("css/multipleStyleSheets.svg"));
    }

    @Test
    void selectorTypes() {
        assertEquals(SUCCESS, compareImages("css/selectorTypes.svg"));
    }

    // §4.3.14: value = s·(i + f·10^-d)·10^(t·e). A sign slip on the fractional term turned
    // 0.7 into 70, so em-sized text rendered far off-screen and appeared missing.
    @Test
    void fractionalNumbersParseCorrectly() {
        FullCssParser parser = new FullCssParser();

        assertEquals(0.7, dimensionValue(parser, "0.7em"), 1e-9);
        assertEquals(0.8, dimensionValue(parser, "0.8em"), 1e-9);
        assertEquals(1.5, dimensionValue(parser, "1.5em"), 1e-9);
        assertEquals(2.0, dimensionValue(parser, "2em"), 1e-9);
        assertEquals(10.25, dimensionValue(parser, "10.25px"), 1e-9);
        assertEquals(-0.25, dimensionValue(parser, "-0.25px"), 1e-9);

        // Scientific notation.
        assertEquals(0.015, dimensionValue(parser, "1.5e-2px"), 1e-9);
        assertEquals(150.0, dimensionValue(parser, "1.5e2px"), 1e-9);

        // Plain numbers and percentages share the same code path.
        assertEquals(0.5, numberValue(parser, "0.5"), 1e-9);
        assertEquals(12.5, percentageValue(parser, "12.5%"), 1e-9);
    }

    private static double dimensionValue(@NotNull FullCssParser parser, @NotNull String value) {
        ComponentValue token = firstToken(parser, value);
        assertTrue(token instanceof Token.Dimension, value);
        return ((Token.Dimension) token).value();
    }

    private static double numberValue(@NotNull FullCssParser parser, @NotNull String value) {
        ComponentValue token = firstToken(parser, value);
        assertTrue(token instanceof Token.Number, value);
        return ((Token.Number) token).value();
    }

    private static double percentageValue(@NotNull FullCssParser parser, @NotNull String value) {
        ComponentValue token = firstToken(parser, value);
        assertTrue(token instanceof Token.Percentage, value);
        return ((Token.Percentage) token).value();
    }

    private static @NotNull ComponentValue firstToken(@NotNull FullCssParser parser, @NotNull String value) {
        for (ComponentValue token : parser.parseCssAttribute(value)) {
            if (!(token instanceof Token) || ((Token) token).type() != TokenType.WHITESPACE) return token;
        }
        throw new AssertionError("No token produced for: " + value);
    }
}
