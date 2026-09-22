/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

public final class UriUtil {
    private static final Pattern PLUS = Pattern.compile("+", Pattern.LITERAL);

    private UriUtil() {}

    /** Percent-decodes a URI component without applying form-url-encoded {@code +} semantics. */
    public static @NotNull String percentDecode(@NotNull String value, @NotNull Charset charset) {
        try {
            // URLDecoder also replaces '+' with a space. Encode it first so URI pluses are preserved.
            String escapedPluses = PLUS.matcher(value).replaceAll("%2B");
            return URLDecoder.decode(escapedPluses, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Charset `" + charset.name() + "' not supported", e);
        }
    }

    public static @NotNull URI removeFragment(@NotNull URI uri) {
        if (uri.getRawFragment() == null) return uri;
        try {
            return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to remove fragment from URI: " + uri, e);
        }
    }
}
