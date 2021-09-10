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
package com.github.weisj.jsvg;

class CharacterDataParser {
    private static final boolean DEBUG = false;

    private enum State {
        SEGMENT_START(false),
        SEGMENT_BREAK(true),
        WHITESPACE_AFTER_CHAR(true),
        WHITESPACE_AFTER_SEGMENT_BREAK(true),
        CHARACTER(false);

        private final boolean isVisualSpace;

        State(boolean isVisualSpace) {
            this.isVisualSpace = isVisualSpace;
        }
    }

    private State state = State.SEGMENT_START;
    private StringBuilder buffer = new StringBuilder();

    private char[] data;
    private int begin;
    private int end;

    public void append(char[] ch, int offset, int length) {
        if (length == 0) return;
        if (DEBUG) {
            System.out.println("Append: [" + new String(ch, offset, length).replace("\n", "\\n") + "]");
        }
        data = ch;
        begin = offset;
        end = offset + length;

        if (isSegmentBreak(data[begin])) {
            int segmentBreaks = trimLeadingWhiteSpace();
            if (state == State.SEGMENT_BREAK) segmentBreaks++;
            if (begin > offset && (segmentBreaks > 1)) {
                begin--;
                data[begin] = ' ';
                if (state == State.CHARACTER || state == State.SEGMENT_BREAK) {
                    state = State.WHITESPACE_AFTER_CHAR;
                }
            }
        }

        int segmentBreaks = trimTrailingWhiteSpace();
        if (end < offset + length) {
            data[end] = segmentBreaks > 0 ? '\n' : ' ';
            end++;
        }
        if (begin >= end) return;
        if (DEBUG) {
            System.out.println("Portion: [" + new String(ch, begin, end - begin).replace("\n", "\\n") + "]");
        }

        buffer.ensureCapacity(buffer.length() + (end - begin));
        appendData();
    }

    private void appendData() {
        int initialOffset = begin;
        while (begin < end) {
            char c = data[begin];
            boolean segmentBreak = isSegmentBreak(c);
            boolean whiteSpace = isWhitespace(c);
            if (!segmentBreak && !whiteSpace) {
                if (state == State.WHITESPACE_AFTER_CHAR
                        || (state.isVisualSpace && begin > initialOffset)) {
                    buffer.append(' ');
                }
                state = State.CHARACTER;
                buffer.append(c);
            } else if (whiteSpace) {
                switch (state) {
                    case CHARACTER:
                    case WHITESPACE_AFTER_CHAR:
                        state = State.WHITESPACE_AFTER_CHAR;
                        break;
                    case SEGMENT_BREAK:
                    case WHITESPACE_AFTER_SEGMENT_BREAK:
                        state = State.WHITESPACE_AFTER_SEGMENT_BREAK;
                        break;
                    default:
                        break;
                }
            } else {
                // c is a segment break
                // This implies we have to **append a whitespace character**
                // But we only do this if we aren't immediately followed by a closing tag.
                state = State.SEGMENT_BREAK;
            }
            begin++;
        }
    }

    public boolean canFlush(boolean dueToSegmentBreak) {
        if (state == State.SEGMENT_START) return false;
        return dueToSegmentBreak || buffer.length() > 0;
    }

    public char[] flush(boolean dueToSegmentBreak) {
        if (dueToSegmentBreak && state != State.CHARACTER) {
            // We ended on a non-character and hence have to insert a whitespace
            buffer.append(' ');
        }
        if (dueToSegmentBreak) state = State.SEGMENT_BREAK;
        char[] ch = new char[buffer.length()];
        buffer.getChars(0, ch.length, ch, 0);
        if (DEBUG) {
            System.out.println("Flush segBreak=" + dueToSegmentBreak + "[" + buffer + "]");
        }
        buffer = new StringBuilder();
        return ch;
    }

    private int trimLeadingWhiteSpace() {
        int segmentBreakCount = 0;
        while (begin < end) {
            if (isSegmentBreak(data[begin])) {
                segmentBreakCount++;
                begin++;
            } else if (isWhitespace(data[begin])) {
                begin++;
            } else {
                break;
            }
        }
        return segmentBreakCount;
    }

    private int trimTrailingWhiteSpace() {
        int segmentBreakCount = 0;
        while (begin < end) {
            if (isSegmentBreak(data[end - 1])) {
                segmentBreakCount++;
                end--;
            } else if (isWhitespace(data[end - 1])) {
                end--;
            } else {
                break;
            }
        }
        return segmentBreakCount;
    }

    private static boolean isSegmentBreak(char c) {
        return c == '\n' || c == '\r';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }
}
