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

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;

/** Component values of a CSS value, tokenized on first access since most declarations are never read. */
final class LazyComponentValues extends AbstractList<ComponentValue> {
    private final @NotNull String input;
    private List<ComponentValue> values;

    LazyComponentValues(@NotNull String input) {
        this.input = input;
    }

    private @NotNull List<ComponentValue> values() {
        if (values == null) {
            ComponentValue single = parseSingleToken(input);
            values = single != null
                    ? Collections.singletonList(single)
                    : BasicParser.parseListOfComponentValues(BasicParserInput.fromString(input));
        }
        return values;
    }

    /** Tokenizes the most common forms of values without the lexer, or returns {@code null} for any other form. */
    private static @Nullable ComponentValue parseSingleToken(@NotNull String s) {
        if (s.isEmpty()) return null;
        char first = s.charAt(0);
        if (isIdentStart(first)) {
            if (isIdentRest(s, 1)) return new Token.Ident(s);
            return parseUrl(s);
        }
        if (first == '#') {
            if (s.length() < 2 || !isIdentRest(s, 1)) return null;
            char c = s.charAt(1);
            if (c == '-') return null;
            return new Token.Hash(s.substring(1), isIdentStart(c) ? Token.HashType.ID : Token.HashType.UNRESTRICTED);
        }
        return isDigit(first) ? parseNumeric(s) : null;
    }

    private static @Nullable ComponentValue parseUrl(@NotNull String s) {
        int end = s.length() - 1;
        if (end <= 4 || !s.startsWith("url(") || s.charAt(end) != ')') return null;
        for (int i = 4; i < end; i++) {
            char c = s.charAt(i);
            if (!isIdentChar(c) && c != '#' && c != '.') return null;
        }
        return new Token.Url(s.substring(4, end));
    }

    private static @Nullable ComponentValue parseNumeric(@NotNull String s) {
        int length = s.length();
        int i = skipDigits(s, 0);
        Token.NumericType type = Token.NumericType.INTEGER;
        if (i < length && s.charAt(i) == '.') {
            int fractionEnd = skipDigits(s, i + 1);
            if (fractionEnd == i + 1) return null;
            i = fractionEnd;
            type = Token.NumericType.NUMBER;
        }
        float value = (float) Double.parseDouble(s.substring(0, i));
        if (i == length) return new Token.Number(value, type);
        if (i == length - 1 && s.charAt(i) == '%') return new Token.Percentage(value);
        for (int j = i; j < length; j++) {
            char c = s.charAt(j);
            if (!isIdentStart(c) || c == '_') return null;
        }
        return new Token.Dimension(value, type, s.substring(i));
    }

    private static int skipDigits(@NotNull String s, int from) {
        int i = from;
        while (i < s.length() && isDigit(s.charAt(i)))
            i++;
        return i;
    }

    private static boolean isIdentRest(@NotNull String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (!isIdentChar(s.charAt(i))) return false;
        }
        return true;
    }

    static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    static boolean isIdentChar(char c) {
        return isIdentStart(c) || isDigit(c) || c == '-';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    @Override
    public ComponentValue get(int index) {
        return values().get(index);
    }

    @Override
    public int size() {
        return values().size();
    }
}
