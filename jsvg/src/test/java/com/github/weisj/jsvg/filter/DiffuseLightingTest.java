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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;

class DiffuseLightingTest {
    @TestFactory
    Stream<DynamicTest> repaintPreservesSurfaceNormals() {
        return Stream.of("defaultKernel", "scaledKernel", "explicitKernel", "blurredHeightMap")
                .flatMap(name -> Stream.of(false, true).map(vertical -> DynamicTest.dynamicTest(
                        name + (vertical ? " vertical edge" : " horizontal edge"), () -> {
                            Rectangle clip = vertical ? new Rectangle(80, 64, 8, 12) : new Rectangle(64, 80, 12, 8);
                            assertRepaintMatchesFullRender(name, clip);
                        })));
    }

    @TestFactory
    Stream<DynamicTest> transformedLightingMatchesTheFullRender() {
        return Stream.of("rotated", "sheared", "rotatedExplicit", "shearedExplicit", "shearedSurface",
                "pointLight", "spotLight")
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    Rectangle clip = new Rectangle(78, 76, 9, 11);
                    assertRepaintMatchesFullRender(name, clip);
                }));
    }

    @TestFactory
    Stream<DynamicTest> lightingMatchesReferenceArtwork() {
        return Stream.of("rotated", "rotatedPoint", "kernelDefaults")
                .map(name -> referenceArtwork(name, null));
    }

    @TestFactory
    Stream<DynamicTest> objectBoundingBoxUnitsMatchUserSpaceArtwork() {
        return Stream.of("squarePoint", "rectanglePoint", "rectangleSurface")
                .map(name -> referenceArtwork("units/" + name, null));
    }

    @TestFactory
    Stream<DynamicTest> clippedObjectBoundingBoxSurfaceMatchesReferenceArtwork() {
        return Stream.of(new Rectangle(60, 52, 8, 12), new Rectangle(40, 48, 12, 8))
                .map(clip -> referenceArtwork("units/rectangleSurface", clip));
    }

    @TestFactory
    Stream<DynamicTest> sobelNormalsMatchReferenceRenderer() {
        return Stream.of("interior", "boundary").map(name -> DynamicTest.dynamicTest(name, () -> {
            // Batik truncates some final color bands where JSVG rounds: allow one 8-bit level.
            PathImageSource source = new PathImageSource("filter/diffuseLighting/normals/" + name + ".svg");
            assertEquals(SUCCESS, compareImages(new CompareInfo(
                    expected(source, RenderType.Batik), actual(source, RenderType.JSVG), 0, 1.0 / 255)));
        }));
    }

    @Test
    void asymmetricNormalsMatchSpecifiedKernels() {
        // librsvg reference, independently checked at every pixel against the nine specified Sobel
        // kernels by tools/reference-audit/generate-heightmap.py. The PNG input fixes alpha rounding.
        // Batik has a left-edge discrepancy on this height field; it is not the oracle for this case.
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("filter/diffuseLighting/normals/asymmetric_ref.png"),
                        RenderType.DiskImage),
                actual(new PathImageSource("filter/diffuseLighting/normals/asymmetric.svg"), RenderType.JSVG),
                0, 1 / 255.0)));
    }

    @TestFactory
    Stream<DynamicTest> linearHeightRampHasConstantLighting() {
        return Stream.of("rotatedRamp", "shearedRamp", "reflectedRamp", "fractionalRamp")
                .map(name -> DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS,
                        compareImages(new CompareInfo(
                                expected(new PathImageSource("filter/diffuseLighting/ramp_ref.svg"), RenderType.JSVG),
                                actual(new PathImageSource("filter/diffuseLighting/" + name + ".svg"), RenderType.JSVG),
                                0, 0)))));
    }

    private static DynamicTest referenceArtwork(String name, Rectangle clip) {
        String path = "filter/diffuseLighting/" + name;
        String description = clip == null ? name : name + " clipped to " + clip;
        return DynamicTest.dynamicTest(description, () -> assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource(path + "_ref.svg"), RenderType.JSVG, graphics -> graphics.setClip(clip)),
                actual(new PathImageSource(path + ".svg"), RenderType.JSVG, graphics -> graphics.setClip(clip)), 0,
                0))));
    }

    private static void assertRepaintMatchesFullRender(String name, Rectangle clip) throws IOException {
        PathImageSource source = new PathImageSource("filter/diffuseLighting/" + name + ".svg");
        BufferedImage full = expected(source, RenderType.JSVG).render(null);
        BufferedImage partial = actual(source, RenderType.JSVG, graphics -> graphics.setClip(clip)).render(null);
        assertEquals(SUCCESS, compareImageRasterization(
                full.getSubimage(clip.x, clip.y, clip.width, clip.height),
                partial.getSubimage(clip.x, clip.y, clip.width, clip.height),
                name + "-" + clip.x + "-" + clip.y, 0, 0));
    }

}
