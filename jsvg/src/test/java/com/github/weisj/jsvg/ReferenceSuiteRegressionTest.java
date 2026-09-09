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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.renderer.animation.AnimationState;

/** Small regressions that also run when the optional external suites are absent. */
class ReferenceSuiteRegressionTest {
    @TempDir
    Path directory;

    @Test
    void gradientsAndPaintFallbacks() {
        // The reference expresses ordered stops and solid fallback colors directly. Use the same
        // rasterizer to avoid unrelated endpoint quantization differences in Batik's gradients.
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("paint/suiteRegressions_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("paint/suiteRegressions.svg"), RenderType.JSVG), 0, 0)));
    }

    @Test
    void hueRotation() {
        // Allow one byte of rounding difference between the two filter implementations.
        assertEquals(SUCCESS, compareImages("filter/hueRotateBoundaries.svg", 0, 1 / 255f));
    }

    @Test
    void componentTransferClampsWithoutOverflow() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/componentTransferClamping_ref.svg"), RenderType.Batik),
                actual(new PathImageSource("filter/componentTransferClamping.svg"), RenderType.JSVG), 0, 0)));
    }

    @TestFactory
    Stream<DynamicTest> quotedAndEscapedExternalPaintUrls() throws Exception {
        Files.writeString(directory.resolve("paint).svg"), """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <linearGradient id="blue"><stop stop-color="blue"/></linearGradient>
                </svg>
                """);
        var reference = new MemoryImageSource("blue", """
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
                  <rect width="16" height="16" fill="blue"/>
                </svg>
                """);
        return Stream.of("url('paint).svg#blue') red", "url(paint\\).svg#blue) red")
                .map(paint -> DynamicTest.dynamicTest(paint, () -> {
                    var source = new MemoryImageSource("external-paint", """
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
                              <rect width="16" height="16" fill="%s"/>
                            </svg>
                            """.formatted(paint), directory.resolve("document.svg").toUri().toURL());
                    assertEquals(SUCCESS, compareImages(new CompareInfo(expected(reference, RenderType.Batik),
                            actual(source, RenderType.JSVG), 0, 0)));
                }));
    }

    @TestFactory
    Stream<DynamicTest> animationEndFrames() {
        return Stream.of("freeze", "remove").flatMap(fill -> Stream.of(499L, 500L, 1000L, 1499L, 1500L, 1501L)
                .map(timestamp -> DynamicTest.dynamicTest(fill + " at " + timestamp + " ms", () -> {
                    var source = new MemoryImageSource("animated-width-" + fill, """
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="8">
                              <rect width="4" height="8" fill="blue">
                                <animate attributeName="width" from="4" to="16" begin="0.5s" dur="1s" fill="%s"/>
                              </rect>
                            </svg>
                            """.formatted(fill));
                    double width = timestamp < 500 || timestamp >= 1500 && fill.equals("remove") ? 4
                            : timestamp >= 1500 ? 16 : 4 + 12 * (timestamp - 500) / 1000d;
                    var reference = new MemoryImageSource("static-width-" + fill + "-" + timestamp, """
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="8">
                              <rect width="%s" height="8" fill="blue"/>
                            </svg>
                            """.formatted(width));
                    assertEquals(SUCCESS, compareImages(new CompareInfo(expected(reference, RenderType.JSVG),
                            actual(source, RenderType.JSVG.withAnimationState(new AnimationState(0, timestamp))), 0,
                            0)));
                })));
    }
}
