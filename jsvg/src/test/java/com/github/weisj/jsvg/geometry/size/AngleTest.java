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

class AngleTest {

    private static final float DELTA = 1e-5f;

    // -------------------------------------------------------------------------
    // AngleUnit conversions
    // -------------------------------------------------------------------------

    @Test
    void testDegToRadians() {
        // 0 deg → 0 rad
        assertEquals(0f, AngleUnit.Deg.toRadians(0f), DELTA);
        // 90 deg → π/2
        assertEquals((float) (Math.PI / 2), AngleUnit.Deg.toRadians(90f), DELTA);
        // 180 deg → π
        assertEquals((float) Math.PI, AngleUnit.Deg.toRadians(180f), DELTA);
        // 360 deg → 2π
        assertEquals((float) (2 * Math.PI), AngleUnit.Deg.toRadians(360f), DELTA);
        // Negative: -90 deg → -π/2
        assertEquals((float) (-Math.PI / 2), AngleUnit.Deg.toRadians(-90f), DELTA);
    }

    @Test
    void testGradToRadians() {
        // 400 grad = 360 deg = 2π rad
        assertEquals((float) (2 * Math.PI), AngleUnit.Grad.toRadians(400f), DELTA);
        // 100 grad = 90 deg = π/2 rad
        assertEquals((float) (Math.PI / 2), AngleUnit.Grad.toRadians(100f), DELTA);
        // 0 grad = 0 rad
        assertEquals(0f, AngleUnit.Grad.toRadians(0f), DELTA);
    }

    @Test
    void testRadToRadians() {
        // Rad unit should be identity
        assertEquals(0f, AngleUnit.Rad.toRadians(0f), DELTA);
        assertEquals(1f, AngleUnit.Rad.toRadians(1f), DELTA);
        assertEquals((float) Math.PI, AngleUnit.Rad.toRadians((float) Math.PI), DELTA);
    }

    @Test
    void testTurnToRadians() {
        // 1 turn = 2π rad
        assertEquals((float) (2 * Math.PI), AngleUnit.Turn.toRadians(1f), DELTA);
        // 0.5 turn = π rad
        assertEquals((float) Math.PI, AngleUnit.Turn.toRadians(0.5f), DELTA);
        // 0 turn = 0 rad
        assertEquals(0f, AngleUnit.Turn.toRadians(0f), DELTA);
    }

    @Test
    void testRawToRadians() {
        // Raw unit behaves like Deg (converts degrees to radians)
        assertEquals(AngleUnit.Deg.toRadians(90f), AngleUnit.Raw.toRadians(90f), DELTA);
    }

    // -------------------------------------------------------------------------
    // Angle class
    // -------------------------------------------------------------------------

    @Test
    void testAngleConstants() {
        assertTrue(Angle.UNSPECIFIED.isUnspecified());
        assertFalse(Angle.UNSPECIFIED.isSpecified());
        assertTrue(Float.isNaN(Angle.UNSPECIFIED.radians()));

        assertFalse(Angle.ZERO.isUnspecified());
        assertTrue(Angle.ZERO.isSpecified());
        assertEquals(0f, Angle.ZERO.radians(), DELTA);
    }

    @Test
    void testAngleConstructionDeg() {
        Angle a = new Angle(AngleUnit.Deg, 180f);
        assertEquals((float) Math.PI, a.radians(), DELTA);
        assertFalse(a.isUnspecified());
        assertTrue(a.isSpecified());
    }

    @Test
    void testAngleConstructionRad() {
        Angle a = new Angle(AngleUnit.Rad, (float) Math.PI);
        assertEquals((float) Math.PI, a.radians(), DELTA);
    }

    @Test
    void testAngleEqualsAndHashCode() {
        Angle a = new Angle(AngleUnit.Deg, 90f);
        Angle b = new Angle(AngleUnit.Rad, (float) (Math.PI / 2));
        // Both represent the same angle in radians
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        Angle c = new Angle(AngleUnit.Deg, 45f);
        assertNotEquals(a, c);
    }

    @Test
    void testAngleEqualsSpecialCases() {
        Angle a = new Angle(AngleUnit.Deg, 90f);
        assertEquals(a, a);
        assertNotEquals(null, a);
        assertNotEquals("angle", a);
    }

    @Test
    void testAngleStaticIsUnspecified() {
        assertTrue(Angle.isUnspecified(Float.NaN));
        assertFalse(Angle.isUnspecified(0f));
        assertFalse(Angle.isUnspecified(1f));
    }

}
