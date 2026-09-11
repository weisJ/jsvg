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
package com.github.weisj.jsvg.parser.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UrlTest {
    @TestFactory
    Stream<DynamicTest> cssEscapes() {
        return Stream.of(
                Map.entry("url(#grad\\69 ent)", "#gradient"),
                Map.entry("url(#\\000061b)", "#ab"),
                Map.entry("url(\"#a\\)b\")", "#a)b"),
                Map.entry("url('#a\\'b')", "#a'b"),
                Map.entry("url(#a\\ b)", "#a b"),
                Map.entry("url('#a\\\\b')", "#a\\b"),
                Map.entry("url('#a\\\nb')", "#ab"),
                Map.entry("url('#a\\\r\nb')", "#ab"),
                Map.entry("url('#a\\\fb')", "#ab"),
                Map.entry("url('#\\61\r\nb')", "#ab"),
                Map.entry("url('#\\61  b')", "#a b"),
                Map.entry("url('#\\0')", "#\ufffd"),
                Map.entry("url('#\\d800')", "#\ufffd"),
                Map.entry("url('#\\110000')", "#\ufffd"),
                Map.entry("url('#\\1f600')", "#\ud83d\ude00"))
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
                    Url parsed = Url.parse(entry.getKey(), Url.RequireFragment.YES);
                    assertNotNull(parsed);
                    assertEquals(entry.getValue(), parsed.rawUrl());
                    assertNull(parsed.url());
                    assertEquals(entry.getValue().substring(1), parsed.fragment());
                }));
    }

    @TestFactory
    Stream<DynamicTest> invalidFunctions() {
        return Stream.of("url(", "url(#a", "url('#a)", "url(\"#a')", "url(#a) extra", "url(#a\\)", "url()")
                .map(value -> DynamicTest.dynamicTest(value,
                        () -> assertNull(Url.parse(value, Url.RequireFragment.NO))));
    }

    @Test
    void externalUrlAndFragment() {
        Url parsed = Url.parse("url('file.svg#gradient')", Url.RequireFragment.YES);
        assertNotNull(parsed);
        assertEquals("file.svg", parsed.url());
        assertEquals("gradient", parsed.fragment());
        assertEquals("file.svg#gradient", parsed.rawUrl());

        Url externalOnly = Url.parse("file.svg", Url.RequireFragment.NO);
        assertNotNull(externalOnly);
        assertEquals("file.svg", externalOnly.url());
        assertNull(externalOnly.fragment());
        assertNull(Url.parse("file.svg", Url.RequireFragment.YES));
    }
}
