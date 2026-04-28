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
package com.github.weisj.jsvg.parser.css.impl.phase0;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * <a href="https://www.w3.org/TR/css-syntax-3/#input-preprocessing">CSS Syntax Module Level 3 §3.3 input preprocessing</a>.
 * <p>
 * Pulls one preprocessed code point at a time from the raw segment list:
 * <ul>
 *   <li>U+000D (CR), U+000C (FF), and U+000D U+000A (CRLF) are normalized to U+000A (LF).</li>
 *   <li>U+0000 NULL is replaced with U+FFFD REPLACEMENT CHARACTER.</li>
 *   <li>UTF-16 surrogate pairs are combined into a single supplementary code point;
 *       unpaired surrogates are replaced with U+FFFD.</li>
 * </ul>
 * The raw input is never copied — at most one segment lookahead is read to detect CRLF
 * and to pair surrogates across segment boundaries.
 */
public final class PreProcessor {

    public static final char REPLACEMENT_CHARACTER = '\uFFFD';

    /** Returned by {@link #read()} once the input is exhausted. */
    public static final int EOF = -1;

    private final @NotNull List<char[]> input;
    private int listIndex = 0;
    private int characterIndex = 0;

    public PreProcessor(@NotNull List<char[]> input) {
        this.input = input;
    }

    /** Returns the next preprocessed code point, or {@link #EOF} if the input is exhausted. */
    public int read() {
        skipEmptySegments();
        if (listIndex >= input.size()) return EOF;

        char ch = input.get(listIndex)[characterIndex];
        ++characterIndex; // advances to next character

        switch (ch) {
            case '\r':
                if (peekChar() == '\n') ++characterIndex; // consumes both characters
                // \r\n becomes \n
                return '\n';
            case '\f':
                return '\n';
            case '\0':
                return REPLACEMENT_CHARACTER;
            default:
                // two-character Unicode code points are returned as a single int
                if (Character.isHighSurrogate(ch)) {
                    char nextChar = peekChar();
                    if (nextChar != (char) EOF && Character.isLowSurrogate(nextChar)) {
                        ++characterIndex; // consumes both characters
                        return Character.toCodePoint(ch, nextChar);
                    }
                    // lonely high surrogate Unicode character (invalid)
                    return REPLACEMENT_CHARACTER;
                }
                // lonely low surrogate Unicode character (invalid)
                if (Character.isLowSurrogate(ch)) return REPLACEMENT_CHARACTER;
                return ch;
        }
    }

    private char peekChar() {
        int si = listIndex;
        int ci = characterIndex;
        // skips empty segments when peeking
        while (si < input.size() && ci >= input.get(si).length) {
            si++;
            ci = 0;
        }
        if (si >= input.size()) return (char) EOF;
        return input.get(si)[ci];
    }

    private void skipEmptySegments() {
        while (listIndex < input.size()
            && characterIndex >= input.get(listIndex).length) {
            listIndex++;
            characterIndex = 0;
        }
    }
}
