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

import java.util.Random;
import java.util.function.IntPredicate;

public final class RandomData {

    private RandomData() {}

    public static float[] generateRandomFloatArray(Random r) {
        int count = 5 + r.nextInt(10);
        float[] arr = new float[count];
        for (int i = 0; i < count; i++) {
            arr[i] = r.nextFloat();
        }
        return arr;
    }

    public enum CharType {
        ALL_ASCII,
        ALPHA_NUMERIC_ONLY
    }

    public static String[] generateRandomStringArray(Random r) {
        return generateRandomStringArray(r, CharType.ALPHA_NUMERIC_ONLY);
    }

    public static String[] generateRandomStringArray(Random r, CharType charType) {
        int count = 5 + r.nextInt(10);
        String[] arr = new String[count];
        for (int i = 0; i < count; i++) {
            arr[i] = generateRandomString(r, charType);
        }
        return arr;
    }

    public static String generateRandomString(Random random) {
        return generateRandomString(random, CharType.ALPHA_NUMERIC_ONLY);
    }

    public static String generateRandomString(Random random, CharType charType) {
        int targetStringLength = 10;

        IntPredicate allowedCharactersFilter;
        int leftLimit;
        int rightLimit;

        switch (charType) {
            case ALL_ASCII -> {
                leftLimit = 0;
                rightLimit = 126;
                allowedCharactersFilter = i -> i >= ' ' || Character.isWhitespace(i);
            }
            case ALPHA_NUMERIC_ONLY -> {
                leftLimit = '0';
                rightLimit = 'z';
                allowedCharactersFilter =
                        i -> ('0' <= i && i <= '9') || ('A' <= i && i <= 'Z') || ('a' <= i && i <= 'z');
            }
            default -> throw new IllegalStateException();
        };

        return random.ints(leftLimit, rightLimit + 1)
                .filter(allowedCharactersFilter)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
