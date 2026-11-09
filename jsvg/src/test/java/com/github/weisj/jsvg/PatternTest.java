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

import static com.github.weisj.jsvg.ImageComparison.*;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class PatternTest {

    @Test
    void patternRefTest() {
        assertEquals(SUCCESS, compareImages("pattern/patternUnits.svg"));
        // Batik renders this incorrectly.
        assertEquals(SUCCESS, compareImages(new ImageComparison.CompareInfo(
                expected(new PathImageSource("pattern/patternContentUnits_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("pattern/patternContentUnits.svg"), RenderType.JSVG))));
    }

    @Test
    void referencedPatternProvidesChildren() {
        String actualSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <defs>
                    <pattern id="base" patternUnits="userSpaceOnUse" width="100" height="100">
                      <rect width="50" height="50" fill="grey"/>
                      <rect x="50" y="50" width="50" height="50" fill="green"/>
                    </pattern>
                    <pattern id="derived" href="#base"/>
                  </defs>
                  <rect width="600" height="500" fill="url(#derived)"/>
                </svg>
                """;
        String referenceSvg = actualSvg.replace("url(#derived)", "url(#base)");
        assertEquals(SUCCESS, compareImages(new ImageComparison.CompareInfo(
                expected(new MemoryImageSource("base-pattern", referenceSvg), RenderType.JSVG),
                actual(new MemoryImageSource("referenced-pattern", actualSvg), RenderType.JSVG), 0, 0)));
    }
}
