/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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

import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.weisj.jsvg.parser.css.StyleProperty;
import com.github.weisj.jsvg.util.RandomData;

class CssParserTest {

    private static @NotNull List<char[]> inputFromString(@NotNull String string) {
        return List.of(string.toCharArray());
    }

    @Test
    void invalidCssProducesNoRules() {
        CssParserImpl cssParser = new CssParserImpl();

        Consumer<String> assertNoRulesProduced = css -> {
            StyleSheetImpl sheet = (StyleSheetImpl) cssParser.parse(inputFromString(css));
            assertEquals(0, sheet.rules().size());
        };

        assertNoRulesProduced.accept(".a .b {}");
        assertNoRulesProduced.accept(".a {");
        assertNoRulesProduced.accept(".a .");
        assertNoRulesProduced.accept(".a }");
        assertNoRulesProduced.accept(".a .b");
        assertNoRulesProduced.accept(".a :");
        assertNoRulesProduced.accept(".a { .b }");
        assertNoRulesProduced.accept("#a.b");

        var s = (StyleSheetImpl) cssParser.parse(inputFromString("#rule { c : d; }"));
        var rule = s.rules().stream().filter(r -> {
            List<@NotNull SelectorPart> selectorParts = r.selectorParts();
            return selectorParts.size() == 1 && selectorParts.get(0).value().equals("#rule");
        }).findAny();
        assertTrue(rule.isPresent());
        assertEquals(Set.of("c"), rule.get().properties().keySet());
        assertEquals(new StyleProperty("c", "d"), rule.get().properties().get("c"));

        var sheet = (StyleSheetImpl) cssParser.parse(inputFromString("""
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
                """));
        assertEquals(3, sheet.rules().size());
        assertEquals(
                Set.of("#rule1", "#rule2", "#rule3"),
                sheet.rules().stream()
                        .map(CssRule::selectorString)
                        .collect(Collectors.toSet()));
        var p = List.of(new StyleProperty("fill", "orange"));
        sheet.rules().forEach(r -> assertEquals(p, r.properties().values().stream().toList()));
    }

    @Test
    void ruleWithoutSemicolon() {
        CssParserImpl cssParser = new CssParserImpl();
        var s = (StyleSheetImpl) cssParser.parse(inputFromString(".cls{fill:#6e6e6e}"));
        assertEquals(1, s.rules().size());
        assertEquals(".cls", s.rules().get(0).selectorString());
        assertEquals(
                List.of(new StyleProperty("fill", "#6e6e6e")),
                s.rules().get(0).properties().values().stream().toList());
    }

    @Test
    @Timeout(value = 10)
    void randomInput() {
        CssParserImpl cssParser = new CssParserImpl();
        Random r = new Random();
        for (int i = 0; i < 200; i++) {
            String[] inputStrings = RandomData.generateRandomStringArray(r, RandomData.CharType.ALL_ASCII);
            List<char[]> input = new ArrayList<>();
            for (String inputString : inputStrings) {
                input.add(inputString.toCharArray());
            }
            assertDoesNotThrow(() -> {
                cssParser.parse(input);
            }, () -> Arrays.toString(inputStrings));
        }
    }

    @Test
    void invalidIdentifiers() {
        CssParserImpl parser = new CssParserImpl();
        assertDoesNotThrow(() -> parser.parse(inputFromString("..{}")));
        assertDoesNotThrow(() -> parser.parse(inputFromString("#.{}")));
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
}
