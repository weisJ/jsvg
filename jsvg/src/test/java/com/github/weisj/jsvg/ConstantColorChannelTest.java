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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    private static ElementBuilder constant(int color) {
        String values = "0 0 0 0 " + ((color >>> 16) & 0xff) / 255.0
                + " 0 0 0 0 " + ((color >>> 8) & 0xff) / 255.0
                + " 0 0 0 0 " + (color & 0xff) / 255.0
                + " 0 0 0 0 " + (color >>> 24) / 255.0;
        return element("feColorMatrix").attributes(Map.of("values", values));
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
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            document.render(null, graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
