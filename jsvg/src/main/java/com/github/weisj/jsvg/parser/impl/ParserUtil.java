/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.parser.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.util.ParserBase;

public final class ParserUtil {

    private ParserUtil() {}

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    private static @NotNull String removeWhiteSpace(@NotNull String value) {
        return WHITESPACE_PATTERN.matcher(value).replaceAll("");
    }

    public static @Nullable String parseUrl(@Nullable String value) {
        if (value == null) return null;
        if (!value.startsWith("url(") || !value.endsWith(")")) return removeWhiteSpace(value);
        return removeWhiteSpace(value.substring(4, value.length() - 1));
    }

    public static int parseInt(@Nullable String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static float parseFloat(@Nullable String value, float fallback) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Contract("_,!null -> !null")
    public static @Nullable Length parseNumber(@Nullable String value, @Nullable Length fallback) {
        if (value == null) return fallback;
        try {
            return Unit.RAW.valueOf(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static float @NotNull [] parseFloatList(@Nullable String value) {
        String[] values = parseStringList(value, SeparatorMode.COMMA_AND_WHITESPACE, new String[0]);
        float[] ret = new float[values.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = parseFloat(values[i], 0);
        }
        return ret;
    }

    public static double @NotNull [] parseDoubleList(@Nullable String value) {
        if (value == null || value.isEmpty()) return new double[0];
        List<Double> list = new ArrayList<>();
        ParserBase base = new ParserBase(value, 0);
        while (base.hasNext()) {
            list.add(base.nextDouble());
            base.consumeWhiteSpaceOrSeparator();
        }
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public static @NotNull String @NotNull [] parseStringList(@Nullable String value, SeparatorMode separatorMode) {
        return parseStringList(value, separatorMode, new String[0]);
    }

    @Contract("_,_,!null -> !null")
    public static @NotNull String @Nullable [] parseStringList(@Nullable String value, SeparatorMode separatorMode,
            @NotNull String @Nullable [] fallback) {
        if (value == null || value.isEmpty()) return fallback;
        List<String> list = new ArrayList<>();
        int max = value.length();
        int start = 0;
        int i = 0;
        boolean inWhiteSpace = false;
        for (; i < max; i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!inWhiteSpace && separatorMode.allowWhitespace() && i - start > 0) {
                    list.add(value.substring(start, i));
                    start = i + 1;
                }
                inWhiteSpace = true;
                continue;
            }
            inWhiteSpace = false;
            if (separatorMode.separator() != 0 && c == separatorMode.separator()) {
                list.add(value.substring(start, i));
                start = i + 1;
            }
        }
        if (i - start > 0) list.add(value.substring(start, i));
        return list.toArray(new String[0]);
    }
}
