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
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.sun.management.ThreadMXBean;

class ConstantColorChannelTest {
    private static final int SIZE = 100;
    private static final String NON_UNIFORM_SOURCE = element("g").attributes("filter='url(#f)'").children(
            rectangle(10, 10, 25, 25, 0xffff0000), rectangle(55, 60, 30, 20, 0xff0000ff)).build();

    private static final String SOURCE = element("rect")
            .attributes("width='100' height='100' fill='red' filter='url(#f)'").build();

    @TestFactory
    Stream<DynamicTest> rasterizesUniformColors() {
        return Stream.of(0xffff0000, 0x80336699, 0x00336699).map(color -> DynamicTest.dynamicTest(
                Integer.toHexString(color), () -> {
                    String filtered = filterDocument(100, 100, constant(color).build(), SOURCE, "",
                            "color-interpolation-filters='sRGB'");
                    assertRenderedEquals(wrapTag(100, 100, coloredRectangle(color).build()), filtered);
                }));
    }

    @Test
    void producerRetainsColorWhenAlphaIsZero() {
        String primitives = constant(0x00336699).build() + element("feComponentTransfer")
                .children(element("feFuncA").attributes("type='linear' slope='0' intercept='1'")).build();
        assertRenderedEquals(wrapTag(100, 100, coloredRectangle(0xff336699).build()),
                filterDocument(100, 100, primitives, SOURCE, "", "color-interpolation-filters='sRGB'"));
    }

    @Test
    void filteredProducerRetainsItsClip() {
        String primitives = constant(0x80336699)
                .attributes("x='20' y='20' width='30' height='30'").build()
                + element("feOffset").attributes("dx='10' x='30' y='20' width='30' height='30'").build();
        String reference = coloredRectangle(0x80336699, "x='30' y='20' width='30' height='30'").build();
        assertRenderedEquals(wrapTag(100, 100, reference),
                filterDocument(100, 100, primitives, SOURCE, "", "color-interpolation-filters='sRGB'"));
    }

    @Test
    void repeatedFractionalClippingPreservesAntialiasing() {
        String region = "x='20.5' y='20.5' width='20' height='20'";
        String primitives = constant(0x80000000).attributes(region).build()
                + element("feOffset").attributes(region).build();
        String reference = coloredRectangle(0x80000000, region).build();
        assertRenderedEquals(wrapTag(100, 100, reference),
                filterDocument(100, 100, primitives, SOURCE, "", "color-interpolation-filters='sRGB'"));
    }

    @Test
    void alphaOnlyBlurOfUniformColorMatchesArtwork() {
        String shadow = element("feDropShadow")
                .attributes("stdDeviation='1' dx='5' dy='5' flood-color='blue'").build();
        String reference = filterDocument(100, 100, shadow,
                coloredRectangle(0x80336699).attributes("filter='url(#f)'").build(), "",
                "color-interpolation-filters='sRGB'");
        String actual = filterDocument(100, 100, constant(0x80336699).build() + shadow, SOURCE, "",
                "color-interpolation-filters='sRGB'");
        assertRenderedEquals(reference, actual);
    }

    @Test
    void samplingUniformDisplacementAvoidsPixelBuffersAndPerPixelAllocations() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof ThreadMXBean);
        ThreadMXBean allocations = (ThreadMXBean) bean;
        assumeTrue(allocations.isThreadAllocatedMemorySupported());
        allocations.setThreadAllocatedMemoryEnabled(true);
        int size = 512;
        String primitives = constant(0xffff0000).attributes("result='control'").build()
                + element("feDisplacementMap")
                        .attributes(
                                "in='SourceGraphic' in2='control' scale='10' xChannelSelector='R' yChannelSelector='G'")
                        .build();
        String source = element("rect").attributes(Map.of("width", size, "height", size))
                .attributes("fill='blue' filter='url(#f)'").build();
        SVGDocument document = load(filterDocument(size, size, primitives, source, "",
                "color-interpolation-filters='sRGB'"));
        for (int i = 0; i < 3; i++) {
            render(document, size);
        }
        long before = allocations.getTotalThreadAllocatedBytes();
        assumeTrue(before >= 0);
        BufferedImage actual = render(document, size);
        long allocated = allocations.getTotalThreadAllocatedBytes() - before;
        // Allow the source, destination and filter buffers plus conservative overhead, but reject
        // allocating a color array for every sample or rasterizing the uniform control image.
        long budget = 48L * size * size;
        assertTrue(allocated <= budget,
                () -> "Uniform displacement allocated " + allocated + " bytes (allowed " + budget + ")");
        String reference = wrapTag(size, size, element("rect")
                .attributes(Map.of("x", 0, "y", 5, "width", size - 5, "height", size - 5))
                .attributes("fill='blue'").build());
        assertPixelsEqual(render(load(reference), size), actual);
    }

    private static ElementBuilder matrix(String values) {
        return element("feColorMatrix").attributes(Map.of("values", values));
    }

    private static ElementBuilder constantTransfer(String type) {
        ElementBuilder transfer = element("feComponentTransfer");
        String[] channels = {"R", "G", "B", "A"};
        String[] values = {".2", ".4", ".6", "1"};
        for (int i = 0; i < channels.length; i++) {
            ElementBuilder function = element("feFunc" + channels[i]).attributes(Map.of("type", type));
            switch (type) {
                case "table", "discrete" -> function.attributes(Map.of("tableValues", values[i] + " " + values[i]));
                case "linear" -> function.attributes("slope='0'").attributes(Map.of("intercept", values[i]));
                case "gamma" -> function.attributes("amplitude='0'").attributes(Map.of("offset", values[i]));
                default -> throw new IllegalArgumentException(type);
            }
            transfer.children(function);
        }
        return transfer;
    }

    @TestFactory
    Stream<DynamicTest> inputIndependentOperationsProduceTheirColorThroughoutTheRegion() {
        return Stream.of("sRGB", "linearRGB").flatMap(space -> Stream.of(
                constant(0xff336699), constantTransfer("table"), constantTransfer("discrete"),
                constantTransfer("linear"), constantTransfer("gamma"))
                .map(primitive -> DynamicTest.dynamicTest(space + " " + primitive.build(), () -> {
                    String svg = document(primitive.build(), NON_UNIFORM_SOURCE, space);
                    int color = space.equals("sRGB") ? 0xff336699 : 0xff7caacb;
                    assertReference(svg, wrapTag(SIZE, SIZE, rectangle(0, 0, SIZE, SIZE, color)), 0);
                })));
    }

    @TestFactory
    Stream<DynamicTest> foldsPointOperationsLikeTheSameOperationsOnUniformArtwork() {
        return Stream.of("sRGB", "linearRGB").flatMap(space -> Stream.of(
                matrix(".5 0 0 .1 0 0 .5 0 0 .2 0 0 .5 0 0 0 0 0 .5 0"),
                element("feColorMatrix").attributes("type='saturate' values='.3'"),
                element("feColorMatrix").attributes("type='hueRotate' values='45'"),
                element("feColorMatrix").attributes("type='luminanceToAlpha'"),
                element("feComponentTransfer").children(
                        element("feFuncR").attributes("type='table' tableValues='1 .5 0'"),
                        element("feFuncG").attributes("type='gamma' amplitude='.7' exponent='2' offset='.1'"),
                        element("feFuncA").attributes("type='linear' slope='.5'")))
                .map(operation -> DynamicTest.dynamicTest(space + " " + operation.build(), () -> {
                    // The seed is explicitly sRGB; the operation then chooses its own color space.
                    String seed = constant(0xff336699).attributes("color-interpolation-filters='sRGB'").build();
                    String referenceSource = element("rect")
                            .attributes("width='100' height='100' fill='#336699' filter='url(#f)'").build();
                    assertReference(document(seed + operation.build(), NON_UNIFORM_SOURCE, space),
                            document(operation.build(), referenceSource, space), 0);
                })));
    }

    @TestFactory
    Stream<DynamicTest> clippedUniformInputKeepsItsTransparentExterior() {
        return Stream.of(
                matrix("0 1 0 0 0 1 0 0 0 0 0 0 1 0 0 0 0 0 -1 1"),
                element("feComponentTransfer").children(
                        element("feFuncA").attributes("type='linear' slope='-1' intercept='1'")))
                .map(operation -> DynamicTest.dynamicTest(operation.build(), () -> {
                    String region = "x='20' y='30' width='40' height='20'";
                    String seed = constant(0xff336699).attributes(region).build();
                    String referenceSource = element("rect").attributes(region,
                            "fill='#336699' filter='url(#f)'").build();
                    operation.attributes("x='0' y='0' width='100' height='100'");
                    assertReference(document(seed + operation.build(), NON_UNIFORM_SOURCE, "sRGB"),
                            document(operation.build(), referenceSource, "sRGB"), 0);
                }));
    }

    @Test
    void transparentConstantPreservesItsColorForAnAlphaGeneratingOperation() throws IOException {
        String seed = matrix("0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0").build();
        String restoreAlpha = element("feComponentTransfer").children(
                element("feFuncA").attributes("type='linear' slope='0' intercept='1'")).build();
        assertReference(document(seed + restoreAlpha, NON_UNIFORM_SOURCE, "sRGB"),
                wrapTag(SIZE, SIZE, rectangle(0, 0, SIZE, SIZE, 0xffff0000)), 0);
    }

    @TestFactory
    Stream<DynamicTest> constantChainsAvoidIntermediateImageAllocations() {
        return Stream.of(constant(0xff336699), constantTransfer("table"))
                .map(seed -> DynamicTest.dynamicTest(seed.build(), () -> {
                    java.lang.management.ThreadMXBean platformBean = ManagementFactory.getThreadMXBean();
                    assumeTrue(platformBean instanceof ThreadMXBean);
                    ThreadMXBean allocations = (ThreadMXBean) platformBean;
                    assumeTrue(allocations.isThreadAllocatedMemorySupported());
                    allocations.setThreadAllocatedMemoryEnabled(true);
                    assumeTrue(allocations.getTotalThreadAllocatedBytes() >= 0);

                    int size = 512;
                    String invert = element("feComponentTransfer").children(
                            element("feFuncR").attributes("type='linear' slope='-1' intercept='1'"),
                            element("feFuncG").attributes("type='linear' slope='-1' intercept='1'"),
                            element("feFuncB").attributes("type='linear' slope='-1' intercept='1'")).build();
                    String swap = matrix("0 0 1 0 0 0 1 0 0 0 1 0 0 0 0 0 0 0 1 0").build();
                    String svg = filterDocument(size, size, seed.build() + (invert + swap).repeat(16),
                            NON_UNIFORM_SOURCE, "", "color-interpolation-filters='sRGB'");
                    SVGDocument document = load(svg);
                    for (int i = 0; i < 4; i++) {
                        render(document, size, RenderingHints.VALUE_ANTIALIAS_OFF);
                    }
                    long before = allocations.getTotalThreadAllocatedBytes();
                    BufferedImage image = render(document, size, RenderingHints.VALUE_ANTIALIAS_OFF);
                    long bytes = allocations.getTotalThreadAllocatedBytes() - before;
                    // Allow eight full rasters and fixed overhead, but reject a raster per point operation.
                    long budget = 8L * size * size * Integer.BYTES + 2_000_000;
                    assertTrue(bytes <= budget,
                            () -> "Constant chain allocated " + bytes + " bytes (budget " + budget + ")");
                    BufferedImage reference = render(load(wrapTag(size, size,
                            rectangle(0, 0, size, size, 0xff336699))), size, RenderingHints.VALUE_ANTIALIAS_OFF);
                    assertEquals(SUCCESS, compareImageRasterization(reference, image, "constant-chain", 0, 0));
                }));
    }

    private static String document(String primitives, String source, String space) {
        return filterDocument(SIZE, SIZE, primitives, source, "", "color-interpolation-filters='" + space + "'");
    }

    private static void assertReference(String svg, String referenceSvg, double tolerance) throws IOException {
        BufferedImage image = render(load(svg), SIZE, RenderingHints.VALUE_ANTIALIAS_OFF);
        BufferedImage reference = expected(new ImageSource.MemoryImageSource("constant-color-reference", referenceSvg),
                RenderType.JSVG).render(null);
        assertEquals(SUCCESS, compareImageRasterization(reference, image, "constant-color", 0, tolerance));
    }

    private static ElementBuilder constant(int color) {
        String values = "0 0 0 0 " + ((color >>> 16) & 0xff) / 255.0
                + " 0 0 0 0 " + ((color >>> 8) & 0xff) / 255.0
                + " 0 0 0 0 " + (color & 0xff) / 255.0
                + " 0 0 0 0 " + (color >>> 24) / 255.0;
        return matrix(values);
    }

    private static ElementBuilder coloredRectangle(int color) {
        return coloredRectangle(color, "width='100' height='100'");
    }

    private static ElementBuilder coloredRectangle(int color, String region) {
        return element("rect").attributes(region)
                .attributes(Map.of("fill", "#" + String.format("%06x", color & 0xffffff),
                        "fill-opacity", (color >>> 24) / 255.0));
    }

    private static void assertRenderedEquals(String reference, String actual) {
        assertPixelsEqual(render(load(reference), 100), render(load(actual), 100));
    }

    private static void assertPixelsEqual(BufferedImage reference, BufferedImage actual) {
        int width = reference.getWidth();
        int height = reference.getHeight();
        assertArrayEquals(reference.getRGB(0, 0, width, height, null, 0, width),
                actual.getRGB(0, 0, width, height, null, 0, width));
    }

    private static SVGDocument load(String svg) {
        SVGDocument document = new SVGLoader().load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null, LoaderContext.builder().build());
        assertNotNull(document);
        return document;
    }

    private static BufferedImage render(SVGDocument document, int size) {
        return render(document, size, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static BufferedImage render(SVGDocument document, int size, Object antialiasing) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
        try {
            document.render(null, graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
