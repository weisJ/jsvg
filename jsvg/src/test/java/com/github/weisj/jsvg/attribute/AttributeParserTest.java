/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
package com.github.weisj.jsvg.attribute;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.AttributeParser;
import com.github.weisj.jsvg.attributes.paint.DefaultPaintParser;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import com.github.weisj.jsvg.util.RandomData;

class AttributeParserTest {

    private AttributeParser parser;

    @BeforeEach
    void setup() {
        parser = new AttributeParser(new DefaultPaintParser());
    }

    @Test
    void testStringListNoRequiredComma() {
        testStringList(false);
    }

    @Test
    void testStringListRequiredComma() {
        testStringList(true);
    }

    @Test
    void testFloatList() {
        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            float[] arr = RandomData.generateRandomFloatArray(r);
            float[] parsed = parser.parseFloatList(appendToList(box(arr), r, false));
            Assertions.assertArrayEquals(arr, parsed);
        }
    }

    private Float[] box(float[] arr) {
        Float[] boxed = new Float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            boxed[i] = arr[i];
        }
        return boxed;
    }

    private void testStringList(boolean requireComma) {
        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            String[] arr = RandomData.generateRandomStringArray(r);
            String[] parsed = parser.parseStringList(
                    appendToList(arr, r, requireComma),
                    requireComma ? SeparatorMode.COMMA_ONLY : SeparatorMode.COMMA_AND_WHITESPACE);
            Assertions.assertArrayEquals(arr, parsed);
        }
    }

    private <T> String appendToList(T[] arr, Random r, boolean requireComma) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i < arr.length - 1) {
                char separator = !requireComma && r.nextBoolean() ? ' ' : ',';
                builder.append(separator);
            }
        }
        return builder.toString();
    }
}
