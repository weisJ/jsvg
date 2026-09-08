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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class FilterSamplingTest {
    @TestFactory
    Stream<DynamicTest> offsetsBringSourceIntoTheRepaint() {
        return Stream.of("offsetRight", "offsetLeft", "offsetDown", "offsetUp")
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    Rectangle clip = new Rectangle(64, 64, 12, 8);
                    assertEquals(SUCCESS, compareImages(new CompareInfo(
                            expected(new PathImageSource("filter/sampling/" + name + "_ref.svg"),
                                    RenderType.JSVG, graphics -> graphics.setClip(clip)),
                            actual(new PathImageSource("filter/sampling/" + name + ".svg"),
                                    RenderType.JSVG, graphics -> graphics.setClip(clip)),
                            0, 0)));
                }));
    }

    @TestFactory
    Stream<DynamicTest> samplingChainsMatchTheFullRender() {
        return Stream.of("threeBlurs", "blurThenOffset", "offsetThenBlur")
                .flatMap(name -> Stream.of(false, true).map(right -> DynamicTest.dynamicTest(
                        name + (right ? " right edge" : " left edge"), () -> {
                            Rectangle clip = new Rectangle(right ? 62 : 28, 32, 10, 80);
                            PathImageSource source = new PathImageSource("filter/sampling/" + name + ".svg");
                            // A repaint must reproduce the same pixels as a full render, including blur tails.
                            BufferedImage full = expected(source, RenderType.JSVG).render(null);
                            BufferedImage partial = actual(source, RenderType.JSVG,
                                    graphics -> graphics.setClip(clip)).render(null);
                            assertEquals(SUCCESS, compareImageRasterization(
                                    full.getSubimage(clip.x, clip.y, clip.width, clip.height),
                                    partial.getSubimage(clip.x, clip.y, clip.width, clip.height),
                                    name + (right ? "-right" : "-left"), 0, 0));
                        })));
    }
}
