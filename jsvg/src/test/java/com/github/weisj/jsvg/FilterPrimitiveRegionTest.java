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

import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
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

class FilterPrimitiveRegionTest {
    private static final String REGION = "x='20' y='20' width='20' height='20' result='clipped'";
    private static final String SOURCE = "<rect x='5' y='5' width='75' height='75' fill='red' filter='url(#f)'/>";
    private static final String EXPAND_BUFFER = "<feFlood flood-opacity='0'/><feMerge>"
            + "<feMergeNode in='clipped'/><feMergeNode/></feMerge>";

    @TestFactory
    Stream<DynamicTest> clipsPrimitiveBeforeItsResultIsReused() {
        return Stream.of(
                element("feFlood"),
                element("feOffset"),
                element("feGaussianBlur").attributes("stdDeviation='0'"),
                element("feGaussianBlur").attributes("stdDeviation='2'"),
                element("feColorMatrix"),
                element("feColorMatrix").attributes("type='saturate' values='0'"),
                element("feComponentTransfer"),
                element("feComponentTransfer").children(element("feFuncA").attributes("type='linear' slope='0.5'")),
                element("feComposite").attributes("in2='SourceAlpha'"),
                element("feBlend").attributes("in2='SourceAlpha'"),
                element("feMerge").children(element("feMergeNode").attributes("in='SourceGraphic'")),
                element("feDisplacementMap").attributes("in2='SourceAlpha' scale='0'"),
                element("feDisplacementMap").attributes("in2='SourceAlpha' scale='6'"),
                element("feDiffuseLighting").children(element("feDistantLight").attributes("elevation='90'")),
                element("feTurbulence").attributes("type='fractalNoise' baseFrequency='0.05'"),
                element("feDropShadow").attributes("stdDeviation='0' dx='0' dy='0'"))
                .map(primitive -> DynamicTest.dynamicTest(primitive.build(), () -> {
                    BufferedImage image = render(primitive.attributes(REGION).build() + EXPAND_BUFFER, SOURCE, "");
                    assertTrue((image.getRGB(30, 30) >>> 24) > 0);
                    for (int y = 0; y < 100; y++) {
                        for (int x = 0; x < 100; x++) {
                            if (x < 20 || x >= 40 || y < 20 || y >= 40) {
                                assertEquals(0, image.getRGB(x, y) >>> 24, "Outside region at " + x + ", " + y);
                            }
                        }
                    }
                }));
    }

    @TestFactory
    Stream<DynamicTest> emptyOutputDoesNotRenderTheSource() {
        return Stream.of(
                "<feFlood width='0'/>",
                "<feFlood x='200' width='20'/>",
                "<feTurbulence width='0'/>",
                "<feColorMatrix width='0'/>",
                "<feColorMatrix width='-10'/>").map(primitive -> DynamicTest.dynamicTest(primitive, () -> {
                    BufferedImage image = render(primitive, SOURCE, "");
                    for (int y = 0; y < 100; y++) {
                        for (int x = 0; x < 100; x++) {
                            assertEquals(0, image.getRGB(x, y) >>> 24);
                        }
                    }
                }));
    }

    @Test
    void repeatedClippingDoesNotApplyAntialiasingTwice() {
        String region = "x='20.5' y='20.5' width='20' height='20' result='clipped'";
        String flood = element("feFlood").attributes("flood-opacity='0.5'", region).build();
        BufferedImage expected = render(flood + EXPAND_BUFFER, SOURCE, "");
        BufferedImage image =
                render(flood + element("feOffset").attributes(region).build() + EXPAND_BUFFER, SOURCE, "");
        assertArrayEquals(expected.getRGB(0, 0, 100, 100, null, 0, 100),
                image.getRGB(0, 0, 100, 100, null, 0, 100));
        assertEquals(128, image.getRGB(30, 30) >>> 24);
        assertEquals(64, image.getRGB(20, 30) >>> 24);
        assertEquals(32, image.getRGB(20, 20) >>> 24);
        assertEquals(0, image.getRGB(19, 30) >>> 24);
    }

    @TestFactory
    Stream<DynamicTest> clipsTransformedPrimitiveRegions() {
        return Stream.of(
                AffineTransform.getTranslateInstance(10, 15),
                new AffineTransform(-1, 0, 0, 1, 70, 0),
                AffineTransform.getRotateInstance(Math.toRadians(25), 50, 50),
                new AffineTransform(1, 0.25, 0.5, 1, 0, 0),
                new AffineTransform(1.5, 0, 0, 0.75, 10, 5))
                .map(transform -> DynamicTest.dynamicTest(transform.toString(), () -> {
                    String matrix = "matrix(" + transform.getScaleX() + "," + transform.getShearY() + ","
                            + transform.getShearX() + "," + transform.getScaleY() + ","
                            + transform.getTranslateX() + "," + transform.getTranslateY() + ")";
                    BufferedImage image = render(element("feFlood").attributes(REGION).build() + EXPAND_BUFFER,
                            element("g").attributes(Map.of("transform", matrix)).children(SOURCE).build(), "");
                    Shape region = transform.createTransformedShape(new Rectangle(20, 20, 20, 20));
                    for (int y = 0; y < 100; y++) {
                        for (int x = 0; x < 100; x++) {
                            if (region.contains(x, y, 1, 1)) {
                                assertEquals(255, image.getRGB(x, y) >>> 24, "Inside at " + x + ", " + y);
                            } else if (!region.intersects(x, y, 1, 1)) {
                                assertEquals(0, image.getRGB(x, y) >>> 24, "Outside at " + x + ", " + y);
                            }
                        }
                    }
                }));
    }

    @TestFactory
    Stream<DynamicTest> clipsSourceChannelsBeforeSampling() {
        return Stream.of("SourceGraphic", "SourceAlpha")
                .map(input -> DynamicTest.dynamicTest(input, () -> {
                    BufferedImage image =
                            render(element("feOffset").attributes(Map.of("in", input, "dx", 15)).build(), SOURCE,
                                    "x='20' y='20' width='20' height='20'");
                    assertEquals(0, image.getRGB(25, 30) >>> 24);
                    assertEquals(255, image.getRGB(35, 30) >>> 24);
                }));
    }

    @Test
    void defaultSourceSubregionIsTheFilterRegion() {
        BufferedImage image = render("<feOffset in='SourceGraphic' dx='10'/>",
                "<rect x='20' y='20' width='10' height='10' fill='red' filter='url(#f)'/>", "");
        assertEquals(0xffff0000, image.getRGB(35, 25));
    }

    private static BufferedImage render(String primitives, String source, String filterRegion) {
        String svg = filterDocument(100, 100, primitives, source, filterRegion, "");
        SVGDocument document = new SVGLoader().load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null, LoaderContext.builder().build());
        assertNotNull(document);
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        document.render(null, graphics);
        graphics.dispose();
        return image;
    }
}
