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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Declaration;
import com.github.weisj.jsvg.parser.css.data.DeclarationListItem;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.Rule;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.CssNormalizer;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.ShorthandExpander;
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
        List<NormalizedProperty> simple = parseSimpleDeclarations(input);
        return simple != null ? simple : parseDeclarations(input);
    }

    private static @NotNull List<@NotNull NormalizedProperty> parseDeclarations(@NotNull String input) {
        List<@NotNull DeclarationListItem> parsedDeclarations = BasicParser.parseListOfDeclarations(
                BasicParserInput.fromString(input));
        return CssNormalizer.normalizeAttribute(parsedDeclarations);
    }

    /**
     * Splits a style attribute without tokenizing its values, or returns {@code null} if separators can't be
     * found without the full parser.
     */
    private static @Nullable List<@NotNull NormalizedProperty> parseSimpleDeclarations(@NotNull String input) {
        if (!hasOnlySimpleSeparators(input)) return null;
        List<NormalizedProperty> result = new ArrayList<>();
        int length = input.length();
        int start = 0;
        while (start < length) {
            int end = endOfDeclaration(input, start);
            if (!addSimpleDeclaration(input, start, end, result)) {
                result.addAll(parseDeclarations(input.substring(start, end)));
            }
            start = end + 1;
        }
        return result;
    }

    /** Returns {@code false} if the declaration needs the full parser. */
    private static boolean addSimpleDeclaration(@NotNull String input, int from, int to,
            @NotNull List<NormalizedProperty> result) {
        int start = skipWhitespace(input, from, to);
        int end = to;
        while (end > start && isWhitespace(input.charAt(end - 1)))
            end--;
        if (start == end) return true;

        int nameEnd = start;
        while (nameEnd < end && LazyComponentValues.isIdentChar(input.charAt(nameEnd)))
            nameEnd++;
        char first = input.charAt(start);
        boolean validStart = LazyComponentValues.isIdentStart(first)
                || (first == '-' && start + 1 < end && LazyComponentValues.isIdentStart(input.charAt(start + 1)));
        if (!validStart) return false;

        int colon = skipWhitespace(input, nameEnd, end);
        if (colon == end || input.charAt(colon) != ':') return false;

        // !important needs tokens, and only the font shorthand looks into its value during expansion.
        for (int i = colon + 1; i < end; i++) {
            if (input.charAt(i) == '!') return false;
        }
        String name = input.substring(start, nameEnd).toLowerCase(Locale.ENGLISH);
        if (name.equals("font")) return false;

        String value = input.substring(skipWhitespace(input, colon + 1, end), end);
        result.addAll(ShorthandExpander.expand(new Declaration(name, new LazyComponentValues(value), false)));
        return true;
    }

    private static int endOfDeclaration(@NotNull String input, int start) {
        char quote = 0;
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ';') {
                return i;
            }
        }
        return input.length();
    }

    private static boolean hasOnlySimpleSeparators(@NotNull String input) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' || c >= 0x80 || (c < 0x20 && !isWhitespace(c))) return false;
            if (quote != 0) {
                if (c == quote)
                    quote = 0;
                else if (c == ';' || c == '\n' || c == '\r' || c == '\f') return false;
                continue;
            }
            switch (c) {
                case '"':
                case '\'':
                    // Inside an unquoted url() a quote doesn't start a string.
                    if (depth != 0) return false;
                    quote = c;
                    break;
                case '(':
                    depth++;
                    break;
                case ')':
                    if (--depth < 0) return false;
                    break;
                case ';':
                    if (depth != 0) return false;
                    break;
                case '/':
                case '{':
                case '}':
                case '[':
                case ']':
                case '@':
                    return false;
                default:
                    break;
            }
        }
        return depth == 0 && quote == 0;
    }

    private static int skipWhitespace(@NotNull String input, int from, int to) {
        int i = from;
        while (i < to && isWhitespace(input.charAt(i)))
            i++;
        return i;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    @Override
    public @NotNull List<@NotNull ComponentValue> parseCssAttribute(@NotNull String input) {
        return new LazyComponentValues(input);
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
