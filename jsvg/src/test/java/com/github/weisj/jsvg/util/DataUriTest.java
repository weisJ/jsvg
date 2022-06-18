/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

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
class DataUriTest {

    @Test
    public void testSimple() throws DataUri.MalformedDataUriException {
        final String test = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";
        DataUri duri = DataUri.parse(test, UTF_8);

        assertEquals("image/gif", duri.mime());
        assertNull(duri.charset());
        assertNull(duri.filename());
        assertNull(duri.contentDisposition());
        assertArrayEquals(
                new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1, -1, -1, 0, 0, 0, 33, -7, 4, 1, 0, 0, 0,
                        0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59},
                duri.data());
    }



    @Test
    public void testSimpleConstructor() {
        final String EXPECTED_MIME = "text/plain";
        final Charset EXPECTED_CHARSET = Charset.forName("ISO-8859-15");
        final byte[] EXPECTED_DATA = new byte[] {1, 2, 3};

        DataUri duri = new DataUri(EXPECTED_MIME, EXPECTED_CHARSET, EXPECTED_DATA);

        assertEquals(EXPECTED_MIME, duri.mime());
        assertEquals(EXPECTED_CHARSET, duri.charset());
        assertNull(duri.filename());
        assertNull(duri.contentDisposition());
        assertArrayEquals(EXPECTED_DATA, duri.data());
    }



    @Test
    public void testExtendedConstructor() {
        final String EXPECTED_MIME = "text/plain";
        final Charset EXPECTED_CHARSET = Charset.forName("ISO-8859-15");
        final String EXPECTED_FILENAME = "test.txt";
        final String EXPECTED_CONTENT_DISPOSITION = "inline";
        final byte[] EXPECTED_DATA = new byte[] {1, 2, 3};

        DataUri duri = new DataUri(
                EXPECTED_MIME,
                EXPECTED_CHARSET,
                EXPECTED_FILENAME,
                EXPECTED_CONTENT_DISPOSITION,
                EXPECTED_DATA);

        assertEquals(EXPECTED_MIME, duri.mime());
        assertEquals(EXPECTED_CHARSET, duri.charset());
        assertEquals(EXPECTED_FILENAME, duri.filename());
        assertEquals(EXPECTED_CONTENT_DISPOSITION, duri.contentDisposition());
        assertArrayEquals(EXPECTED_DATA, duri.data());
    }



    @Test
    public void testStartWithDataSchema() {
        assertThrows(DataUri.MalformedDataUriException.class, () -> DataUri.parse(
                "dato:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==", UTF_8));
    }



    @Test
    public void testCaseInsensitivedataSchema() throws DataUri.MalformedDataUriException {
        final String test = "DaTa:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";
        DataUri duri = DataUri.parse(test, UTF_8);

        assertEquals("image/gif", duri.mime());
        assertNull(duri.charset());
        assertNull(duri.filename());
        assertNull(duri.contentDisposition());
        assertArrayEquals(
                new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1, -1, -1, 0, 0, 0, 33, -7, 4, 1, 0, 0, 0,
                        0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59},
                duri.data());
    }



    @Test
    public void testEquals() throws DataUri.MalformedDataUriException {
        final String test = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";
        DataUri duri = DataUri.parse(test, UTF_8);

        DataUri equal = new DataUri("image/gif", null, new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1,
                -1, -1, 0, 0, 0, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59});
        DataUri notEqual = new DataUri("image/gif", null, new byte[] {72, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0,
                -1, -1, -1, 0, 0, 0, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59});

        if (!duri.equals(equal)) {
            fail("Equal returns false on equal instances");
        }
        if (duri.equals(notEqual)) {
            fail("Equal returns true on unequal instances");
        }
    }



    @Test
    public void testHashcode() throws DataUri.MalformedDataUriException {
        final String test = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";
        DataUri duri = DataUri.parse(test, UTF_8);

        DataUri equal = new DataUri("image/gif", null, new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1,
                -1, -1, 0, 0, 0, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59});
        DataUri notEqual = new DataUri("image/gif", null, new byte[] {72, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0,
                -1, -1, -1, 0, 0, 0, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59});

        if (duri.hashCode() != equal.hashCode()) {
            fail("hashCode different on equal instances");
        }
        if (duri.hashCode() == notEqual.hashCode()) {
            fail("hashCode equal on different instances, doesn't have to be an error but is highly likely");
        }
    }



    @Test
    public void testMustContainComma() {
        assertThrows(DataUri.MalformedDataUriException.class, () -> DataUri.parse(
                "DaTa:image/gif;base64;R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==", UTF_8));
    }



    @Test
    public void testOptions() throws DataUri.MalformedDataUriException {
        final String test =
                "data:image/gif;charset=utf-8;filename=test.txt;content-disposition=inline;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";
        DataUri duri = DataUri.parse(test, UTF_8);

        assertEquals("image/gif", duri.mime());
        assertEquals(UTF_8, duri.charset());
        assertEquals("test.txt", duri.filename());
        assertEquals("inline", duri.contentDisposition());
        assertArrayEquals(new byte[] {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, -1, -1, -1, 0, 0, 0, 33, -7, 4, 1,
                0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59}, duri.data());
    }



    @Test
    public void testToString() throws DataUri.MalformedDataUriException {
        final String[] testStrings = {
                "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==",
                "data:image/gif;charset=UTF-8;content-disposition=inline;filename=test.txt;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==",
                "data:text/html;base64,dGVzdA==",
                "data:text/html;charset=windows-1250;base64,dGVzdA==",
                "data:text/html;filename=test.txt;base64,dGVzdA==",
                "data:text/html;content-disposition=attachment;base64,dGVzdA==",
                "data:text/html;content-disposition=attachment;filename=t.txt;base64,dGVzdA=="
        };

        for (final String testString : testStrings) {
            assertEquals(testString, DataUri.parse(testString, UTF_8).toString());
        }
    }

    @Test
    public void testPlusCharacter() throws DataUri.MalformedDataUriException {
        // spaces shouldn't turn into pluses (see https://github.com/ooxi/jdatauri/issues/10)
        DataUri duri = DataUri.parse("data:text/plain;charset=utf-8,Hello%2C%20how%20do%20you%20do%3F", UTF_8);
        assertEquals(UTF_8, duri.charset());
        assertEquals("Hello, how do you do?", new String(duri.data(), UTF_8));

        // plus in a mime type isn't decoded to space; pluses and spaces in data are decoded correctly.
        duri = DataUri.parse("data:application/atom+xml;charset=utf-8,%3Ca%3E1%2B1%3D2%20isn%27t%20it%3F%3C%2Fa%3E",
                UTF_8);
        assertEquals(UTF_8, duri.charset());
        assertEquals("<a>1+1=2 isn't it?</a>", new String(duri.data(), UTF_8));
        assertEquals("application/atom+xml", duri.mime());
    }
}
