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
