/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.*;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class ViewportBoundsTest {
    @TestFactory
    Stream<DynamicTest> referenceImages() {
        return cases().map(name -> comparison(name, RenderType.JSVG));
    }

    @TestFactory
    Stream<DynamicTest> independentRendererControls() {
        // Batik does not support SVG 2 symbol reference attributes or transforms on nested SVG elements.
        return cases().filter(name -> !name.equals("anchoredSymbol") && !name.equals("transformedViewport"))
                .map(name -> comparison(name, RenderType.Batik));
    }

    private static Stream<String> cases() {
        return Stream.of("ancestorBounds", "ancestorUseBounds", "ancestorBoundsControl", "ancestorPercentages",
                "transformedViewport", "anchoredSymbol");
    }

    private static DynamicTest comparison(String name, RenderType renderer) {
        String prefix = "filter/viewportBounds/" + name;
        return DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource(prefix + "_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource(prefix + ".svg"), renderer), 0, 0))));
    }
}
