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
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.parser.LoaderContext;

class ImageBoundsTest {
    @TestFactory
    Stream<DynamicTest> referenceImages() {
        return Stream.of("explicitSize", "intrinsicSize", "intrinsicHeight", "intrinsicWidth", "percentageSize",
                "aspectRatio", "transformedGroup", "transformedClip")
                .map(name -> reference(name, RenderType.JSVG));
    }

    @TestFactory
    Stream<DynamicTest> missingResourcesUsePlaceholderBounds() {
        RenderType renderType = new RenderType.JSVGType(LoaderContext.builder()
                .resourceLoader((document, uri) -> platform -> Optional.empty()).build());
        return Stream.of("missingIntrinsic", "missingExplicit").map(name -> reference(name, renderType));
    }

    private static DynamicTest reference(String name, RenderType renderType) {
        return DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("image/bounds/" + name + "_ref.svg"), renderType),
                actual(new PathImageSource("image/bounds/" + name + ".svg"), renderType), 0, 0))));
    }
}
