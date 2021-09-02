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


import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CharacterDataParserTest {

    @Test
    void checkDuplicateWhitespace() {
        assertAddressableChars("   A B C  \nD", "A B C D", false);
        assertAddressableChars("   A B   C  \n   D", "A B C D", false);
        assertAddressableChars("   A\nB   C  \nD", "A B C D", false);

        assertAddressableChars("   A B C  \n   D", "A B C D", true);
        assertAddressableChars("   A B   C  \n   D", "A B C D", true);
        assertAddressableChars("   A\nB   C  \nD", "A B C D", true);
        assertAddressableChars("A\nB   C  \n   D", "A B C D", true);

        assertAddressableChars("   A B C  \n   D\n", "A B C D ", true);
        assertAddressableChars("   A B   C  \n   D\n ", "A B C D ", true);
        assertAddressableChars("   A\nB   C  \nD\n  ", "A B C D ", true);
        assertAddressableChars("A\nB   C  \n   D\n   ", "A B C D ", true);

        assertAddressableChars("Hello\n    World\n !\n  ( )", "Hello World ! ( )", false);
        assertAddressableChars("A\n B", "A B", false);
        assertAddressableChars("ABCDEFGHIJKLMNOPQRSTUVW", "ABCDEFGHIJKLMNOPQRSTUVW", false);
    }

    @Test
    void checkEmptyString() {
        assertAddressableChars(" ", "", false);
        assertAddressableChars("\n ", "", false);
        assertAddressableChars(" \n", "", false);
        assertAddressableChars("\n            ", "", false);
        assertAddressableChars("\n   \n\n         ", "", false);
        assertAddressableChars("\n   B\n\n         ", "B", false);

        assertAddressableChars(" ", " ", true);
        assertAddressableChars("\n ", " ", true);
        assertAddressableChars(" \n", " ", true);
        assertAddressableChars("\n            ", " ", true);
        assertAddressableChars("\n   \n\n         ", " ", true);
        assertAddressableChars("\n   B\n\n         ", "B ", true);
    }

    @Test
    void checkTrailingSpace() {
        assertAddressableChars("A ", "A", false);
        assertAddressableChars("A ", "A ", true);
    }

    private void assertAddressableChars(String input, String output, boolean partialFlush) {
        CharacterDataParser parser = new CharacterDataParser();
        parser.append(input.toCharArray(), 0, input.length());
        char[] expected = output.toCharArray();
        char[] got = parser.flush(partialFlush);
        Assertions.assertArrayEquals(expected, got,
                "Expected: " + Arrays.toString(expected) + " but got " + Arrays.toString(got));
    }
}
