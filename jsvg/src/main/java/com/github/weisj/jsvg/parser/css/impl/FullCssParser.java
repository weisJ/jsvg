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
package com.github.weisj.jsvg.parser.css.impl;

import java.text.MessageFormat;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.DeclarationListItem;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.Rule;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.CssNormalizer;
import com.github.weisj.jsvg.renderer.CssHints;

public class FullCssParser implements CssParser {

    @Override
    public @NotNull StyleRuleList parseStyleSheet(@NotNull List<char[]> input, @NotNull CssHints hints) {
        List<Rule> parsedRules = BasicParser.parseStylesheet(BasicParserInput.fromSegments(input));
        return CssNormalizer.normalizeStyleSheet(parsedRules, hints);
    }

    @Override
    public @NotNull List<@NotNull NormalizedProperty> parseStyleAttribute(@NotNull String input,
            @NotNull CssHints hints) {
        List<@NotNull DeclarationListItem> parsedDeclarations = BasicParser.parseListOfDeclarations(
                BasicParserInput.fromString(input));
        return CssNormalizer.normalizeAttribute(parsedDeclarations);
    }

    @Override
    public @NotNull List<@NotNull ComponentValue> parseCssAttribute(@NotNull String input) {
        return BasicParser.parseListOfComponentValues(BasicParserInput.fromString(input));
    }

    @Override
    public @NotNull List<@NotNull List<@NotNull ComponentValue>> parseCommaSeparatedCssAttribute(
            @NotNull String input) {
        return BasicParser.parseCommaSeparatedListOfComponentValues(BasicParserInput.fromString(input));
    }

    private static final Logger LOGGER = LogFactory.createLogger(FullCssParser.class);

    public static void logParseEvent(@NotNull String message) {
        LOGGER.log(Logger.Level.INFO, MessageFormat.format("Invalid CSS: {0}", message));
    }
}
