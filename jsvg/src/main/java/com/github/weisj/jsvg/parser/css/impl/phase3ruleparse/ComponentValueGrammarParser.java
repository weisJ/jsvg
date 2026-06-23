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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;

public class ComponentValueGrammarParser {
    private int idx = 0;
    private final @NotNull List<@NotNull ComponentValue> tokens;

    public ComponentValueGrammarParser(@NotNull List<@NotNull ComponentValue> tokens) {
        this.tokens = tokens;
    }

    @NotNull
    public ComponentValue current() {
        return tokens.get(idx);
    }

    @Nullable
    public ComponentValue peekNext() {
        return idx + 1 < tokens.size() ? tokens.get(idx + 1) : null;
    }

    public void advance() {
        idx++;
    }

    public boolean isEof() {
        return idx >= tokens.size();
    }

    public boolean isCurrentOneOfKeywords(@NotNull String... keywords) {
        if (isEof()) {
            return false;
        }
        return current().isOneOfKeywords(keywords);
    }

    public boolean isNextOfType(@NotNull TokenType type) {
        ComponentValue next = peekNext();
        return next instanceof Token && ((Token) next).type() == type;
    }

    public boolean isNextANumberInRange(int min, int max) {
        ComponentValue next = peekNext();
        return next instanceof Token.Number
                && ((Token.Number) next).value() >= min
                && ((Token.Number) next).value() <= max;
    }

    @NotNull
    public List<@NotNull ComponentValue> remainingTokens() {
        return tokens.subList(idx, tokens.size());
    }

    public static @NotNull List<@NotNull ComponentValue> stripWhitespace(
            @NotNull List<@NotNull ComponentValue> in) {
        List<ComponentValue> out = new ArrayList<>(in.size());
        for (ComponentValue v : in) {
            if (!(v instanceof Token && ((Token) v).type() == TokenType.WHITESPACE)) {
                out.add(v);
            }
        }
        return out;
    }
}
