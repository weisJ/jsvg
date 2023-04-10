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
package com.github.weisj.jsvg.parser;

import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.css.StyleProperty;
import com.github.weisj.jsvg.parser.css.impl.SimpleCssParser;
import com.github.weisj.jsvg.parser.css.impl.SimpleStyleSheet;

class CssParserTest {

    private static @NotNull List<char[]> inputFromString(@NotNull String string) {
        return List.of(string.toCharArray());
    }

    @Test
    void invalidCssProducesNoRules() {
        SimpleCssParser cssParser = new SimpleCssParser();

        Consumer<String> assertNoRulesProduced = (css) -> {
            SimpleStyleSheet sheet = cssParser.parse(inputFromString(css));
            assertEquals(0, sheet.classRules().size());
            assertEquals(0, sheet.idRules().size());
            assertEquals(0, sheet.tagNameRules().size());
        };

        assertNoRulesProduced.accept(".a .b {}");
        assertNoRulesProduced.accept(".a {");
        assertNoRulesProduced.accept(".a .");
        assertNoRulesProduced.accept(".a }");
        assertNoRulesProduced.accept(".a .b");
        assertNoRulesProduced.accept(".a :");

        var sheet = cssParser.parse(inputFromString("""
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
        assertEquals(0, sheet.classRules().size());
        assertEquals(0, sheet.tagNameRules().size());
        assertEquals(3, sheet.idRules().size());

        assertTrue(sheet.idRules().containsKey("rule1"));
        assertTrue(sheet.idRules().containsKey("rule2"));
        assertTrue(sheet.idRules().containsKey("rule3"));

        var p = List.of(new StyleProperty("fill", "orange"));

        assertEquals(p, sheet.idRules().get("rule1"));
        assertEquals(p, sheet.idRules().get("rule2"));
        assertEquals(p, sheet.idRules().get("rule3"));
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
