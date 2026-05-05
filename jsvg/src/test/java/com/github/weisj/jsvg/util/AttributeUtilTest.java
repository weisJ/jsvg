/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttributeUtilTest {

    // -------------------------------------------------------------------------
    // isBlank
    // -------------------------------------------------------------------------

    @Test
    void testIsBlankEmpty() {
        assertTrue(AttributeUtil.isBlank(""));
    }

    @Test
    void testIsBlankWhitespaceOnly() {
        assertTrue(AttributeUtil.isBlank(" "));
        assertTrue(AttributeUtil.isBlank("   "));
        assertTrue(AttributeUtil.isBlank("\t"));
        assertTrue(AttributeUtil.isBlank("\n"));
        assertTrue(AttributeUtil.isBlank("  \t\n  "));
    }

    @Test
    void testIsBlankNonBlank() {
        assertFalse(AttributeUtil.isBlank("a"));
        assertFalse(AttributeUtil.isBlank("  a"));
        assertFalse(AttributeUtil.isBlank("a  "));
        assertFalse(AttributeUtil.isBlank("  a  "));
        assertFalse(AttributeUtil.isBlank("hello"));
    }

    // -------------------------------------------------------------------------
    // toNonnullArray
    // -------------------------------------------------------------------------

    @Test
    void testToNonnullArrayAllNonNull() {
        String[] result = AttributeUtil.toNonnullArray("a", "b", "c");
        assertNotNull(result);
        assertArrayEquals(new String[] {"a", "b", "c"}, result);
    }

    @Test
    void testToNonnullArrayWithNull() {
        String[] result = AttributeUtil.toNonnullArray("a", null, "c");
        assertNull(result);
    }

    @Test
    void testToNonnullArrayEmpty() {
        String[] result = AttributeUtil.toNonnullArray();
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testToNonnullArrayNullInput() {
        String[] result = AttributeUtil.toNonnullArray((String[]) null);
        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // notNullOrElse
    // -------------------------------------------------------------------------

    @Test
    void testNotNullOrElseNonNull() {
        String result = AttributeUtil.notNullOrElse("value", "default");
        assertEquals("value", result);
    }

    @Test
    void testNotNullOrElseNull() {
        String result = AttributeUtil.notNullOrElse(null, "default");
        assertEquals("default", result);
    }

    @Test
    void testNotNullOrElseWithNumbers() {
        Integer result = AttributeUtil.notNullOrElse(null, 42);
        assertEquals(42, result);
        Integer result2 = AttributeUtil.notNullOrElse(7, 42);
        assertEquals(7, result2);
    }

    // -------------------------------------------------------------------------
    // arrayContains
    // -------------------------------------------------------------------------

    @Test
    void testArrayContainsFound() {
        assertTrue(AttributeUtil.arrayContains(new String[] {"a", "b", "c"}, "b"));
        assertTrue(AttributeUtil.arrayContains(new String[] {"a", "b", "c"}, "a"));
        assertTrue(AttributeUtil.arrayContains(new String[] {"a", "b", "c"}, "c"));
    }

    @Test
    void testArrayContainsNotFound() {
        assertFalse(AttributeUtil.arrayContains(new String[] {"a", "b", "c"}, "d"));
    }

    @Test
    void testArrayContainsEmpty() {
        assertFalse(AttributeUtil.arrayContains(new String[] {}, "a"));
    }

    @Test
    void testArrayContainsNullElement() {
        // null element in array matched by null search value
        assertTrue(AttributeUtil.arrayContains(new String[] {null, "a"}, null));
        assertFalse(AttributeUtil.arrayContains(new String[] {"a", "b"}, null));
    }
}
