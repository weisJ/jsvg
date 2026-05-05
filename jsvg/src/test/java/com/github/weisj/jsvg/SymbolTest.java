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

class SymbolTest {

    @Test
    void symbolRefTest() {
        assertEquals(SUCCESS, compareImages("symbol/symbol1.svg"));
    }

    @Test
    void testSymbolRefXRefYCenter() {
        // refX="center" / refY="center" are SVG 2 keywords not supported by Batik;
        // compare against a flattened reference using their numeric equivalents.
        assertEquals(SUCCESS, compareImages(new ImageComparison.CompareInfo(
                expected(new PathImageSource("symbol/symbol2_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("symbol/symbol2.svg"), RenderType.JSVG))));
    }
}
