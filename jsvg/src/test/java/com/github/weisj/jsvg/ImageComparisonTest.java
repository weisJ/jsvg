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

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.renderer.animation.AnimationState;

class ImageComparisonTest {
    @TempDir
    Path directory;

    @TestFactory
    Stream<DynamicTest> batikSamplesAnimationFrames() {
        var source = new MemoryImageSource("animated-width", """
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="8">
                  <rect width="4" height="8" fill="blue">
                    <animate attributeName="width" from="4" to="16" dur="1s" fill="freeze"/>
                  </rect>
                </svg>
                """);
        return Stream.of(0L, 500L, 1500L).map(timestamp -> {
            int width = timestamp == 0 ? 4 : timestamp == 500 ? 10 : 16;
            var reference = new MemoryImageSource("width-" + width, """
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="8">
                      <rect width="%d" height="8" fill="blue"/>
                    </svg>
                    """.formatted(width));
            return DynamicTest.dynamicTest(timestamp + " ms", () -> assertEquals(SUCCESS,
                    compareImages(new CompareInfo(expected(reference, RenderType.JSVG),
                            actual(source,
                                    RenderType.Batik.withAnimationState(new AnimationState(3000, 3000 + timestamp))),
                            0, 0))));
        });
    }

    @TestFactory
    Stream<DynamicTest> inMemorySvgResolvesRelativeImages() throws Exception {
        Files.writeString(directory.resolve("tile.svg"), """
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12">
                  <rect width="12" height="12" fill="blue"/>
                </svg>
                """);
        var source = new MemoryImageSource("relative-image", """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                     width="24" height="12">
                  <rect width="12" height="12" fill="lime"/>
                  <image x="12" width="12" height="12" xlink:href="tile.svg"/>
                </svg>
                """, directory.resolve("document.svg").toUri().toURL());
        var reference = new MemoryImageSource("two-colored-rectangles", """
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="12">
                  <rect width="12" height="12" fill="lime"/>
                  <rect x="12" width="12" height="12" fill="blue"/>
                </svg>
                """);
        // document.svg does not exist: render the supplied XML and use its URI only as a base.
        return Stream.of(RenderType.Batik, RenderType.JSVG)
                .map(renderer -> DynamicTest.dynamicTest(renderer.getClass().getSimpleName(),
                        () -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                                expected(reference, RenderType.Batik), actual(source, renderer), 0, 0)))));
    }

    @TestFactory
    Stream<DynamicTest> grayscaleReferencesPreserveColorAndAlpha() {
        return Stream.of("gray", "grayAlpha").flatMap(name -> Stream.of(8, 16).map(depth -> DynamicTest
                .dynamicTest(name + depth, () -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                        expected(new PathImageSource("imageComparison/" + name + "_ref.png"), RenderType.DiskImage),
                        actual(new PathImageSource("imageComparison/" + name + depth + ".png"), RenderType.DiskImage),
                        0, 0))))));
    }
}
