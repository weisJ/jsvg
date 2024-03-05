/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
import static com.github.weisj.jsvg.ReferenceTest.renderJsvg;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void textRefTest() {
        assertEquals(SUCCESS, compareImages("text/text1.svg"));
        assertEquals(SUCCESS, compareImages("text/text2.svg"));
        assertEquals(SUCCESS, compareImages("text/text5.svg"));
        // Batik doesn't correctly implement rotation.
        assertDoesNotThrow(() -> renderJsvg("text/text3.svg"));
        // textPath has to be checked manually.
        assertDoesNotThrow(() -> renderJsvg("text/text4.svg"));
        assertDoesNotThrow(() -> renderJsvg("text/text6.svg"));
    }

    @Test
    void textLengthTest() {
        // Batik doesn't implement this correctly. But our implementation isn't 100% correct either.
        assertDoesNotThrow(() -> renderJsvg("text/textLengthPath.svg"));
        // Batik also doesn't handle this correctly.
        assertDoesNotThrow(() -> renderJsvg("text/textLength.svg"));
    }

    @Test
    void lengthAdjustTest() {
        // Font rendering is not pixel perfect, so this test is flaky.
        assertDoesNotThrow(() -> renderJsvg("text/lengthAdjust.svg"));
        // assertEquals(SUCCESS, compareImages("text/lengthAdjust.svg"));
    }

    @Test
    void dominantBaselineTest() {
        assertDoesNotThrow(() -> renderJsvg("text/dominantBaseline.svg"));
    }

    @Test
    void textAnchorTest() {
        assertEquals(SUCCESS, compareImages("text/textAnchor.svg", 0.4));
    }

    @Test
    void letterSpacingTest() {
        assertEquals(SUCCESS, compareImages("text/letterSpacing.svg"));
    }

    @Test
    void fontStretchTest() {
        // Batik resolves font-stretch differently as we do, hence this will always fail.
        // Precisely AWT will happily synthesise a stretched font for us in all cases, but batik doesn't.
        assertDoesNotThrow(() -> renderJsvg("text/fontStretch.svg"));

    }
}
