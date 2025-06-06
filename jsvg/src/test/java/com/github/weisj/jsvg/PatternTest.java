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
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ReferenceTest.ImageSource.PathImageSource;

class PatternTest {

    @Test
    void patternRefTest() {
        assertEquals(SUCCESS, compareImages("pattern/patternUnits.svg"));
        // Batik renders this incorrectly.
        assertEquals(SUCCESS, compareImages(new ReferenceTest.CompareInfo(
                expected(new PathImageSource("pattern/patternContentUnits_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("pattern/patternContentUnits.svg"), RenderType.JSVG))));
    }
}
