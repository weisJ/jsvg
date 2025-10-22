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
            if (!urlString.endsWith(")")) return null;
            urlString = ParserUtil.removeWhiteSpace(urlString.substring(4, urlString.length() - 1));
        }

        String[] split = urlString.split("#", 2);

        if (split.length == 0) return null;
        if (requireFragment == RequireFragment.YES && split.length != 2) return null;

        String url = nullIfEmpty(split[0]);
        String fragment = nullIfEmpty(split.length == 2 ? split[1] : null);

        if (url == null && fragment == null) return null;

        return new Url(urlString, url, fragment);
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
