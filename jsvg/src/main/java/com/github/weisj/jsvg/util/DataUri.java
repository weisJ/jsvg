/*
 * MIT License
 *
 * Copyright (c) 2013-2023 Jannis Weis
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copyright (c) 2013 ooxi
 *     https://github.com/ooxi/jdatauri
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software in a
 *     product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source distribution.
 *
 *  Note: This file has been modified for usage in the JSVG project.
 */
final class DataUri {
    private static final String CHARSET_OPTION_NAME = "charset";
    private static final String FILENAME_OPTION_NAME = "filename";
    private static final String CONTENT_DISPOSITION_OPTION_NAME = "content-disposition";

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

    static class MalformedDataUriException extends IOException {
        MalformedDataUriException(@NotNull String reason) {
            super(reason);
        }

        MalformedDataUriException(@NotNull Exception reason) {
            super(reason);
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

        // If URI does not start with a case-insensitive "data:": Throw a MALFORMED_URI exception.
        if (!uri.toLowerCase().startsWith("data:"))
            throw new MalformedDataUriException("URI must start with a case-insensitive `data:'");

        // If URI does not contain a ",": Throw a MALFORMED_URI exception.
        if (-1 == uri.indexOf(',')) throw new MalformedDataUriException("URI must contain a `,'");

        // Let supportedContentEncodings be an array of strings
        // representing the supported content encodings. (["base64"] for example)
        Collection<String> supportedContentEncodings = Collections.singletonList("base64");

        // Let mimeType be a string with the value "text/plain".
        String mimeType = "text/plain";

        // Let contentEncoding be an empy string.
        String contentEncoding = "";

        /*
         * Let contentEncodingAlreadySet be a boolean with a value of false.
         */
        boolean contentEncodingAlreadySet = false;

        // Let supportedValues be a map of string:string pairs where the
        // first string in each pair represents the name of the
        // supported value and the second string in each pair represents
        // an empty string or default string value.
        // (Example: {"charset" : "", "filename" : "", "content-disposition" : ""})
        final Map<String, String> supportedValues = new HashMap<>();
        supportedValues.put(CHARSET_OPTION_NAME, "");
        supportedValues.put(FILENAME_OPTION_NAME, "");
        supportedValues.put(CONTENT_DISPOSITION_OPTION_NAME, "");

        // Let supportedValueSetBits be a map of string:bool pairs
        // representing each of the names in supportedValues with each
        // name set to false.
        final Map<String, Boolean> supportedValueSetBits = new HashMap<>();
        for (String key : supportedValues.keySet()) {
            supportedValueSetBits.put(key, false);
        }

        // Let comma be the position of the first "," found in URI.
        int comma = uri.indexOf(',');

        // Let temp be the substring of URI from, and including,
        // position 5 to, and excluding, the comma position. (between "data:" and first ",")
        String temp = uri.substring("data:".length(), comma);

        // Let headers be an array of strings returned by splitting temp by ";".
        String[] headers = temp.split(";");

        // For each string s in headers:
        for (int header = 0; header < headers.length; ++header) {
            String s = headers[header];

            // Let s equal the lowercase version of s
            s = s.toLowerCase();

            // Let eq be the position result of searching for "=" in s.
            int eq = s.indexOf('=');

            // Let name and value be empty strings.
            String name;
            String value = "";

            // If eq is not a valid position in s:
            if (-1 == eq) {

                // Let name equal the result of percent-decoding s.
                name = percentDecode(s, charset);

                // Let name equal the result of trimming leading and trailing white-space from name.
                name = name.trim();

                // Else:
            } else {

                // Let name equal the substring of s from position 0 to, but not including, position eq.
                name = s.substring(0, eq);

                // Let name equal the result of percent-decoding name.
                name = percentDecode(name, charset);

                // Let name equal the result of trimming leading and trailing white-space from name.
                name = name.trim();

                // Let value equal the substring of s from position eq + 1 to the end of s.
                value = s.substring(eq + 1);

                // Let value equal the result of percent-decoding value.
                value = percentDecode(value, charset);

                // Let value equal the result of trimming leading and trailing white-space from value.
                value = value.trim();
            }

            // If s is the first element in headers and eq is not a valid position in s and the length
            // of name is greater than 0:
            if ((0 == header) && (-1 == eq) && !name.isEmpty()) {

                // Let mimeType equal name.
                mimeType = name;

                // Else:
            } else {

                // If eq is not a valid position in s:
                if (-1 == eq) {

                    // If name is found case-insensitively in supportedContentEncodings:
                    final String nameCaseInsensitive = name.toLowerCase();

                    if (supportedContentEncodings.contains(nameCaseInsensitive)) {

                        // If contentEncodingAlreadySet is false:
                        if (!contentEncodingAlreadySet) {

                            // Let contentEncoding equal name.
                            contentEncoding = name;

                            // Let contentEncodingAlreadySet equal true.
                            contentEncodingAlreadySet = true;
                        }
                    }

                    // Else:
                } else {

                    // If the length of value is greater than 0 and name is found case-insensitively
                    // in supportedValues:
                    final String nameCaseInsensitive = name.toLowerCase();

                    if (!value.isEmpty() && supportedValues.containsKey(nameCaseInsensitive)) {

                        // If the corresponding value for name found (case-insensitivley) in
                        // supportedValueSetBits is false:
                        boolean valueSet = supportedValueSetBits.get(nameCaseInsensitive);

                        if (!valueSet) {

                            // Let the corresponding value for name found (case-insensitively)
                            // in supportedValues equal value.
                            supportedValues.put(nameCaseInsensitive, value);

                            // Let the corresponding value for name found (case-insensitively)
                            // in supportedValueSetBits equal true.
                            supportedValueSetBits.put(nameCaseInsensitive, true);
                        }
                    }
                }
            }

        }

        // Let data be the substring of URI from position comma + 1 to the end of URI.
        String data = uri.substring(comma + 1);

        // Let data be the result of percent-decoding data.
        data = percentDecode(data, charset);

        // Let dataURIObject be an object consisting of the mimeType,
        // contentEncoding, data and supportedValues objects.
        final String finalMimeType = mimeType;
        final Charset finalCharset = supportedValues.get(CHARSET_OPTION_NAME).isEmpty()
                ? null
                : Charset.forName(supportedValues.get(CHARSET_OPTION_NAME));
        final String finalFilename = supportedValues.get(FILENAME_OPTION_NAME).isEmpty()
                ? null
                : supportedValues.get(FILENAME_OPTION_NAME);
        final String finalContentDisposition = supportedValues.get(CONTENT_DISPOSITION_OPTION_NAME).isEmpty()
                ? null
                : supportedValues.get(CONTENT_DISPOSITION_OPTION_NAME);

        final byte[] finalData;
        try {
            finalData = "base64".equalsIgnoreCase(contentEncoding)
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

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("data:").append(this.mime()).append(";");

        if (this.charset != null) s.append(CHARSET_OPTION_NAME + "=").append(this.charset.name()).append(";");
        if (this.contentDisposition != null)
            s.append(CONTENT_DISPOSITION_OPTION_NAME + "=").append(this.contentDisposition).append(";");
        if (this.filename != null) s.append(FILENAME_OPTION_NAME + "=").append(this.filename).append(";");

        s.append("base64,").append(new String(Base64.getEncoder().encode(this.data()), StandardCharsets.UTF_8));

        return s.toString();
    }

    private static final Pattern PLUS = Pattern.compile("+", Pattern.LITERAL);

    private static String percentDecode(String s, Charset cs) {
        try {
            // We only need to decode %hh escape sequences, while
            // URLDecoder.decode in addition to that also replaces '+' with space.
            // As a workaround we first replace all pluses with %2B sequence,
            // so that they are preserved after decoding.
            s = PLUS.matcher(s).replaceAll("%2B");

            return URLDecoder.decode(s, cs.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Charset `" + cs.name() + "' not supported", e);
        }
    }
}
