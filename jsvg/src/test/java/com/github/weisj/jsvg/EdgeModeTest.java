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
package com.github.weisj.jsvg;

import static com.github.weisj.jsvg.ImageComparison.*;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class EdgeModeTest {
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
        String reference = filterDocument(144, 80,
                element("feGaussianBlur").attributes(Map.of("stdDeviation", deviation, "edgeMode", "none")).build(),
                element("g").attributes("filter='url(#f)'").children(continuedTile(mode)).build(),
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
                    element("feGaussianBlur").attributes("stdDeviation='1.5' edgeMode='none'").build(),
                    element("g").attributes("filter='url(#f)'").children(continuedTile(mode)).build(),
                    "x='-100' y='-100' width='344' height='280'", "color-interpolation-filters='sRGB'");
            compareSvg("edge-mode-chained-" + mode, svg, reference);
        }));
    }

    @TestFactory
    Stream<DynamicTest> transparentInputEdgesStayTransparent() {
        return Stream.of("duplicate", "wrap")
                .map(mode -> DynamicTest.dynamicTest(mode + " transparent edge", () -> {
                    String input = element("feOffset").attributes("x='20' y='12' width='48' height='40'").build();
                    String blur = element("feGaussianBlur").attributes("stdDeviation='1.5'")
                            .attributes(Map.of("edgeMode", mode)).build();
                    String referenceBlur =
                            element("feGaussianBlur").attributes("stdDeviation='1.5' edgeMode='none'").build();
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
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new ImageSource.MemoryImageSource(name + "-ref", reference), RenderType.JSVG),
                actual(new ImageSource.MemoryImageSource(name, svg), RenderType.JSVG), 0, 2 / 255.0)));
    }

    private static String tile(int x, int y) {
        return element("g").children(
                rectangle(x, y, 12, 8, 0xffe08040),
                rectangle(x + 12, y, 12, 8, 0xff40c080),
                rectangle(x, y + 8, 12, 8, 0xff204060),
                rectangle(x + 12, y + 8, 12, 8, 0xff8040c0)).build();
    }

    private static String continuedTile(String mode) {
        if (mode.equals("none")) return tile(32, 24);
        ElementBuilder artwork = element("g");
        if (mode.equals("duplicate")) {
            artwork.children(
                    rectangle(-100, -100, 144, 132, 0xffe08040),
                    rectangle(44, -100, 200, 132, 0xff40c080),
                    rectangle(-100, 32, 144, 148, 0xff204060),
                    rectangle(44, 32, 200, 148, 0xff8040c0));
        } else {
            for (int y = 24 - 16 * 8; y < 180; y += 16) {
                for (int x = 32 - 24 * 6; x < 244; x += 24) {
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
                expected(new ImageSource.MemoryImageSource(name + "-ref", reference), RenderType.Batik),
                actual(new ImageSource.MemoryImageSource(name, svg), RenderType.JSVG), 0, 2 / 255.0)));
    }
}
