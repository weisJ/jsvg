/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.attributes.filter.EdgeMode;

class EdgeModeTest {
    private static final int TEST_WIDTH = 900;
    private static final int TEST_HEIGHT = 500;

    @TestFactory
    Stream<DynamicTest> zeroDeviationAxisSubregionReferences() {
        return Stream.of("duplicateHorizontal", "duplicateVertical", "wrapHorizontal", "wrapVertical")
                .map(name -> DynamicTest.dynamicTest(name, () -> assertEquals(SUCCESS,
                        compareImages(new CompareInfo(
                                expected(new PathImageSource("filter/edgeMode/" + name + "_ref.svg"),
                                        RenderType.JSVG),
                                actual(new PathImageSource("filter/edgeMode/" + name + ".svg"), RenderType.JSVG),
                                0, 2 / 255.0)))));
    }

    @Test
    void zeroRadiusOmitsAxis() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB_PRE);
        List<EdgeMode.Axis> convolvedAxes = new ArrayList<>();

        EdgeMode.None.convolveSeparable(image, new Rectangle(0, 0, 2, 2), null,
                new EdgeMode.SeparableConvolveOperation() {
                    @Override
                    public int radius(EdgeMode.@NotNull Axis axis) {
                        return axis == EdgeMode.Axis.Horizontal ? 0 : 1;
                    }

                    @Override
                    public @NotNull BufferedImage convolve(@NotNull BufferedImage image,
                            EdgeMode.@NotNull Axis axis, RenderingHints hints) {
                        convolvedAxes.add(axis);
                        return image;
                    }
                });

        assertEquals(List.of(EdgeMode.Axis.Vertical), convolvedAxes);
    }

    @TestFactory
    Stream<DynamicTest> edgeModesOperateOnTheSameDestinationPixels() {
        return Stream.of(EdgeMode.values()).map(mode -> DynamicTest.dynamicTest(mode.name(), () -> {
            BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB_PRE);
            List<Dimension> convolutionSizes = new ArrayList<>();

            mode.convolveSeparable(image, new Rectangle(5, 5, 10, 10), null,
                    new EdgeMode.SeparableConvolveOperation() {
                        @Override
                        public int radius(EdgeMode.@NotNull Axis axis) {
                            return axis == EdgeMode.Axis.Horizontal ? 2 : 0;
                        }

                        @Override
                        public @NotNull BufferedImage convolve(@NotNull BufferedImage paddedImage,
                                EdgeMode.@NotNull Axis axis, RenderingHints hints) {
                            convolutionSizes.add(new Dimension(paddedImage.getWidth(), paddedImage.getHeight()));
                            return paddedImage;
                        }
                    });

            // edgeMode changes the samples outside the logical source bounds. It must not change
            // the destination raster on which the convolution runs.
            assertEquals(List.of(new Dimension(24, 20)), convolutionSizes);
        }));
    }

    @TestFactory
    Stream<DynamicTest> sourceGraphicUsesFilterRegionAsImageBounds() {
        return Stream.of("duplicate", "wrap").map(mode -> DynamicTest.dynamicTest(mode, () -> {
            int width = 600;
            int height = 500;
            String artwork = element("g").attributes("filter='url(#f)'").children(
                    rectangle(250, 200, 100, 100, 0xffe08040)).build();
            String actual = filterDocument(width, height,
                    element("feGaussianBlur")
                            .attributes("stdDeviation='12'", "edgeMode='" + mode + "'")
                            .build(),
                    artwork, "", "color-interpolation-filters='sRGB'");
            String reference = filterDocument(width, height,
                    element("feGaussianBlur")
                            .attributes("stdDeviation='12' edgeMode='none'")
                            .build(),
                    artwork, "", "color-interpolation-filters='sRGB'");

            // The painted rectangle is tightly bounded, but the logical SourceGraphic also
            // contains the transparent remainder of the filter region. Its edges are therefore
            // too far away for duplicate or wrap to affect this blur.
            compareSvg("source-graphic-filter-region-" + mode, actual, reference);
        }));
    }

    @TestFactory
    Stream<DynamicTest> clippedRenderingRetainsInputSamples() {
        return Stream.of("duplicate", "wrap").map(mode -> DynamicTest.dynamicTest(mode, () -> {
            int width = 600;
            int height = 500;
            String primitive = element("feGaussianBlur")
                    .attributes("stdDeviation='12 0'")
                    .attributes(Map.of("edgeMode", mode)).build();
            String artwork = element("g").attributes("filter='url(#f)'").children(
                    rectangle(0, 0, width, height, 0xff204060),
                    rectangle(230, 0, 90, height, 0xffe08040),
                    rectangle(570, 0, 30, height, 0xff40c080)).build();
            String svg = filterDocument(width, height, primitive, artwork, "",
                    "color-interpolation-filters='sRGB'");
            Rectangle clip = mode.equals("wrap")
                    ? new Rectangle(0, 200, 40, 100)
                    : new Rectangle(250, 200, 50, 100);
            MemoryImageSource source = new MemoryImageSource("edge-mode-repaint-" + mode, svg);
            BufferedImage full = expected(source, RenderType.JSVG).render(null);
            BufferedImage partial = actual(source, RenderType.JSVG,
                    graphics -> graphics.setClip(clip)).render(null);

            assertEquals(SUCCESS, compareImageRasterization(
                    full.getSubimage(clip.x, clip.y, clip.width, clip.height),
                    partial.getSubimage(clip.x, clip.y, clip.width, clip.height),
                    "edge-mode-repaint-" + mode, 0, 0));
        }));
    }

    @TestFactory
    Stream<DynamicTest> duplicateAndWrapMatchContinuedArtwork() {
        return Stream.of("duplicate", "wrap").flatMap(mode -> Stream.of("1.5", "4", "12", "1.5 0", "0 1.5")
                .map(deviation -> DynamicTest.dynamicTest(mode + " " + deviation, () -> {
                    // The vertical bands continue beyond the viewport. Their left and right
                    // surroundings have the same color, so both edge modes reproduce that continuation.
                    String artwork = element("g").attributes("filter='url(#f)'").children(
                            element("rect").attributes("x='-300' y='-300' width='800' height='800' fill='#204060'"),
                            element("rect").attributes("x='30' y='-300' width='20' height='800' fill='#e08040'"),
                            element("rect").attributes("x='75' y='-300' width='25' height='800' fill='#40c080'"))
                            .build();
                    compare(mode, deviation, artwork, "x='-300' y='-300' width='800' height='800'");
                })));
    }

    @TestFactory
    Stream<DynamicTest> noneMatchesTransparentSurroundings() {
        return Stream.of("1.5", "1.5 0", "0 1.5").map(deviation -> DynamicTest.dynamicTest("none " + deviation, () -> {
            String artwork = element("g").attributes("filter='url(#f)'").children(
                    element("rect").attributes("x='25' y='20' width='35' height='40' fill='#e08040'"),
                    element("rect").attributes("x='80' y='25' width='40' height='30' fill='#40c080'"))
                    .build();
            compare("none", deviation, artwork, "");
        }));
    }

    @TestFactory
    Stream<DynamicTest> inputSubregionMatchesContinuedArtwork() {
        return Stream.of("duplicate", "wrap", "none")
                .flatMap(mode -> Stream.of("1.5", "4", "12", "1.5 0", "0 1.5")
                        .map(deviation -> DynamicTest.dynamicTest(mode + " subregion " + deviation,
                                () -> compareSubregion(mode, deviation, false))));
    }

    @TestFactory
    Stream<DynamicTest> namedInputSurvivesLaterResults() {
        return Stream.of("duplicate", "wrap", "none")
                .map(mode -> DynamicTest.dynamicTest(mode + " named input",
                        () -> compareSubregion(mode, "1.5", true)));
    }

    private static void compareSubregion(String mode, String deviation, boolean named) {
        ElementBuilder input = element("feOffset").attributes("x='32' y='24' width='24' height='16'");
        ElementBuilder blur = element("feGaussianBlur")
                .attributes("x='0' y='0' width='144' height='80'", "result='tile'")
                .attributes(Map.of("stdDeviation", deviation, "edgeMode", mode));
        String interveningResult = "";
        if (named) {
            input.attributes("result='tile'");
            blur.attributes("in='tile'");
            // The named input must remain accessible after another output replaces LastResult.
            interveningResult = element("feFlood")
                    .attributes("x='90' y='10' width='10' height='10' flood-color='red'").build();
        }
        // Later passes overwrite both aliases with a different region.
        String primitives = input.build() + interveningResult + blur.build()
                + element("feOffset").attributes("result='tile'").build();
        String artwork = element("g").attributes("filter='url(#f)'").children(tile(32, 24)).build();
        String svg = filterDocument(144, 80, primitives, artwork, "", "color-interpolation-filters='sRGB'");
        // Explicitly continue the artwork, so the reference needs only transparent-edge blur.
        boolean horizontal = hasBlurOnAxis(deviation, 0);
        boolean vertical = hasBlurOnAxis(deviation, 1);
        String reference = filterDocument(144, 80,
                element("feGaussianBlur")
                        .attributes(Map.of("stdDeviation", deviation, "edgeMode", "none")).build(),
                element("g").attributes("filter='url(#f)'")
                        .children(continuedTile(mode, horizontal, vertical)).build(),
                "x='-100' y='-100' width='344' height='280'", "color-interpolation-filters='sRGB'");
        compareSvg("edge-mode-subregion-" + mode + "-" + deviation + "-" + named, svg, reference);
    }

    @TestFactory
    Stream<DynamicTest> chainedPrimitivePreservesOriginalInput() {
        return Stream.of("duplicate", "wrap").map(mode -> DynamicTest.dynamicTest(mode + " chained input", () -> {
            String primitives = element("feOffset")
                    .attributes("x='32' y='24' width='24' height='16' result='tile'").build()
                    + element("feDropShadow")
                            .attributes("in='tile' result='tile' dx='0' dy='0' stdDeviation='0' flood-opacity='0'")
                            .build()
                    + element("feGaussianBlur")
                            .attributes("x='0' y='0' width='144' height='80' stdDeviation='1.5'")
                            .attributes(Map.of("edgeMode", mode)).build();
            String svg = filterDocument(144, 80, primitives,
                    element("g").attributes("filter='url(#f)'").children(tile(32, 24)).build(), "",
                    "color-interpolation-filters='sRGB'");
            String reference = filterDocument(144, 80,
                    element("feGaussianBlur")
                            .attributes("stdDeviation='1.5' edgeMode='none'")
                            .build(),
                    element("g").attributes("filter='url(#f)'").children(continuedTile(mode, true, true)).build(),
                    "x='-100' y='-100' width='344' height='280'", "color-interpolation-filters='sRGB'");
            compareSvg("edge-mode-chained-" + mode, svg, reference);
        }));
    }

    @TestFactory
    Stream<DynamicTest> transparentInputEdgesStayTransparent() {
        return Stream.of("duplicate", "wrap")
                .map(mode -> DynamicTest.dynamicTest(mode + " transparent edge", () -> {
                    String input = element("feOffset")
                            .attributes("x='20' y='12' width='48' height='40' result='artwork'").build()
                            + element("feFlood")
                                    .attributes(
                                            "x='20' y='12' width='48' height='40' flood-opacity='0' result='extent'")
                                    .build()
                            + element("feMerge").attributes("result='input'").children(
                                    element("feMergeNode").attributes("in='extent'"),
                                    element("feMergeNode").attributes("in='artwork'"))
                                    .build();
                    String blur = element("feGaussianBlur").attributes("in='input' stdDeviation='1.5'")
                            .attributes(Map.of("edgeMode", mode)).build();
                    String referenceBlur = element("feGaussianBlur")
                            .attributes("x='20' y='12' width='48' height='40'",
                                    "stdDeviation='1.5' edgeMode='none'")
                            .build();
                    String artwork =
                            element("g").attributes("filter='url(#f)' opacity='0.5'").children(tile(32, 24)).build();
                    compareSvg("edge-mode-transparent-" + mode,
                            filterDocument(144, 80, input + blur, artwork, "", "color-interpolation-filters='sRGB'"),
                            filterDocument(144, 80, input + referenceBlur, artwork, "",
                                    "color-interpolation-filters='sRGB'"));
                }));
    }

    @TestFactory
    Stream<DynamicTest> emptyInputHasNoEdgesToRepeat() {
        return Stream.of("duplicate", "wrap", "none").map(mode -> DynamicTest.dynamicTest(mode + " empty input", () -> {
            String primitives = element("feFlood").attributes("x='32' y='24' width='0' height='16'").build()
                    + element("feGaussianBlur").attributes("x='0' y='0' width='144' height='80' stdDeviation='1.5'")
                            .attributes(Map.of("edgeMode", mode)).build();
            String svg = filterDocument(144, 80, primitives,
                    element("g").attributes("filter='url(#f)'").children(tile(32, 24)).build(), "", "");
            compareSvg("edge-mode-empty-" + mode, svg, wrapTag(144, 80, ""));
        }));
    }

    private static void compareSvg(String name, String svg, String reference) {
        try {
            BufferedImage sizeHint = new BufferedImage(TEST_WIDTH, TEST_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            BufferedImage expectedImage = expected(
                    new ImageSource.MemoryImageSource(name + "-ref", reference), RenderType.JSVG).render(sizeHint);
            BufferedImage actualImage = actual(
                    new ImageSource.MemoryImageSource(name, svg), RenderType.JSVG).render(expectedImage);
            assertEquals(SUCCESS, compareImageRasterization(
                    expectedImage, actualImage, name, 0, 2 / 255.0));
        } catch (IOException e) {
            throw new AssertionError("Could not render " + name, e);
        }
    }

    private static String tile(int x, int y) {
        return element("g").children(
                rectangle(x, y, 12, 8, 0xffe08040),
                rectangle(x + 12, y, 12, 8, 0xff40c080),
                rectangle(x, y + 8, 12, 8, 0xff204060),
                rectangle(x + 12, y + 8, 12, 8, 0xff8040c0)).build();
    }

    private static boolean hasBlurOnAxis(String deviation, int axis) {
        String[] values = deviation.split(" ");
        String value = values.length == 1 ? values[0] : values[axis];
        return Double.parseDouble(value) > 0;
    }

    private static String continuedTile(String mode, boolean horizontal, boolean vertical) {
        if (mode.equals("none")) return tile(32, 24);
        ElementBuilder artwork = element("g");
        if (mode.equals("duplicate")) {
            int left = horizontal ? -100 : 32;
            int right = horizontal ? 244 : 56;
            int top = vertical ? -100 : 24;
            int bottom = vertical ? 180 : 40;
            artwork.children(
                    rectangle(left, top, 44 - left, 32 - top, 0xffe08040),
                    rectangle(44, top, right - 44, 32 - top, 0xff40c080),
                    rectangle(left, 32, 44 - left, bottom - 32, 0xff204060),
                    rectangle(44, 32, right - 44, bottom - 32, 0xff8040c0));
        } else {
            int firstY = vertical ? 24 - 16 * 8 : 24;
            int bottom = vertical ? 180 : 40;
            int firstX = horizontal ? 32 - 24 * 6 : 32;
            int right = horizontal ? 244 : 56;
            for (int y = firstY; y < bottom; y += 16) {
                for (int x = firstX; x < right; x += 24) {
                    artwork.children(tile(x, y));
                }
            }
        }
        return artwork.build();
    }

    private static void compare(String mode, String deviation, String artwork, String region) {
        String primitive = element("feGaussianBlur")
                .attributes(Map.of("stdDeviation", deviation, "edgeMode", mode)).build();
        String referencePrimitive = element("feGaussianBlur").attributes(Map.of("stdDeviation", deviation)).build();
        String svg = filterDocument(144, 80, primitive, artwork, region, "color-interpolation-filters='sRGB'");
        // Batik does not implement Gaussian blur's edgeMode attribute. The reference instead uses
        // ordinary Gaussian blur on artwork that explicitly supplies the required surrounding colors.
        String reference = filterDocument(144, 80, referencePrimitive, artwork, region,
                "color-interpolation-filters='sRGB'");
        String name = "edge-mode-" + mode + "-" + deviation;
        // Allow small rounding differences between the two renderers' blur implementations.
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new ImageSource.MemoryImageSource(name + "-ref", reference),
                        RenderType.Batik.withViewportSize(TEST_WIDTH, TEST_HEIGHT)),
                actual(new ImageSource.MemoryImageSource(name, svg), RenderType.JSVG), 0.002, 2 / 255.0)));
    }
}
