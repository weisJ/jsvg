/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jannis Weis
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

import static com.github.weisj.jsvg.ReferenceTest.*;
import static com.github.weisj.jsvg.ReferenceTest.ImageInfo.actual;
import static com.github.weisj.jsvg.ReferenceTest.ImageInfo.expected;
import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ReferenceTest.ImageSource.PathImageSource;

class TransformTest {

    @Test
    void refTest() {
        assertEquals(SUCCESS, compareImages("transform/transform.svg"));
        assertEquals(SUCCESS, compareImages("transform/matrix.svg"));
        assertEquals(SUCCESS, compareImages("transform/rotate.svg"));
        assertEquals(SUCCESS, compareImages("transform/translate.svg"));
        assertEquals(SUCCESS, compareImages("transform/scale.svg"));
        assertEquals(SUCCESS, compareImages("transform/skewX.svg"));
        assertEquals(SUCCESS, compareImages("transform/skewY.svg"));

        // Batik does not implement transform on <svg> elements
        assertDoesNotThrow(() -> compareImages("transform/SVGinSVG.svg"));
    }

    @Test
    void testTransformBoxFill() {
        // Compare with reference as batik doesn't support transform-box
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("transform/transformBoxFill_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("transform/transformBoxFill.svg"), RenderType.JSVG))));
    }

    @Test
    void testTransformBoxStroke() {
        // Compare with reference as batik doesn't support transform-box
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("transform/transformBoxStroke_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("transform/transformBoxStroke.svg"), RenderType.JSVG))));
    }

    @Test
    void testTransformBoxFillTransform() {
        // Compare with reference as batik doesn't support transform-box
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("transform/transformBoxFillTranslate_ref.svg"),
                        RenderType.JSVG),
                actual(new PathImageSource("transform/transformBoxFillTranslate.svg"),
                        RenderType.JSVG))));
    }

    @Test
    void testTransformBoxStrokeTransform() {
        // Compare with reference as batik doesn't support transform-box
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("transform/transformBoxStrokeTranslate_ref.svg"),
                        RenderType.JSVG),
                actual(new PathImageSource("transform/transformBoxStrokeTranslate.svg"),
                        RenderType.JSVG))));
    }

    @Test
    void testSplitNumbersWithoutSpaceIfLeadingMinus() {
        assertEquals(SUCCESS, compareImages("transform/noSpace_bug146.svg"));
    }
}
