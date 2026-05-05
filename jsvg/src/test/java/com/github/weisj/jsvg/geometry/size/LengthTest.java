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
package com.github.weisj.jsvg.geometry.size;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LengthTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    @Test
    void testUnspecifiedConstant() {
        assertTrue(Length.UNSPECIFIED.isUnspecified());
        assertFalse(Length.UNSPECIFIED.isSpecified());
        assertTrue(Float.isNaN(Length.UNSPECIFIED.raw()));
    }

    @Test
    void testZeroConstant() {
        assertTrue(Length.ZERO.isZero());
        assertFalse(Length.ZERO.isUnspecified());
        assertEquals(0f, Length.ZERO.raw());
    }

    @Test
    void testOneConstant() {
        assertEquals(1f, Length.ONE.raw());
        assertFalse(Length.ONE.isZero());
        assertFalse(Length.ONE.isUnspecified());
    }

    // -------------------------------------------------------------------------
    // isUnspecified / isSpecified (static helpers)
    // -------------------------------------------------------------------------

    @Test
    void testStaticIsUnspecified() {
        assertTrue(Length.isUnspecified(Float.NaN));
        assertFalse(Length.isUnspecified(0f));
        assertFalse(Length.isUnspecified(1f));
        assertFalse(Length.isUnspecified(-5f));
    }

    @Test
    void testStaticIsSpecified() {
        assertFalse(Length.isSpecified(Float.NaN));
        assertTrue(Length.isSpecified(0f));
        assertTrue(Length.isSpecified(42f));
    }

    // -------------------------------------------------------------------------
    // isAbsolute
    // -------------------------------------------------------------------------

    @Test
    void testIsAbsoluteForAbsoluteUnits() {
        assertTrue(new Length(Unit.PX, 10f).isAbsolute());
        assertTrue(new Length(Unit.IN, 1f).isAbsolute());
        assertTrue(new Length(Unit.CM, 2f).isAbsolute());
        assertTrue(new Length(Unit.MM, 5f).isAbsolute());
        assertTrue(new Length(Unit.Q, 4f).isAbsolute());
        assertTrue(new Length(Unit.PT, 12f).isAbsolute());
        assertTrue(new Length(Unit.PC, 3f).isAbsolute());
        assertTrue(new Length(Unit.RAW, 7f).isAbsolute());
    }

    @Test
    void testIsAbsoluteForRelativeUnits() {
        assertFalse(new Length(Unit.EM, 1f).isAbsolute());
        assertFalse(new Length(Unit.REM, 1f).isAbsolute());
        assertFalse(new Length(Unit.PERCENTAGE, 50f).isAbsolute());
        assertFalse(new Length(Unit.VW, 10f).isAbsolute());
        assertFalse(new Length(Unit.VH, 10f).isAbsolute());
    }

    // -------------------------------------------------------------------------
    // isZero
    // -------------------------------------------------------------------------

    @Test
    void testIsZero() {
        assertTrue(new Length(Unit.PX, 0f).isZero());
        assertFalse(new Length(Unit.PX, 1f).isZero());
        assertFalse(new Length(Unit.PX, -1f).isZero());
    }

    // -------------------------------------------------------------------------
    // coerceNonNegative
    // -------------------------------------------------------------------------

    @Test
    void testCoerceNonNegativePositive() {
        Length original = new Length(Unit.PX, 5f);
        assertSame(original, original.coerceNonNegative());
    }

    @Test
    void testCoerceNonNegativeZero() {
        Length original = new Length(Unit.PX, 0f);
        Length result = original.coerceNonNegative();
        assertSame(Length.ZERO, result);
    }

    @Test
    void testCoerceNonNegativeNegative() {
        Length original = new Length(Unit.PX, -3f);
        Length result = original.coerceNonNegative();
        assertSame(Length.ZERO, result);
    }

    @Test
    void testCoerceNonNegativeUnspecified() {
        // Unspecified stays unspecified
        Length result = Length.UNSPECIFIED.coerceNonNegative();
        assertSame(Length.UNSPECIFIED, result);
    }

    // -------------------------------------------------------------------------
    // multiply
    // -------------------------------------------------------------------------

    @Test
    void testMultiplyByZero() {
        Length l = new Length(Unit.PX, 10f);
        assertSame(Length.ZERO, l.multiply(0));
    }

    @Test
    void testMultiplyByOne() {
        Length l = new Length(Unit.PX, 10f);
        assertSame(l, l.multiply(1));
    }

    @Test
    void testMultiplyByScalar() {
        Length l = new Length(Unit.PX, 4f);
        Length result = l.multiply(3);
        assertEquals(12f, result.raw(), 0.001f);
        assertEquals(Unit.PX, result.unit());
    }

    // -------------------------------------------------------------------------
    // orElseIfUnspecified
    // -------------------------------------------------------------------------

    @Test
    void testOrElseIfUnspecifiedWhenSpecified() {
        Length l = new Length(Unit.PX, 5f);
        assertSame(l, l.orElseIfUnspecified(10f));
    }

    @Test
    void testOrElseIfUnspecifiedWhenUnspecified() {
        Length result = Length.UNSPECIFIED.orElseIfUnspecified(10f);
        assertEquals(10f, result.raw(), 0.001f);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    void testEquals() {
        Length a = new Length(Unit.PX, 5f);
        Length b = new Length(Unit.PX, 5f);
        Length c = new Length(Unit.EM, 5f);
        Length d = new Length(Unit.PX, 6f);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(null, a);
        assertNotEquals("string", a);
        assertEquals(a, a);
    }

    @Test
    void testHashCode() {
        Length a = new Length(Unit.PX, 5f);
        Length b = new Length(Unit.PX, 5f);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // -------------------------------------------------------------------------
    // Unit.valueOf
    // -------------------------------------------------------------------------

    @Test
    void testUnitValueOfZeroReturnsZeroConstant() {
        assertSame(Length.ZERO, Unit.PX.valueOf(0f));
        assertSame(Length.ZERO, Unit.EM.valueOf(0f));
    }

    @Test
    void testUnitValueOfNonZero() {
        Length l = Unit.PX.valueOf(5f);
        assertEquals(Unit.PX, l.unit());
        assertEquals(5f, l.raw());
    }

    // -------------------------------------------------------------------------
    // Unit.isPercentage
    // -------------------------------------------------------------------------

    @Test
    void testUnitIsPercentage() {
        assertTrue(Unit.PERCENTAGE.isPercentage());
        assertTrue(Unit.PERCENTAGE_WIDTH.isPercentage());
        assertTrue(Unit.PERCENTAGE_HEIGHT.isPercentage());
        assertTrue(Unit.PERCENTAGE_LENGTH.isPercentage());
        assertFalse(Unit.PX.isPercentage());
        assertFalse(Unit.EM.isPercentage());
        assertFalse(Unit.RAW.isPercentage());
    }
}
