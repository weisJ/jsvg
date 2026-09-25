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
import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;

class FeConvolveMatrixTest {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 500;

    @Test
    void identityKernelPreservesItsInput() {
        String artwork = artwork("filter='url(#f)'");
        String actualSvg = filterDocument(WIDTH, HEIGHT,
                element("feConvolveMatrix")
                        .attributes("kernelMatrix='0 0 0 0 1 0 0 0 0'", "color-interpolation-filters='sRGB'")
                        .build(),
                artwork, "", "");
        compareSvg("convolve-identity", actualSvg, wrapTag(WIDTH, HEIGHT, artwork("")));
    }

    @Test
    void reversedKernelAndKernelUnitLengthSelectTheExpectedNeighbor() {
        String primitive = element("feConvolveMatrix")
                .attributes("order='2 1' targetX='0' kernelMatrix='1 0'",
                        "kernelUnitLength='20 1' edgeMode='none' color-interpolation-filters='sRGB'")
                .build();
        String source = element("g").attributes("filter='url(#f)'").children(
                rectangle(220, 170, 80, 160, 0xff204060),
                rectangle(300, 170, 80, 160, 0xffe08040)).build();
        String reference = wrapTag(WIDTH, HEIGHT, element("g").children(
                rectangle(200, 170, 80, 160, 0xff204060),
                rectangle(280, 170, 80, 160, 0xffe08040)).build());
        compareSvg("convolve-kernel-direction", filterDocument(WIDTH, HEIGHT,
                primitive, source, "", ""), reference);
    }

    @Test
    void awtPathMatchesExactPathForCustomTargetAndSignedKernel() {
        String commonAttributes = "order='3' targetX='0' targetY='2' "
                + "kernelMatrix='0 -1 0 -1 5 -1 0 -1 0' edgeMode='duplicate' "
                + "color-interpolation-filters='sRGB'";
        String awtFilter = element("feConvolveMatrix")
                .attributes(commonAttributes)
                .build();
        // With alpha constant over the complete input, preserveAlpha produces the same values but
        // deliberately selects the general fallback so the two convolution engines can be compared.
        String exactFilter = element("feConvolveMatrix")
                .attributes(commonAttributes, "preserveAlpha='true'")
                .build();
        String source = element("g").attributes("filter='url(#f)'").children(
                element("rect").attributes(
                        "x='0' y='0' width='200' height='500' fill='#204060' fill-opacity='0.5'").build(),
                element("rect").attributes(
                        "x='200' y='0' width='200' height='500' fill='#c04020' fill-opacity='0.5'").build(),
                element("rect").attributes(
                        "x='400' y='0' width='200' height='500' fill='#40c080' fill-opacity='0.5'").build())
                .build();

        compareSvg("convolve-awt-fast-path",
                filterDocument(WIDTH, HEIGHT, awtFilter, source, "", ""),
                filterDocument(WIDTH, HEIGHT, exactFilter, source, "", ""));
    }

    @Test
    void evenOrderDefaultTargetDoesNotShiftTheAwtResult() {
        String artwork = artwork("filter='url(#f)'");
        String actualSvg = filterDocument(WIDTH, HEIGHT,
                element("feConvolveMatrix")
                        .attributes("order='2 1' kernelMatrix='1 0'", "color-interpolation-filters='sRGB'")
                        .build(),
                artwork, "", "");
        compareSvg("convolve-even-default-target", actualSvg, wrapTag(WIDTH, HEIGHT, artwork("")));
    }

    @Test
    void numericallyZeroKernelSumUsesOneAsTheDefaultDivisor() {
        String kernel = "kernelMatrix='0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 -0.8'";
        String defaultDivisor = element("feConvolveMatrix")
                .attributes(kernel, "color-interpolation-filters='sRGB'")
                .build();
        String explicitDivisor = element("feConvolveMatrix")
                .attributes(kernel, "divisor='1'", "color-interpolation-filters='sRGB'")
                .build();
        String source = artwork("filter='url(#f)'");

        compareSvg("convolve-zero-sum-divisor",
                filterDocument(WIDTH, HEIGHT, defaultDivisor, source, "", ""),
                filterDocument(WIDTH, HEIGHT, explicitDivisor, source, "", ""));
    }

    @Test
    void zeroDivisorUsesTheDefaultDivisor() {
        String kernel = "kernelMatrix='0 1 0 1 4 1 0 1 0'";
        String defaultDivisor = element("feConvolveMatrix")
                .attributes(kernel, "color-interpolation-filters='sRGB'")
                .build();
        String zeroDivisor = element("feConvolveMatrix")
                .attributes(kernel, "divisor='0'", "color-interpolation-filters='sRGB'")
                .build();
        String source = artwork("filter='url(#f)'");

        compareSvg("convolve-zero-divisor",
                filterDocument(WIDTH, HEIGHT, zeroDivisor, source, "", ""),
                filterDocument(WIDTH, HEIGHT, defaultDivisor, source, "", ""));
    }

    @TestFactory
    Stream<DynamicTest> edgeModesExtendTheLogicalInputRegion() {
        return Stream.of("duplicate", "wrap", "mirror").map(mode -> DynamicTest.dynamicTest(mode, () -> {
            String primitives = element("feFlood")
                    .attributes("x='250' y='200' width='100' height='100' flood-color='#40c080' result='sample'")
                    .build()
                    + element("feConvolveMatrix")
                            .attributes("in='sample' x='0' y='0' width='600' height='500'",
                                    "kernelMatrix='0 0 0 0 1 0 0 0 0' color-interpolation-filters='sRGB'",
                                    "edgeMode='" + mode + "'")
                            .build();
            String source = rectangle(0, 0, WIDTH, HEIGHT, 0xff000000).replace("/>",
                    " filter='url(#f)'/>");
            String expectedSvg = wrapTag(WIDTH, HEIGHT,
                    rectangle(0, 0, WIDTH, HEIGHT, 0xff40c080));
            compareSvg("convolve-edge-" + mode,
                    filterDocument(WIDTH, HEIGHT, primitives, source, "", ""), expectedSvg);
        }));
    }

    @Test
    void preserveAlphaUsesUnpremultipliedColors() {
        String primitive = element("feConvolveMatrix")
                .attributes("in='FillPaint' order='1' kernelMatrix='0' divisor='1' bias='1'",
                        "preserveAlpha='true' color-interpolation-filters='sRGB'")
                .build();
        String source = element("rect")
                .attributes("width='600' height='500' fill='#204060' fill-opacity='0.5' filter='url(#f)'")
                .build();
        String reference = wrapTag(WIDTH, HEIGHT,
                element("rect").attributes("width='600' height='500' fill='white' fill-opacity='0.5'").build());
        compareSvg("convolve-preserve-alpha",
                filterDocument(WIDTH, HEIGHT, primitive, source, "", ""), reference);
    }

    @Test
    void biasUsesTheConvolvedAlphaForPremultipliedColors() {
        String primitive = element("feConvolveMatrix")
                .attributes("order='1' kernelMatrix='0' divisor='1' bias='0.5'",
                        "color-interpolation-filters='sRGB'")
                .build();
        String source = element("rect")
                .attributes("width='600' height='500' fill='black' filter='url(#f)'")
                .build();
        String reference = wrapTag(WIDTH, HEIGHT,
                element("rect").attributes("width='600' height='500' fill='#808080' fill-opacity='0.5'").build());
        compareSvg("convolve-bias-alpha",
                filterDocument(WIDTH, HEIGHT, primitive, source, "", ""), reference);
    }

    @Test
    void invalidKernelMatrixIsAPassThroughFilter() {
        String artwork = artwork("filter='url(#f)'");
        String actualSvg = filterDocument(WIDTH, HEIGHT,
                element("feConvolveMatrix").attributes("order='3' kernelMatrix='1 0 0'").build(),
                artwork, "", "");
        compareSvg("convolve-invalid-kernel", actualSvg, wrapTag(WIDTH, HEIGHT, artwork("")));
    }

    private static String artwork(String attributes) {
        return element("g").attributes(attributes).children(
                rectangle(80, 70, 180, 160, 0xff204060),
                rectangle(210, 180, 240, 190, 0xffe08040),
                rectangle(380, 80, 140, 180, 0xff40c080)).build();
    }

    private static void compareSvg(String name, String svg, String reference) {
        try {
            BufferedImage sizeHint = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            BufferedImage expectedImage = expected(
                    new MemoryImageSource(name + "-ref", reference), RenderType.JSVG).render(sizeHint);
            BufferedImage actualImage = actual(
                    new MemoryImageSource(name, svg), RenderType.JSVG).render(expectedImage);
            assertEquals(SUCCESS, compareImageRasterization(
                    expectedImage, actualImage, name, 0, 1 / 255.0));
        } catch (IOException e) {
            throw new AssertionError("Could not render " + name, e);
        }
    }
}
