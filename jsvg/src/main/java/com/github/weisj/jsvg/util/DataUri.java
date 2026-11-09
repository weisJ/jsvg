/*
 * MIT License
 *
 * Copyright (c) 2013-2026 Jannis Weis
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright (c) 2013 ooxi
 *     <a href="https://github.com/ooxi/jdatauri">...</a>
 * <p>
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 * <p>
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * <p>
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software in a
 *     product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * <p>
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * <p>
 *  3. This notice may not be removed or altered from any source distribution.
 * <p>
 *  Note: This file has been modified for usage in the JSVG project.
 */
final class DataUri {
    private static final String DATA_SCHEME = "data:";
    private static final String CHARSET_OPTION_NAME = "charset";
    private static final String FILENAME_OPTION_NAME = "filename";
    private static final String CONTENT_DISPOSITION_OPTION_NAME = "content-disposition";
    private static final Collection<String> SUPPORTED_CONTENT_ENCODINGS = Collections.singletonList("base64");

    private final @NotNull String mime;
    private final @Nullable Charset charset;
    private final @Nullable String filename;
    private final @Nullable String contentDisposition;
    private final byte @NotNull [] data;

    public DataUri(String mime, Charset charset, byte[] data) {
        this(mime, charset, null, null, data);
    }

    public DataUri(@NotNull String mime, @Nullable Charset charset, @Nullable String filename,
            @Nullable String contentDisposition, byte @NotNull [] data) {
        this.mime = mime;
        this.charset = charset;
        this.filename = filename;
        this.contentDisposition = contentDisposition;
        this.data = data;
    }

    public @NotNull String mime() {
        return mime;
    }

    public byte @NotNull [] data() {
        return data;
    }

    public @Nullable Charset charset() {
        return charset;
    }

    public @Nullable String contentDisposition() {
        return contentDisposition;
    }

    public @Nullable String filename() {
        return filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataUri)) return false;
        DataUri dataUri = (DataUri) o;
        return mime.equals(dataUri.mime)
                && Objects.equals(charset, dataUri.charset)
                && Objects.equals(filename, dataUri.filename)
                && Objects.equals(contentDisposition, dataUri.contentDisposition)
                && Arrays.equals(data, dataUri.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mime, charset, filename, contentDisposition);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    static final class MalformedDataUriException extends IOException {
        MalformedDataUriException(@NotNull String reason) {
            super(reason);
        }

        MalformedDataUriException(@NotNull Exception reason) {
            super(reason);
        }
    }

    private static final class HeaderParseState {
        private String mimeType = "text/plain";
        private String contentEncoding = "";
        private boolean contentEncodingAlreadySet;
        private final Map<String, String> supportedValues = new HashMap<>();
        private final Set<String> supportedValueSetBits = new HashSet<>();

        private HeaderParseState() {
            supportedValues.put(CHARSET_OPTION_NAME, "");
            supportedValues.put(FILENAME_OPTION_NAME, "");
            supportedValues.put(CONTENT_DISPOSITION_OPTION_NAME, "");
        }
    }

    /**
     * Tries to parse a data URI described in RFC2397
     *
     * @param uri A string representing the data URI
     * @param charset Charset to use when decoding percent encoded options
     *     like filename
     *
     * @return Parsed data URI
     * @throws IllegalArgumentException iff an error occurred during parse
     *     process
     */
    public static DataUri parse(@NotNull String uri, Charset charset) throws MalformedDataUriException {
        int comma = headerEnd(uri);
        HeaderParseState headerState = parseHeaders(uri.substring(DATA_SCHEME.length(), comma), charset);

        // Let data be the substring of URI from position comma + 1 to the end of URI.
        String data = uri.substring(comma + 1);

        // Let data be the result of percent-decoding data.
        data = UriUtil.percentDecode(data, charset);

        // Let dataURIObject be an object consisting of the mimeType,
        // contentEncoding, data and supportedValues objects.
        final String finalMimeType = headerState.mimeType;
        final Charset finalCharset = headerState.supportedValues.get(CHARSET_OPTION_NAME).isEmpty()
                ? null
                : Charset.forName(headerState.supportedValues.get(CHARSET_OPTION_NAME));
        final String finalFilename = headerState.supportedValues.get(FILENAME_OPTION_NAME).isEmpty()
                ? null
                : headerState.supportedValues.get(FILENAME_OPTION_NAME);
        final String finalContentDisposition =
                headerState.supportedValues.get(CONTENT_DISPOSITION_OPTION_NAME).isEmpty()
                        ? null
                        : headerState.supportedValues.get(CONTENT_DISPOSITION_OPTION_NAME);

        final byte[] finalData;
        try {
            finalData = "base64".equalsIgnoreCase(headerState.contentEncoding)
                    ? Base64.getMimeDecoder().decode(data)
                    : data.getBytes(charset);
        } catch (RuntimeException e) {
            throw new MalformedDataUriException(e);
        }

        // return dataURIObject.
        return new DataUri(
                finalMimeType,
                finalCharset,
                finalFilename,
                finalContentDisposition,
                finalData);
    }

    private static int headerEnd(@NotNull String uri) throws MalformedDataUriException {
        // If URI does not start with a case-insensitive "data:": Throw a MALFORMED_URI exception.
        if (!uri.toLowerCase(Locale.ENGLISH).startsWith(DATA_SCHEME))
            throw new MalformedDataUriException("URI must start with a case-insensitive `data:'");

        // If URI does not contain a ",": Throw a MALFORMED_URI exception.
        int comma = uri.indexOf(',');
        if (-1 == comma) throw new MalformedDataUriException("URI must contain a `,'");
        return comma;
    }

    private static @NotNull HeaderParseState parseHeaders(@NotNull String headerString, @NotNull Charset charset) {
        HeaderParseState state = new HeaderParseState();
        String[] headers = headerString.split(";");
        for (int header = 0; header < headers.length; ++header) {
            parseHeader(headers[header], header, charset, state);
        }
        return state;
    }

    private static void parseHeader(@NotNull String rawHeader, int index, @NotNull Charset charset,
            @NotNull HeaderParseState state) {
        String header = rawHeader.toLowerCase(Locale.ENGLISH);
        int eq = header.indexOf('=');
        String name = decodeHeaderName(header, eq, charset);

        if ((0 == index) && (-1 == eq) && !name.isEmpty()) {
            state.mimeType = name;
        } else if (-1 == eq) {
            applyContentEncoding(name, state);
        } else {
            applySupportedValue(name, decodeHeaderValue(header, eq, charset), state);
        }
    }

    private static @NotNull String decodeHeaderName(@NotNull String header, int eq, @NotNull Charset charset) {
        String name = eq == -1 ? header : header.substring(0, eq);
        return UriUtil.percentDecode(name, charset).trim();
    }

    private static @NotNull String decodeHeaderValue(@NotNull String header, int eq, @NotNull Charset charset) {
        return UriUtil.percentDecode(header.substring(eq + 1), charset).trim();
    }

    private static void applyContentEncoding(@NotNull String name, @NotNull HeaderParseState state) {
        String nameCaseInsensitive = name.toLowerCase(Locale.ENGLISH);
        if (SUPPORTED_CONTENT_ENCODINGS.contains(nameCaseInsensitive) && !state.contentEncodingAlreadySet) {
            state.contentEncoding = name;
            state.contentEncodingAlreadySet = true;
        }
    }

    private static void applySupportedValue(@NotNull String name, @NotNull String value,
            @NotNull HeaderParseState state) {
        String nameCaseInsensitive = name.toLowerCase(Locale.ENGLISH);
        if (!value.isEmpty()
                && state.supportedValues.containsKey(nameCaseInsensitive)
                && !state.supportedValueSetBits.contains(nameCaseInsensitive)) {
            state.supportedValues.put(nameCaseInsensitive, value);
            state.supportedValueSetBits.add(nameCaseInsensitive);
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(DATA_SCHEME).append(this.mime()).append(";");

        if (this.charset != null) s.append(CHARSET_OPTION_NAME + "=").append(this.charset.name()).append(";");
        if (this.contentDisposition != null)
            s.append(CONTENT_DISPOSITION_OPTION_NAME + "=").append(this.contentDisposition).append(";");
        if (this.filename != null) s.append(FILENAME_OPTION_NAME + "=").append(this.filename).append(";");

        s.append("base64,").append(new String(Base64.getEncoder().encode(this.data()), StandardCharsets.UTF_8));

        return s.toString();
    }

}
