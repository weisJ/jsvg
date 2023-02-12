/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
package com.github.weisj.jsvg;

import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static com.github.weisj.jsvg.ReferenceTest.render;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class FilterTest {

    @Test
    @Disabled("FIXME: Colors aren't accurate")
    void testGaussianBlur() {
        assertEquals(SUCCESS, compareImages("filter/blur.svg"));
        assertEquals(SUCCESS, compareImages("filter/blur2.svg"));
    }

    @Test
    void testGaussianBlurNotThrowing() {
        assertDoesNotThrow(() -> render("filter/blur.svg"));
        assertDoesNotThrow(() -> render("filter/blur2.svg"));
    }

    @Test
    void testColorMatrix() {
        assertEquals(SUCCESS, compareImages("filter/colormatrix.svg", 1.5));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void testTurbulence() {
        assertEquals(SUCCESS, compareImages("filter/turbulence1.svg"));
        assertEquals(SUCCESS, compareImages("filter/turbulence2.svg"));
        assertEquals(SUCCESS, compareImages("filter/turbulence3.svg"));
    }

    @Test
    void testFlood() {
        // TODO: Filter region not applied correctly.
        assertDoesNotThrow(() -> render("filter/flood.svg"));
        assertDoesNotThrow(() -> render("filter/floodColor_bug29.svg"));
    }

    @Test
    void testBlend() {
        // Filter region not applied correctly.
        assertDoesNotThrow(() -> render("filter/blend.svg"));
    }

    @Test
    void testDisplacementMap() {
        assertEquals(SUCCESS, compareImages("filter/displacement.svg"));
    }

}
