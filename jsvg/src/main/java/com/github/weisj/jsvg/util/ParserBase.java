/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.Length;

public class ParserBase {
    protected final @NotNull String input;
    private final int inputLength;
    private int index;

    public ParserBase(@NotNull String input, int startIndex) {
        this.input = input;
        this.inputLength = input.length();
        this.index = startIndex;
    }

    protected char peek() {
        return input.charAt(index);
    }

    protected void consume() {
        index++;
    }

    public boolean hasNext() {
        return index < inputLength;
    }

    public void consumeWhiteSpaceOrSeparator() {
        while (hasNext() && isWhiteSpaceOrSeparator(peek())) {
            consume();
        }
    }

    private boolean isWhiteSpaceOrSeparator(char c) {
        return c == ',' || Character.isWhitespace(c);
    }

    private String nextNumberString() {
        int start = index;
        NumberCharState state = new NumberCharState();
        while (hasNext() && isValidNumberChar(peek(), state)) {
            consume();
        }
        int end = index;
        return input.substring(start, end);
    }

    protected float nextFloatOrUnspecified() {
        if (!hasNext()) return Length.UNSPECIFIED_RAW;
        return nextFloat();
    }

    public float nextFloat() throws NumberFormatException {
        int start = index;
        String token = nextNumberString();
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException e) {
            String msg = "Unexpected token '" + token + "' rest="
                    + input.substring(start, Math.min(input.length(), start + 10))
                    + currentLocation();
            throw new IllegalStateException(msg, e);
        }
    }

    public double nextDouble() throws NumberFormatException {
        int start = index;
        String token = nextNumberString();
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            String msg = "Unexpected token '" + token + "' rest="
                    + input.substring(start, Math.min(input.length(), start + 10))
                    + currentLocation();
            throw new IllegalStateException(msg, e);
        }
    }

    protected @NotNull String currentLocation() {
        return "(index=" + index + " in input=" + input + ")";
    }

    // This only checks for the rough structure of a number as we need to know
    // when to separate the next token.
    // Explicit parsing is done by Float#parseFloat.
    private boolean isValidNumberChar(char c, NumberCharState state) {
        boolean valid = '0' <= c && c <= '9';
        if (valid && state.iteration == 1 && input.charAt(index - 1) == '0') {
            // Break up combined zeros into multiple numbers.
            return false;
        }
        state.signAllowed = state.signAllowed && !valid;
        if (state.dotAllowed && !valid) {
            valid = c == '.';
            state.dotAllowed = !valid;
        }
        if (state.signAllowed && !valid) {
            valid = c == '+' || c == '-';
            state.signAllowed = valid;
        }
        if (state.exponentAllowed && !valid) {
            // Possible exponent notation. Needs at least one preceding number
            valid = c == 'e' || c == 'E';
            state.exponentAllowed = !valid;
            state.signAllowed = valid;
            state.dotAllowed = !valid;
        }
        state.iteration++;
        return valid;
    }

    private static final class NumberCharState {
        int iteration = 0;
        boolean dotAllowed = true;
        boolean signAllowed = true;
        boolean exponentAllowed = true;
    }
}
