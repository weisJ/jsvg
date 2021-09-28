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
package com.github.weisj.jsvg.attribute;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.AttributeParser;
import com.github.weisj.jsvg.attributes.paint.DefaultPaintParser;

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
            float[] arr = generateRandomFloatArray(r);
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
            String[] arr = generateRandomStringArray(r);
            String[] parsed = parser.parseStringList(
                    appendToList(arr, r, requireComma), requireComma);
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

    private float[] generateRandomFloatArray(Random r) {
        int count = 5 + r.nextInt(10);
        float[] arr = new float[count];
        for (int i = 0; i < count; i++) {
            arr[i] = r.nextFloat();
        }
        return arr;
    }

    private String[] generateRandomStringArray(Random r) {
        int count = 5 + r.nextInt(10);
        String[] arr = new String[count];
        for (int i = 0; i < count; i++) {
            arr[i] = generateRandomString(r);
        }
        return arr;
    }

    private String generateRandomString(Random random) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
