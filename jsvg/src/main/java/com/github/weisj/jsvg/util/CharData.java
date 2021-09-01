/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
package com.github.weisj.jsvg.util;

import java.util.Arrays;

public final class CharData {
    private CharData() {}

    public static char[] pruneWhiteSpace(char[] ch, int start, int length) {
        // Fixme: Is this unicode compliant?
        // Prune off leading/trailing whitespace.
        if (length == 1) {
            if (Character.isWhitespace(ch[start])) return new char[0];
        } else {
            boolean startsWithWhiteSpace = Character.isWhitespace(ch[start]);
            while (length > 0 && Character.isWhitespace(ch[start])) {
                ch[start] = Character.SPACE_SEPARATOR;
                start++;
                length--;
            }
            boolean endsWithWhiteSpace = Character.isWhitespace(ch[start + length - 1]);
            while (length >= 0 && Character.isWhitespace(ch[start + length - 1])) {
                ch[start + length - 1] = Character.SPACE_SEPARATOR;
                length--;
            }
            // Preserve at least one character of leading/trailing whitespace
            if (startsWithWhiteSpace) {
                start--;
                length++;
            }
            if (endsWithWhiteSpace) {
                length++;
            }
            if (length == 0) return new char[0];
            if (length == 1 && startsWithWhiteSpace) return new char[0];
        }
        return Arrays.copyOfRange(ch, start, start + length);
    }
}
