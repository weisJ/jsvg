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
package com.github.weisj.jsvg.parser.css.impl.phase3ruleparse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.renderer.CssHints;

class MediaAtRuleConditionEvaluatorTest {

    private static final CssHints LIGHT_SCREEN = CssHints.DEFAULT;
    private static final CssHints DARK_PRINT = new CssHints.Builder()
            .mediaType(CssHints.MediaType.PRINT)
            .colorScheme(CssHints.ColorScheme.DARK)
            .build();

    private static boolean matches(@NotNull String prelude, @NotNull CssHints hints) {
        return MediaAtRuleConditionEvaluator.matches(
                BasicParser.parseListOfComponentValues(BasicParserInput.fromString(prelude)), hints);
    }

    @Test
    void mediaTypes() {
        assertTrue(matches("", LIGHT_SCREEN));
        assertTrue(matches("all", LIGHT_SCREEN));
        assertTrue(matches("screen", LIGHT_SCREEN));
        assertFalse(matches("print", LIGHT_SCREEN));
        assertTrue(matches("print", DARK_PRINT));
        assertFalse(matches("tv", LIGHT_SCREEN));
        assertTrue(matches("only screen", LIGHT_SCREEN));
        assertTrue(matches("not print", LIGHT_SCREEN));
        assertFalse(matches("not screen", LIGHT_SCREEN));
        assertTrue(matches("print, screen", LIGHT_SCREEN));
        assertFalse(matches("print, tv", LIGHT_SCREEN));
    }

    @Test
    void prefersColorScheme() {
        assertTrue(matches("(prefers-color-scheme: light)", LIGHT_SCREEN));
        assertFalse(matches("(prefers-color-scheme: dark)", LIGHT_SCREEN));
        assertTrue(matches("(prefers-color-scheme: dark)", DARK_PRINT));
        assertTrue(matches("(prefers-color-scheme)", LIGHT_SCREEN));
        assertTrue(matches("screen and (prefers-color-scheme: light)", LIGHT_SCREEN));
        assertFalse(matches("screen and (prefers-color-scheme: dark)", LIGHT_SCREEN));
        assertTrue(matches("(prefers-color-scheme: dark) or (prefers-color-scheme: light)", LIGHT_SCREEN));
    }

    @Test
    void negatedConditions() {
        // <media-condition> = not <media-in-parens>
        assertTrue(matches("not (prefers-color-scheme: dark)", LIGHT_SCREEN));
        assertFalse(matches("not (prefers-color-scheme: light)", LIGHT_SCREEN));
        assertFalse(matches("not (prefers-color-scheme: dark)", DARK_PRINT));
        // ... after a media type, via <media-condition-without-or>
        assertTrue(matches("screen and not (prefers-color-scheme: dark)", LIGHT_SCREEN));
        assertFalse(matches("screen and not (prefers-color-scheme: light)", LIGHT_SCREEN));
        // ... nested inside parentheses
        assertTrue(matches("(not (prefers-color-scheme: dark))", LIGHT_SCREEN));
        assertTrue(matches("(not (prefers-color-scheme: dark)) and (prefers-color-scheme: light)", LIGHT_SCREEN));
        // not (unknown) stays unknown, which never matches
        assertFalse(matches("not (unsupported-feature: 3)", LIGHT_SCREEN));
        assertFalse(matches("not (prefers-color-scheme: dark) or (prefers-color-scheme: dark)", LIGHT_SCREEN));
        // double negation without parentheses is malformed
        assertFalse(matches("not not (prefers-color-scheme: dark)", LIGHT_SCREEN));
    }
}
