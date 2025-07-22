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

import static com.github.weisj.jsvg.ReferenceTest.ImageInfo.actual;
import static com.github.weisj.jsvg.ReferenceTest.ImageInfo.expected;
import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static com.github.weisj.jsvg.ReferenceTest.renderJsvg;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ReferenceTest.CompareInfo;
import com.github.weisj.jsvg.ReferenceTest.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ReferenceTest.RenderType;

class VectorEffectsTest {

    @Test
    void simpleEffects() {
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/before.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/none.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonScalingSize.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonRotation.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/fixedPosition.svg"));
    }

    @Test
    void combinedEffects() {
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonScalingSizeNonRotation.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonScalingSizeFixedPosition.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonRotationFixedPosition.svg"));
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonScalingSizeNonRotationFixedPosition.svg"));
    }

    @Test
    void nonScalingStroke() {
        assertDoesNotThrow(() -> renderJsvg("vectorEffect/nonScalingStroke.svg"));
        Consumer<Graphics2D> hints =
                g -> g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("vectorEffect/nonScalingStroke_bug139_ref.svg"), RenderType.JSVG, hints),
                actual(new PathImageSource("vectorEffect/nonScalingStroke_bug139.svg"), RenderType.JSVG, hints))));
    }
}
