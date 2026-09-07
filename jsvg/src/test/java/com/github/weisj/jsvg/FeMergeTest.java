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

import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;

class FeMergeTest {
    private static final int SIZE = 100;
    private static final String SOURCE = element("g").attributes("filter='url(#f)'").children(
            rectangle(20, 30, 15, 20, 0xffff0000), rectangle(45, 30, 15, 20, 0xff0000ff)).build();
    private static final String RESTORE_ALPHA = element("feComponentTransfer").children(
            element("feFuncA").attributes("type='linear' slope='0' intercept='1'")).build();

    @TestFactory
    Stream<DynamicTest> emptyMergeProducesTransparentBlack() {
        return Stream.of(
                element("feMerge").build(),
                element("feMerge").children(element("title").children("Empty merge")).build(),
                element("feOffset").attributes("dx='10'").build() + element("feMerge").build())
                .map(primitives -> DynamicTest.dynamicTest(primitives, () -> assertArtwork("", document(primitives))));
    }

    @TestFactory
    Stream<DynamicTest> emptyMergePreservesItsDefaultAndExplicitSubregion() {
        return Stream.of(
                new String[] {"", "x='10' y='15' width='65' height='60'"},
                new String[] {"x='25' width='20'", "x='25' y='15' width='20' height='60'"},
                new String[] {"x='25' y='30' width='20' height='10'", "x='25' y='30' width='20' height='10'"})
                .map(regions -> DynamicTest.dynamicTest("region=" + regions[0], () -> {
                    String primitives = element("feMerge").attributes(regions[0]).build() + RESTORE_ALPHA;
                    String svg = filterDocument(SIZE, SIZE, primitives, SOURCE,
                            "x='10' y='15' width='65' height='60'", "color-interpolation-filters='sRGB'");
                    assertArtwork(element("rect").attributes(regions[1]).build(), svg);
                }));
    }

    @Test
    void emptyMergeResolvesObjectBoundingBoxSubregion() {
        String primitives = element("feMerge").attributes("x='25%' y='25%' width='50%' height='50%'").build()
                + RESTORE_ALPHA;
        String svg = filterDocument(SIZE, SIZE, primitives, SOURCE, "",
                "primitiveUnits='objectBoundingBox' color-interpolation-filters='sRGB'");
        assertArtwork(rectangle(30, 35, 20, 10, 0xff000000), svg);
    }

    @TestFactory
    Stream<DynamicTest> emptyResultCanBeReadByNameOrAsPreviousResult() {
        return Stream.of("", "in='empty'").map(input -> DynamicTest.dynamicTest("input=" + input, () -> {
            String primitives = element("feOffset").attributes("result='saved'").build()
                    + element("feMerge").attributes("result='empty' x='30' y='35' width='20' height='10'").build();
            if (!input.isEmpty()) {
                primitives += element("feOffset").attributes("in='saved' dx='10'").build();
            }
            primitives += element("feComponentTransfer").attributes(input).children(
                    element("feFuncA").attributes("type='linear' slope='0' intercept='1'")).build();
            assertArtwork(rectangle(30, 35, 20, 10, 0xff000000), document(primitives));
        }));
    }

    @TestFactory
    Stream<DynamicTest> emptyMergeLeavesUnrelatedInputsAvailable() {
        return Stream.of("SourceGraphic", "saved").map(input -> DynamicTest.dynamicTest("input=" + input, () -> {
            String primitives = element("feOffset").attributes("result='saved'").build()
                    + element("feMerge").attributes("result='empty'").build()
                    + element("feOffset").attributes(Map.of("in", input, "dx", 15)).build();
            assertArtwork(rectangle(35, 30, 15, 20, 0xffff0000)
                    + rectangle(60, 30, 15, 20, 0xff0000ff), document(primitives));
        }));
    }

    @Test
    void singleMergeNodeDefaultsToThePreviousResult() {
        String primitives = element("feOffset").attributes("dx='15'").build()
                + element("feMerge").children(element("feMergeNode")).build();
        assertArtwork(rectangle(35, 30, 15, 20, 0xffff0000)
                + rectangle(60, 30, 15, 20, 0xff0000ff), document(primitives));
    }

    @TestFactory
    Stream<DynamicTest> validPassThroughPrimitivesKeepTheirInput() {
        return Stream.of(element("feColorMatrix").attributes("values='1 0'"),
                element("feGaussianBlur").attributes("stdDeviation='0'"))
                .map(primitive -> DynamicTest.dynamicTest(primitive.build(),
                        () -> assertArtwork(rectangle(20, 30, 15, 20, 0xffff0000)
                                + rectangle(45, 30, 15, 20, 0xff0000ff), document(primitive.build()))));
    }

    @TestFactory
    Stream<DynamicTest> mergeCompositesInDocumentOrderWithoutChangingItsInputs() {
        return Stream.of(false, true).map(reuse -> DynamicTest.dynamicTest("reuse=" + reuse, () -> {
            String source = element("rect").attributes(
                    "x='10' y='10' width='30' height='30' fill='red' fill-opacity='.5' filter='url(#f)'").build();
            String primitives = element("feOffset").attributes("result='red'").build()
                    + element("feColorMatrix").attributes(
                            "values='0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0' result='blue'").build()
                    + element("feMerge").children(
                            element("feMergeNode").attributes("in='red'"),
                            element("feMergeNode").attributes("in='blue'")).build();
            if (reuse) {
                primitives += element("feOffset").attributes("in='red' dx='40'").build();
            }
            String artwork = reuse
                    ? element("rect").attributes("x='50' y='10' width='30' height='30' fill='red' fill-opacity='.5'")
                            .build()
                    : element("g").attributes("fill-opacity='.5'").children(
                            element("rect").attributes("x='10' y='10' width='30' height='30' fill='red'"),
                            element("rect").attributes("x='10' y='10' width='30' height='30' fill='blue'")).build();
            assertArtwork(artwork, filterDocument(SIZE, SIZE, primitives, source, "",
                    "color-interpolation-filters='sRGB'"));
        }));
    }

    private static String document(String primitives) {
        return filterDocument(SIZE, SIZE, primitives, SOURCE, "", "color-interpolation-filters='sRGB'");
    }

    private static void assertArtwork(String artwork, String svg) {
        BufferedImage reference = render(wrapTag(SIZE, SIZE, artwork));
        BufferedImage actual = render(svg);
        assertArrayEquals(reference.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE),
                actual.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE));
    }

    private static BufferedImage render(String svg) {
        SVGDocument document = new SVGLoader().load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null, LoaderContext.builder().build());
        assertNotNull(document);
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        try {
            document.render(null, graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
