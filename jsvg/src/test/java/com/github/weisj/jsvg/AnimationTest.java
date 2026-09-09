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

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.renderer.animation.AnimationState;

class AnimationTest {

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
