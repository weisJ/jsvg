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
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class TurbulenceTest {
    @TestFactory
    Stream<DynamicTest> matchesBatik() {
        return Stream.of("stitching", "transforms")
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    PathImageSource source = new PathImageSource("filter/turbulence/" + name + ".svg");
                    assertEquals(SUCCESS, compareImages(new CompareInfo(
                            expected(source, RenderType.Batik), actual(source, RenderType.JSVG), 0, 0)));
                }));
    }

    @Test
    void boundingBoxUnitsMatchExplicitCoordinates() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/turbulence/boundingBox_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("filter/turbulence/boundingBox.svg"), RenderType.JSVG), 0, 0)));
    }

    @Test
    void repaintKeepsTheStitchTile() throws IOException {
        PathImageSource source = new PathImageSource("filter/turbulence/repaint.svg");
        Rectangle clip = new Rectangle(60, 20, 30, 40);
        BufferedImage full = expected(source, RenderType.JSVG).render(null);
        BufferedImage partial = actual(source, RenderType.JSVG, graphics -> graphics.setClip(clip)).render(null);
        assertEquals(SUCCESS, compareImageRasterization(
                full.getSubimage(clip.x, clip.y, clip.width, clip.height),
                partial.getSubimage(clip.x, clip.y, clip.width, clip.height), "turbulence-repaint", 0, 0));
    }
}
