/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static com.github.weisj.jsvg.ImageComparison.renderJsvg;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;

class VectorEffectsTest {

    private static void testVectorEffect(String name) {
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/ve-" + name + ".svg"));
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("vectorEffect/ref-ve-" + name + ".svg"), RenderType.JSVG),
                actual(new PathImageSource("vectorEffect/ve-" + name + ".svg"), RenderType.JSVG))));
    }

    @Test
    void simpleEffects() {
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/ve-initial.svg"));
        testVectorEffect("none");
        testVectorEffect("nonScalingSize");
        testVectorEffect("nonRotation");
        testVectorEffect("fixedPosition");
    }

    @Test
    void combinedEffects() {
        testVectorEffect("nonScalingSize-nonRotation");
        testVectorEffect("nonScalingSize-fixedPosition");
        testVectorEffect("nonRotation-fixedPosition");
        testVectorEffect("nonScalingSize-nonRotation-fixedPosition");
    }

    @Test
    void nonScalingStroke() {
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonScalingStroke.svg"));
        Consumer<Graphics2D> hints =
                g -> g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("vectorEffect/nonScalingStroke_bug139_ref.svg"), RenderType.JSVG, hints),
                actual(new PathImageSource("vectorEffect/nonScalingStroke_bug139.svg"), RenderType.JSVG, hints))));
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("vectorEffect/nonScalingStroke_bug139_ref.svg"), RenderType.JSVG, hints),
                actual(new PathImageSource("vectorEffect/nonScalingStroke_bug139_with_filter.svg"), RenderType.JSVG,
                        hints))));
    }
}
