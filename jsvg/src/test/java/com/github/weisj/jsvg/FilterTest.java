/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import com.github.weisj.jsvg.ReferenceTest.ImageSource.PathImageSource;

class FilterTest {

    @Test
    void testGaussianBlur() {
        assertEquals(SUCCESS, compareImages("filter/blur.svg", 0.4));
        assertEquals(SUCCESS, compareImages("filter/blur2.svg"));
    }

    @Test
    void testIdeaShadows() {
        assertEquals(SUCCESS, compareImages("filter/slim.svg", 0, 0.05));

        assertEquals(SUCCESS, compareImages("shadow/bottom.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/bottomLeft.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/bottomRight.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/left.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/right.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/top.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/topLeft.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/topRight.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("shadow/bottom.svg", 0, 0.05));

        assertEquals(SUCCESS, compareImages("roundShadow/bottomLeft.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("roundShadow/bottomRight.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("roundShadow/left.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("roundShadow/right.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("roundShadow/top.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("roundShadow/topLeft.svg", 0, 0.05));
        assertEquals(SUCCESS, compareImages("roundShadow/topRight.svg", 0, 0.05));
    }

    @Test
    void testEdgeMode() {
        assertDoesNotThrow(() -> renderJsvg("filter/edgeModeDuplicate.svg"));
        assertDoesNotThrow(() -> renderJsvg("filter/edgeModeNone.svg"));
        assertDoesNotThrow(() -> renderJsvg("filter/edgeModeWrap.svg"));
    }

    @Test
    void testGaussianBlurNotThrowing() {
        assertDoesNotThrow(() -> renderJsvg("filter/blur.svg"));
        assertDoesNotThrow(() -> renderJsvg("filter/blur2.svg"));
    }

    @Test
    void testColorMatrix() {
        assertEquals(SUCCESS, compareImages("filter/colormatrix_sRGB.svg"));
        assertEquals(SUCCESS, compareImages("filter/colormatrix_linearRGB.svg"));
        assertEquals(SUCCESS, compareImages("filter/colormatrix_bug41_1.svg"));
        assertEquals(SUCCESS, compareImages("filter/colormatrix_bug41_2.svg"));
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
        assertDoesNotThrow(() -> renderJsvg("filter/flood.svg"));
        assertDoesNotThrow(() -> renderJsvg("filter/floodColor_bug29.svg"));
    }

    @Test
    void testBlend() {
        // Filter region not applied correctly.
        assertDoesNotThrow(() -> renderJsvg("filter/blend.svg"));
        assertEquals(SUCCESS, compareImages("filter/blend_bug41.svg", 0, 1 / 256f));
    }

    @Test
    void testOffset() {
        assertDoesNotThrow(() -> renderJsvg("filter/offset.svg"));
    }

    @Test
    void testDisplacementMap() {
        assertEquals(SUCCESS, compareImages("filter/displacement.svg"));
    }

    @Test
    void testComposite() {
        // TODO: BackgroundImage not supported
        assertDoesNotThrow(() -> renderJsvg("filter/composite.svg"));
        assertEquals(SUCCESS, compareImages("filter/composite_bug33.svg"));
    }

    @Test
    void testMergeNode() {
        assertEquals(SUCCESS, compareImages("filter/merge.svg"));
        assertEquals(SUCCESS, compareImages("filter/merge_composite_bug33.svg"));
    }

    @Test
    void testOutOfBounds() {
        assertEquals(SUCCESS, compareImages("filter/outOfBoundsVisible.svg"));
        assertDoesNotThrow(() -> renderJsvg("filter/outOfBoundsHidden.svg"));
        assertEquals(SUCCESS, compareImages("filter/outOfBoundsHidden.svg"));
    }

    @Test
    void testFilterOnRoot() {
        assertEquals(SUCCESS, compareImages("filter/filterOnRoot_bug61.svg"));
    }

    @Test
    void testSubpixelAlignment() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/ptr_ref_bug62.svg"), RenderType.JSVG),
                actual(new PathImageSource("filter/ptr_bug62.svg"), RenderType.JSVG))));
    }

    @Test
    void testFilterChannels() {
        assertEquals(SUCCESS, compareImages("filter/channelSourceGraphic.svg"));
        assertEquals(SUCCESS, compareImages("filter/channelSourceAlpha.svg"));
    }

    @Test
    void testSourceAlpha() {
        assertEquals(SUCCESS, compareImages("filter/channelSourceAlphaBlend.svg", 0, 1 / 255f));
        assertEquals(SUCCESS, compareImages("filter/channelSourceAlphaBlend2.svg", 0, 1 / 255f));
        assertEquals(SUCCESS, compareImages("filter/channelSourceAlphaBlend_sRGB.svg", 0, 1 / 255f));
    }

    @Test
    void testFilterRegionClip() {
        assertEquals(SUCCESS, compareImages("filter/filterRegionClip.svg"));
    }

    @Test
    @Disabled("See #70")
    void testFilterPrimitiveRegionClip() {
        assertEquals(SUCCESS, compareImages("filter/filterPrimitiveRegionClip.svg"));
        assertEquals(SUCCESS, compareImages("filter/filterPrimitiveRegionClip2.svg"));
    }

    @Test
    void testComponentTransfer() {
        assertEquals(SUCCESS, compareImages("filter/componentTransfer.svg", 0.05, 0.05));
        assertEquals(SUCCESS, compareImages("filter/componentTransfer_sRGB.svg", 0.05, 0.05));
    }

    @Test
    void testDropShadow() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/dropShadow_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("filter/dropShadow.svg"), RenderType.JSVG))));
    }

    @Test
    void testEmptyGroup() {
        assertEquals(SUCCESS, compareImages("filter/emptyGroupNoTransform.svg"));
        assertEquals(SUCCESS, compareImages("filter/emptyGroupTransform.svg"));
    }
}
