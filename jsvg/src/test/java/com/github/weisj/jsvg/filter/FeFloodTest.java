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
package com.github.weisj.jsvg.filter;

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.RenderType.JSVG;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.renderer.animation.AnimationState;

class FeFloodTest {
    @TestFactory
    Stream<DynamicTest> references() {
        return Stream.of("color-opacity", "transformed-regions", "color-only", "current-color")
                .map(name -> DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                        expected(source(name + "-ref"), JSVG), actual(source(name), JSVG), 0, 0)))));
    }

    @Test
    void animatedFloodColor() {
        assertFrame("animated-color", 0, "red");
        assertFrame("animated-color", 500, "purple");
        assertFrame("animated-color", 1500, "blue");
    }

    @Test
    void currentColorFollowsTheAnimatedRenderContext() {
        assertFrame("animated-current-color", 0, "red");
        assertFrame("animated-current-color", 500, "purple");
        assertFrame("animated-current-color", 1500, "blue");
    }

    @Test
    void floodAnimationStartsFromCurrentColor() {
        assertFrame("animation-with-current-color", 0, "green");
        assertFrame("animation-with-current-color", 1500, "purple");
        assertFrame("animation-with-current-color", 2500, "blue");
    }

    private static void assertFrame(String name, long timestamp, String color) {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(source("frame-" + color + "-ref"), JSVG),
                actual(source(name), JSVG.withAnimationState(new AnimationState(0, timestamp))), 0, 0)),
                name + " at " + timestamp + " ms");
    }

    private static PathImageSource source(String name) {
        return new PathImageSource("filter/flood/" + name + ".svg");
    }
}
