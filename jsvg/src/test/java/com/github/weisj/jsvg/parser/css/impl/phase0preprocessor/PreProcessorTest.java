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
package com.github.weisj.jsvg.parser.css.impl.phase0preprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class PreProcessorTest {

    private static @NotNull String preprocess(@NotNull String... segments) {
        List<char[]> input = new ArrayList<>();
        for (String segment : segments) {
            input.add(segment.toCharArray());
        }
        PreProcessor preProcessor = new PreProcessor(() -> input);
        StringBuilder out = new StringBuilder();
        for (int c = preProcessor.read(); c != PreProcessor.EOF; c = preProcessor.read()) {
            out.appendCodePoint(c);
        }
        return out.toString();
    }

    @Test
    void newlinesAreNormalized() {
        assertEquals("a\nb\nc\nd", preprocess("a\r\nb\rc\fd"));
        assertEquals("\n\n", preprocess("\r\n\r\n"));
    }

    @Test
    void crlfSplitAcrossSegments() {
        assertEquals("a\nb", preprocess("a\r", "\nb"));
        // Empty segments in between must be skipped as well.
        assertEquals("a\nb", preprocess("a\r", "", "", "\nb"));
        // A lone CR at a segment end followed by something else is still just one newline.
        assertEquals("a\nb", preprocess("a\r", "b"));
    }

    @Test
    void surrogatePairSplitAcrossSegments() {
        String emoji = new String(Character.toChars(0x1F600));
        assertEquals(emoji, preprocess(emoji));
        assertEquals(emoji, preprocess(emoji.substring(0, 1), emoji.substring(1)));
        assertEquals(emoji, preprocess(emoji.substring(0, 1), "", emoji.substring(1)));
        assertEquals("x" + emoji + "y", preprocess("x" + emoji.substring(0, 1), emoji.substring(1) + "y"));
    }

    @Test
    void unpairedSurrogatesAndNullAreReplaced() {
        String r = String.valueOf(PreProcessor.REPLACEMENT_CHARACTER);
        assertEquals("a" + r + "b", preprocess("a\uD83Db"));
        assertEquals("a" + r + "b", preprocess("a\uDE00b"));
        assertEquals("a" + r, preprocess("a\uD83D"));
        assertEquals(r + r, preprocess("\uD83D", "\uD83D"));
        assertEquals("a" + r + "b", preprocess("a\0b"));
    }

    @Test
    void emptyInput() {
        assertEquals("", preprocess());
        assertEquals("", preprocess("", ""));
        assertEquals("ab", preprocess("", "a", "", "b", ""));
    }
}
