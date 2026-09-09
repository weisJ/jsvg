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

import static com.github.weisj.jsvg.ImageComparison.*;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class FilterBufferTest {
    @TestFactory
    Stream<DynamicTest> transformedGaussianKernels() {
        // Batik renders its source in primitive coordinates and applies the final affine transform.
        // Allow only small per-channel differences from the two Gaussian approximations.
        return Stream.of("rotation", "shear", "singleAxis", "circular", "scaledRotation")
                .map(name -> DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS,
                        compareImages("filter/backingBuffer/" + name + ".svg", 0, 2 / 255.0))));
    }

    @TestFactory
    Stream<DynamicTest> transformedEdgeModes() {
        return Stream.of("duplicate", "wrap")
                .map(name -> DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS,
                        compareImages(new CompareInfo(
                                expected(new PathImageSource("filter/backingBuffer/constant_ref.svg"), RenderType.JSVG),
                                actual(new PathImageSource("filter/backingBuffer/" + name + ".svg"), RenderType.JSVG),
                                0, 2 / 255.0)))));
    }

    @TestFactory
    Stream<DynamicTest> sourceRendering() {
        return Stream.of("stroke", "singular", "nestedViewport", "pointwise")
                .map(name -> DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS,
                        compareImages(new CompareInfo(
                                expected(new PathImageSource("filter/backingBuffer/" + name + "_ref.svg"),
                                        RenderType.JSVG),
                                actual(new PathImageSource("filter/backingBuffer/" + name + ".svg"), RenderType.JSVG),
                                0, 0)))));
    }

    @Test
    void boundingBoxDeviationsUseBothObjectDimensions() {
        // For a 40 by 10 object, 0.1 in bounding-box units equals 4 by 1 in user units.
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/backingBuffer/objectBoundingBox_ref.svg"), RenderType.Batik),
                actual(new PathImageSource("filter/backingBuffer/objectBoundingBox.svg"), RenderType.JSVG),
                0, 2 / 255.0)));
    }

    @Test
    void deviceScalePreservesDetail() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/backingBuffer/deviceScale_ref.svg"), RenderType.Batik),
                actual(new PathImageSource("filter/backingBuffer/deviceScale.svg"), RenderType.JSVG,
                        graphics -> graphics.scale(2, 2)),
                0, 2 / 255.0)));
    }

    @Test
    void transformedDropShadowMatchesExplicitPrimitives() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/backingBuffer/dropShadow_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("filter/backingBuffer/dropShadow.svg"), RenderType.JSVG), 0, 0)));
    }

    @TestFactory
    Stream<DynamicTest> clippedRenderingRetainsSamples() {
        return Stream.of("rotation", "shear", "singleAxis", "scaledRotation")
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    PathImageSource source = new PathImageSource("filter/backingBuffer/" + name + ".svg");
                    Rectangle clip = new Rectangle(72, 64, 12, 12);
                    // Bicubic blitting needs pixels beyond the requested output clip as well as the blur halo.
                    BufferedImage full = expected(source, RenderType.JSVG, graphics -> graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)).render(null);
                    BufferedImage partial = actual(source, RenderType.JSVG,
                            graphics -> {
                                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                graphics.setClip(clip);
                            }).render(null);
                    assertEquals(SUCCESS, compareImageRasterization(
                            full.getSubimage(clip.x, clip.y, clip.width, clip.height),
                            partial.getSubimage(clip.x, clip.y, clip.width, clip.height),
                            "filter-buffer-clip-" + name, 0, 0));
                }));
    }
}
