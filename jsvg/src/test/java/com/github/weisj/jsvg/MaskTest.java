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

import static com.github.weisj.jsvg.ReferenceTest.*;
import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ReferenceTest.renderJsvg;
import static com.github.weisj.jsvg.SVGRenderingHints.KEY_MASK_CLIP_RENDERING;
import static com.github.weisj.jsvg.SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_ACCURACY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MaskTest {

    @Test
    void testMaskUnits() {
        assertEquals(SUCCESS, compareImages("mask/maskUnits.svg"));
        assertEquals(SUCCESS, compareImages("mask/maskUnitsPercentages.svg"));
    }

    @Test
    void testMaskContentUnits() {
        assertEquals(SUCCESS, compareImages("mask/maskContentUnits.svg"));
    }

    @Test
    void testTranslucentMask() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                new ImageInfo(new ImageSource.PathImageSource("mask/translucentMask_ref.svg"), RenderType.JSVG),
                new ImageInfo(new ImageSource.PathImageSource("mask/translucentMask.svg"), RenderType.JSVG))));
    }

    @Test
    void testOverlapping() {
        assertEquals(SUCCESS, compareImages("mask/overlapping.svg"));
    }

    @Test
    void referenceTests() {
        assertEquals(SUCCESS, compareImages("mask/mask1.svg"));
        assertEquals(SUCCESS, compareImages("mask/mask2.svg"));
        // [Note: Flaky] assertEquals(SUCCESS, compareImages("mask/chromeLogo.svg"));
        assertEquals(SUCCESS, compareImages("mask/classIcon.svg"));
        assertEquals(SUCCESS, compareImages("mask/complexTransform_bug32.svg"));
    }

    @Test
    void emptyGroupReportsCorrectSize() {
        assertDoesNotThrow(() -> renderJsvg("mask/empty_group_issue_48.svg"));
    }

    @Test
    void nestedMask() {
        assertEquals(SUCCESS, compareImages("mask/nestedMask.svg"));
    }


    @Test
    void testStrictRendering() {
        Consumer<String> test = path -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                new ImageInfo(new ImageSource.PathImageSource(path), RenderType.JSVG),
                new ImageInfo(new ImageSource.PathImageSource(path), RenderType.JSVG,
                        g -> g.setRenderingHint(KEY_MASK_CLIP_RENDERING, VALUE_MASK_CLIP_RENDERING_ACCURACY)))));
        test.accept("mask/chromeLogo.svg");
        test.accept("mask/classIcon.svg");
        test.accept("mask/mask1.svg");
        test.accept("mask/mask2.svg");
        test.accept("mask/maskContentUnits.svg");
        test.accept("mask/maskUnits.svg");
        test.accept("mask/maskUnitsPercentages.svg");
        test.accept("mask/nestedMask.svg");
        test.accept("mask/overlapping.svg");
    }

    @Test
    void testMaskClipBleedInNonAccurateMode() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                new ImageInfo(new ImageSource.PathImageSource("mask/mask_isolation_2_bug74_ref.svg"), RenderType.JSVG),
                new ImageInfo(new ImageSource.PathImageSource("mask/mask_isolation_2_bug74.svg"), RenderType.JSVG,
                        g -> g.setRenderingHint(KEY_MASK_CLIP_RENDERING, VALUE_MASK_CLIP_RENDERING_ACCURACY)))));
    }

    @Test
    @Disabled("Batik does not render this correctly")
    void testMaskClipBleedInAccurateModeWithTransparentMask() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                new ImageInfo(new ImageSource.PathImageSource("mask/mask_isolation_bug74.svg"), RenderType.Batik),
                new ImageInfo(new ImageSource.PathImageSource("mask/mask_isolation_bug74.svg"), RenderType.JSVG,
                        g -> g.setRenderingHint(KEY_MASK_CLIP_RENDERING, VALUE_MASK_CLIP_RENDERING_ACCURACY)))));
    }

    @Test
    void testMaskAreaIfFilterIsApplied() {
        assertEquals(SUCCESS, compareImages("mask/filterMask_bug84.svg"));
    }
}
