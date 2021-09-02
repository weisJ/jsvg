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

    // Todo: If there is trailing whitespace and the next segment is a separate text-span we need to
    // insert a space.
    // Maybe always keep the space and return the TextSegment directly with a flag indicating whether
    // the space should
    // be "painted".
    public static char[] getAddressableCharacters(char[] ch, int offset, int length, boolean keepLeadingSpace) {
        int begin = offset;
        int end = offset + length;

        int segmentBreakCount = 0;

        // Prune leading whitespace
        while (begin < end) {
            if (isSegmentBreak(ch[begin])) {
                segmentBreakCount++;
                begin++;
            } else if (isWhitespace(ch[begin])) {
                begin++;
            } else {
                break;
            }
        }
        if (begin == end && segmentBreakCount > 1) {
            return new char[] {' '};
        }

        // Keep one whitespace char if needed
        if (keepLeadingSpace && begin > offset) {
            begin--;
            ch[begin] = ' ';
        }

        // Prune trailing whitespace
        while (begin < end && (isWhitespace(ch[end - 1]) || isSegmentBreak(ch[end - 1]))) {
            end--;
        }
        // Always keep one whitespace character if present
        boolean foundWhiteSpace = end < length + offset;
        if (foundWhiteSpace) {
            segmentBreakCount = 0;
            while (end < offset + length && isSegmentBreak(ch[end])) {
                segmentBreakCount++;
                end++;
            }
            if (segmentBreakCount > 0) {
                // We discard the last segment break.
                end--;
            } else {
                // No segment breaks. We still want to keep one whitespace char though
                ch[end] = ' ';
                end++;
            }
        }

        if (begin == end) return new char[0];

        int bufferIndex = 0;
        char[] buffer = new char[end - begin];

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
        if (whitespaceSinceSegmentBreak || isSegmentBreak(ch[begin - 1])) {
            buffer[bufferIndex++] = ' ';
        }

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
