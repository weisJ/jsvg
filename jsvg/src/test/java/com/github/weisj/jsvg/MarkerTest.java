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
package com.github.weisj.jsvg;

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.RenderType;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class MarkerTest {

    @Test
    void markerRefTest() {
        assertEquals(SUCCESS, compareImages("marker/marker3.svg"));
        assertEquals(SUCCESS, compareImages("marker/marker3_flattened.svg"));
    }

    @Test
    void testMarkerOrientAutoStartReverse() {
        // auto-start-reverse is an SVG 2 feature not supported by Batik;
        // compare against a flattened reference where the reversal is expressed
        // by mirroring the marker shape.
        assertEquals(SUCCESS, compareImages(new ImageComparison.CompareInfo(
                expected(new PathImageSource("marker/marker1_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("marker/marker1.svg"), RenderType.JSVG))));
    }

    @Test
    void testMarkerContextStroke() {
        // Batik does not support context-stroke; compare against a flattened reference
        assertEquals(SUCCESS, compareImages(new ImageComparison.CompareInfo(
                expected(new PathImageSource("marker/marker2_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("marker/marker2.svg"), RenderType.JSVG))));
    }

    @Test
    void testMarkerReferencePointWithViewportTransform() {
        assertEquals(SUCCESS, compareImages("marker/marker_bug157.svg"));
    }
}
