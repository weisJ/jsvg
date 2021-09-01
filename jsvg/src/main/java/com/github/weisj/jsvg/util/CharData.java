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

public final class CharData {
    private CharData() {}

    public static char[] getAddressableCharacters(char[] ch, int offset, int length, boolean keepLeadingSpace) {
        int bufferIndex = 0;
        char[] buffer = new char[length];

        int begin = offset;
        int end = offset + length;
        boolean whitespaceSinceSegmentBreak = false;
        boolean encounteredVisibleChar = keepLeadingSpace;
        while (begin < end) {
            char c = ch[begin];
            boolean segmentBreak = isSegmentBreak(c);
            boolean whiteSpace = isWhitespace(c);
            if (!segmentBreak && !whiteSpace) {
                // We have a valid character.
                // First check if we need to insert a collapsed space.
                if (encounteredVisibleChar && whitespaceSinceSegmentBreak) {
                    // If we encounter a non segment-break non-whitespace character we insert
                    // a space if we have encountered one before.
                    buffer[bufferIndex++] = ' ';
                }
                encounteredVisibleChar = true;
                whitespaceSinceSegmentBreak = false;
                buffer[bufferIndex++] = c;
            } else {
                whitespaceSinceSegmentBreak = !segmentBreak;
            }
            begin++;
        }
        if (whitespaceSinceSegmentBreak) buffer[bufferIndex++] = ' ';

        char[] result = new char[bufferIndex];
        System.arraycopy(buffer, 0, result, 0, bufferIndex);
        return result;
    }

    private static boolean isSegmentBreak(char c) {
        return c == '\n' || c == '\r';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }
}
