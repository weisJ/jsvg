/*
 * MIT License
 *
 * Copyright (c) 2025-2026 Jannis Weis
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Url {
    private final @NotNull String rawUrl;
    private final @Nullable String url;
    private final @Nullable String fragment;

    public Url(@NotNull String rawUrl, @Nullable String url, @Nullable String fragment) {
        this.rawUrl = rawUrl;
        this.url = url;
        this.fragment = fragment;
    }

    public enum RequireFragment {
        YES,
        NO
    }

    public static @Nullable Url parse(@Nullable String value, RequireFragment requireFragment) {
        if (value == null) return null;

        String urlString = value;
        if (urlString.startsWith("url(")) {
            if (functionEnd(urlString) != urlString.length() - 1) return null;
            urlString = urlString.substring(4, urlString.length() - 1).trim();
            if (urlString.startsWith("\"") || urlString.startsWith("'")) {
                if (urlString.length() < 2 || urlString.charAt(urlString.length() - 1) != urlString.charAt(0))
                    return null;
                urlString = urlString.substring(1, urlString.length() - 1);
            }
            urlString = unescape(urlString);
        }

        urlString = urlString.trim();
        String[] split = urlString.split("#", 2);

        if (split.length == 0) return null;
        if (requireFragment == RequireFragment.YES && split.length != 2) return null;

        String url = nullIfEmpty(split[0]);
        String fragment = nullIfEmpty(split.length == 2 ? split[1] : null);

        if (url == null && fragment == null) return null;

        return new Url(urlString, url, fragment);
    }

    // Locate the closing parenthesis without splitting quoted or escaped URL characters.
    static int functionEnd(@NotNull String value) {
        char quote = 0;
        for (int i = 4; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == ')') {
                return i;
            }
        }
        return -1;
    }

    private static @NotNull String unescape(@NotNull String value) {
        if (value.indexOf('\\') < 0) return value;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                int start = ++i;
                while (i < value.length() && i - start < 6 && Character.digit(value.charAt(i), 16) >= 0) {
                    i++;
                }
                if (i > start) {
                    int codePoint = Integer.parseInt(value.substring(start, i), 16);
                    result.appendCodePoint(codePoint == 0 || !Character.isValidCodePoint(codePoint)
                            || codePoint >= 0xd800 && codePoint <= 0xdfff ? 0xfffd : codePoint);
                    if (i < value.length() && Character.isWhitespace(value.charAt(i))) {
                        if (value.charAt(i) == '\r' && i + 1 < value.length() && value.charAt(i + 1) == '\n') i++;
                    } else {
                        i--;
                    }
                    continue;
                }
                c = value.charAt(i);
                if (c == '\r' && i + 1 < value.length() && value.charAt(i + 1) == '\n') i++;
                if (c == '\n' || c == '\r' || c == '\f') continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    private static @Nullable String nullIfEmpty(@Nullable String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    public @NotNull String rawUrl() {
        return rawUrl;
    }


    public @Nullable String url() {
        return url;
    }

    public @Nullable String fragment() {
        return fragment;
    }
}
