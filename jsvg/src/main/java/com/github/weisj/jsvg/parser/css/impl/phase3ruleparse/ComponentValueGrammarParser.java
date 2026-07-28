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

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;

public class ComponentValueGrammarParser {
    private final @NotNull List<@NotNull ComponentValue> tokens;
    private final boolean skipWhitespace;
    private int idx;
    // Caches the result of peekNext() for usage in advance(), to avoid skipping over whitespace twice.
    // Set to -1 to indicate that peekNext() has not been called yet in this position.
    private int cachedNextIdx = -1;

    /** @param skipWhitespace hides whitespace from the grammar; it stays in {@link #remainingTokens()}. */
    public ComponentValueGrammarParser(@NotNull List<@NotNull ComponentValue> tokens, boolean skipWhitespace) {
        this.tokens = tokens;
        this.skipWhitespace = skipWhitespace;
        this.idx = firstValidIndexFrom(0);
    }

    /** Returns the first index of a non-whitespace token if skipWhitespace is true, otherwise returns the given index. */
    private int firstValidIndexFrom(int from) {
        int i = from;
        if (skipWhitespace) {
            while (i < tokens.size() && tokens.get(i) == Token.Static.WHITESPACE) {
                i++;
            }
        }
        return i;
    }

    @NotNull
    public ComponentValue current() {
        return tokens.get(idx);
    }

    @Nullable
    public ComponentValue peekNext() {
        if (cachedNextIdx == -1) { // if peekNext() was not already called in this position
            cachedNextIdx = firstValidIndexFrom(idx + 1);
        }
        return cachedNextIdx < tokens.size() ? tokens.get(cachedNextIdx) : null;
    }

    public void advance() {
        idx = firstValidIndexFrom(idx + 1);
        cachedNextIdx = -1; // indicates that peekNext() has not been called yet in the new position
    }

    public boolean isEof() {
        return idx >= tokens.size();
    }

    public boolean isCurrentOneOfKeywords(@NotNull String... keywords) {
        if (isEof()) return false;
        return current().isOneOfKeywords(keywords);
    }

    /** Includes whitespaces, even if skipWhitespace is set */
    @NotNull
    public List<@NotNull ComponentValue> remainingTokens() {
        return tokens.subList(idx, tokens.size());
    }

    public static @NotNull List<@NotNull ComponentValue> stripWhitespace(
            @NotNull List<@NotNull ComponentValue> in) {
        return in.stream()
                .filter(v -> v != Token.Static.WHITESPACE)
                .collect(Collectors.toList());
    }
}
