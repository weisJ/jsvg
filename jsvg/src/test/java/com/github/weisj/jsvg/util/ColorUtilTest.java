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

import java.awt.*;

import org.junit.jupiter.api.Test;

class ColorUtilTest {

    @Test
    void testDiv255() {
        // div255(x) should equal Math.round(x / 255.0) for values in [0, 255*255]
        assertEquals(0, ColorUtil.div255(0));
        assertEquals(1, ColorUtil.div255(255));
        assertEquals(255, ColorUtil.div255(255 * 255));
        // Mid-range: 128 * 255 = 32640 => 128
        assertEquals(128, ColorUtil.div255(128 * 255));
    }

    @Test
    void testToRgbRange() {
        assertEquals(0, ColorUtil.toRgbRange(-10.0));
        assertEquals(0, ColorUtil.toRgbRange(0.0));
        assertEquals(128, ColorUtil.toRgbRange(128.0));
        assertEquals(255, ColorUtil.toRgbRange(255.0));
        assertEquals(255, ColorUtil.toRgbRange(300.0));
        assertEquals(200, ColorUtil.toRgbRange(199.6));
    }

    @Test
    void testComputeLuminance() {
        // Black: all zeros → 0
        assertEquals(0, ColorUtil.computeLuminance(0, 0, 0));
        // White: 255,255,255 → 255
        assertEquals(255, ColorUtil.computeLuminance(255, 255, 255));
        // Pure red: dominant red channel
        int redLum = ColorUtil.computeLuminance(255, 0, 0);
        assertTrue(redLum > 0 && redLum < 255);
        // Pure green: highest luminance coefficient
        int greenLum = ColorUtil.computeLuminance(0, 255, 0);
        assertTrue(greenLum > redLum, "Green should contribute more luminance than red");
        // Pure blue: lowest coefficient
        int blueLum = ColorUtil.computeLuminance(0, 0, 255);
        assertTrue(blueLum < redLum, "Blue should contribute less luminance than red");
    }

    @Test
    void testWithAlpha() {
        Color base = new Color(100, 150, 200);

        Color fully = ColorUtil.withAlpha(base, 1.0f);
        assertEquals(255, fully.getAlpha());
        assertEquals(base.getRed(), fully.getRed());
        assertEquals(base.getGreen(), fully.getGreen());
        assertEquals(base.getBlue(), fully.getBlue());

        Color transparent = ColorUtil.withAlpha(base, 0.0f);
        assertEquals(0, transparent.getAlpha());

        Color half = ColorUtil.withAlpha(base, 0.5f);
        assertEquals(127, half.getAlpha());

        // Clamp: alpha > 1.0 → 255
        Color clamped = ColorUtil.withAlpha(base, 2.0f);
        assertEquals(255, clamped.getAlpha());

        // Clamp: alpha < 0 → 0
        Color clampedNeg = ColorUtil.withAlpha(base, -0.5f);
        assertEquals(0, clampedNeg.getAlpha());
    }

    @Test
    void testToStringColor() {
        assertEquals("null", ColorUtil.toString(null));
        Color c = new Color(10, 20, 30, 40);
        assertEquals("Color[10,20,30,40]", ColorUtil.toString(c));
    }

    @Test
    void testClampColor() {
        assertEquals(0, ColorUtil.clampColor(-1));
        assertEquals(0, ColorUtil.clampColor(0));
        assertEquals(128, ColorUtil.clampColor(128));
        assertEquals(255, ColorUtil.clampColor(255));
        assertEquals(255, ColorUtil.clampColor(256));
    }

    @Test
    void testHslRoundTrip() {
        // Convert known RGB → HSL → RGB and verify we get back the same values (within rounding)
        int[] testColors = {
            // r, g, b, alpha
            255, 0, 0, 255,     // pure red
            0, 255, 0, 255,     // pure green
            0, 0, 255, 255,     // pure blue
            128, 128, 128, 255, // grey
            255, 255, 0, 255,   // yellow
            0, 255, 255, 255,   // cyan
        };

        for (int i = 0; i < testColors.length; i += 4) {
            int r = testColors[i], g = testColors[i + 1], b = testColors[i + 2], a = testColors[i + 3];
            float[] hsl = new float[3];
            ColorUtil.convertRGBPretoHSL(r, g, b, a, hsl);
            // h, s, l must be in [0,1]
            assertTrue(hsl[0] >= 0f && hsl[0] <= 1f, "Hue out of range for r=" + r + " g=" + g + " b=" + b);
            assertTrue(hsl[1] >= 0f && hsl[1] <= 1f, "Sat out of range for r=" + r + " g=" + g + " b=" + b);
            assertTrue(hsl[2] >= 0f && hsl[2] <= 1f, "Lum out of range for r=" + r + " g=" + g + " b=" + b);

            int[] rgb = new int[3];
            ColorUtil.convertHSLtoRGB(hsl[0], hsl[1], hsl[2], rgb);
            // Allow ±2 due to floating point rounding
            assertNear(r, rgb[0], "r", r, g, b);
            assertNear(g, rgb[1], "g", r, g, b);
            assertNear(b, rgb[2], "b", r, g, b);
        }
    }

    @Test
    void testConvertHSLtoRGB_achromatic() {
        // s=0 → greyscale: r==g==b == l*255
        int[] rgb = new int[3];
        ColorUtil.convertHSLtoRGB(0.5f, 0.0f, 0.6f, rgb);
        assertEquals(rgb[0], rgb[1]);
        assertEquals(rgb[1], rgb[2]);
        assertEquals((int) (0.6f * 255f), rgb[0]);
    }

    @Test
    void testSRGBLinearRoundTrip() {
        // Verify that sRGB → linear is monotonically non-decreasing (higher sRGB → higher linear).
        int prevLinear = -1;
        for (int v = 0; v <= 255; v++) {
            int linear = ColorUtil.sRGBtoLinearRGBBand(v);
            assertTrue(linear >= prevLinear, "sRGB→linear not monotone at v=" + v);
            prevLinear = linear;
        }
        // Verify extremes
        assertEquals(0, ColorUtil.sRGBtoLinearRGBBand(0));
        assertEquals(255, ColorUtil.sRGBtoLinearRGBBand(255));
    }

    @Test
    void testSRGBLinearARGBConversion() {
        int argb = 0xFF8040C0; // alpha=255, r=128, g=64, b=192
        int linear = ColorUtil.sRGBtoLinearRGB(argb);
        // Alpha should be preserved
        assertEquals(0xFF, (linear >>> 24) & 0xFF);
        // Verify the conversion is non-trivial (values change)
        assertNotEquals(0x80, (linear >> 16) & 0xFF);
        // Verify round-trip for mid-range values (lower loss than near-black)
        int back = ColorUtil.linearRGBtoSRGB(linear);
        assertEquals(0xFF, (back >>> 24) & 0xFF);
        // For mid-range sRGB values the round-trip should be close (within ±2)
        assertTrue(Math.abs(128 - ((back >> 16) & 0xFF)) <= 2,
                "Red channel round-trip error too large: got " + ((back >> 16) & 0xFF));
        assertTrue(Math.abs(64 - ((back >> 8) & 0xFF)) <= 2,
                "Green channel round-trip error too large: got " + ((back >> 8) & 0xFF));
        assertTrue(Math.abs(192 - (back & 0xFF)) <= 2,
                "Blue channel round-trip error too large: got " + (back & 0xFF));
    }

    private static void assertNear(int expected, int actual, String channel, int r, int g, int b) {
        assertTrue(Math.abs(expected - actual) <= 2,
                "Channel " + channel + " out of range for input r=" + r + " g=" + g + " b=" + b
                        + ": expected " + expected + " but got " + actual);
    }
}
