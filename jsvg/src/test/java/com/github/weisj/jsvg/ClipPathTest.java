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
import static com.github.weisj.jsvg.ReferenceTest.RenderType;
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static com.github.weisj.jsvg.SVGRenderingHints.KEY_MASK_CLIP_RENDERING;
import static com.github.weisj.jsvg.SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_ACCURACY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class ClipPathTest {

    @Test
    void testClipPathUnits() {
        assertEquals(SUCCESS, compareImages("clipPath/clipPathUnits.svg"));
        assertEquals(SUCCESS, compareImages("clipPath/clipPathUnits2.svg"));
        assertEquals(SUCCESS, compareImages("clipPath/clipPathTransform.svg"));
    }

    @Test
    void testClipPathUnitsSoftClip() {
        ReferenceTest.SOFT_CLIPPING_VALUE = SVGRenderingHints.VALUE_SOFT_CLIPPING_ON;
        assertEquals(SUCCESS, compareImages("clipPath/clipPathUnits.svg", 0.75));
        assertEquals(SUCCESS, compareImages("clipPath/clipPathUnits2.svg", 0.75));
        assertEquals(SUCCESS, compareImages("clipPath/clipPathTransform.svg", 0.75));
        ReferenceTest.SOFT_CLIPPING_VALUE = SVGRenderingHints.VALUE_SOFT_CLIPPING_OFF;
    }

    @Test
    void filterAndClipPath() {
        ReferenceTest.SOFT_CLIPPING_VALUE = SVGRenderingHints.VALUE_SOFT_CLIPPING_ON;
        assertEquals(SUCCESS, compareImages("clipPath/filterAndClipPath.svg"));
        assertEquals(SUCCESS, compareImages("clipPath/filterAndClipPath2.svg", 0.1));
        ReferenceTest.SOFT_CLIPPING_VALUE = SVGRenderingHints.VALUE_SOFT_CLIPPING_OFF;
    }

    @Test
    void testStrictRendering() {
        Consumer<String> test = path -> {
            assertEquals(SUCCESS, compareImages(new ReferenceTest.CompareInfo(
                    new ReferenceTest.ImageInfo(new ReferenceTest.ImageSource.PathImageSource(path), RenderType.JSVG),
                    new ReferenceTest.ImageInfo(new ReferenceTest.ImageSource.PathImageSource(path), RenderType.JSVG,
                            g -> g.setRenderingHint(KEY_MASK_CLIP_RENDERING, VALUE_MASK_CLIP_RENDERING_ACCURACY)))));
        };
        for (String path : ResourceWalker.findIcons(SVGViewer.class.getPackage(), "clipPath")) {
            test.accept(path);
        }
    }
}
