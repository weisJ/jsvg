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
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.Graphics2DOutput;

class GenerativeFilterTest {
    private static final String SMALL = "x='10' y='10' width='10' height='10'";
    private static final String REGION = "x='50' y='50' width='20' height='20'";
    private static final String HUGE = "x='-1000' y='-1000' width='2000' height='2000'";
    private static final String SOURCE = element("rect").attributes(SMALL, "fill='red'", "filter='url(#f)'").build();

    private record Generator(ElementBuilder primitive, int color) {
    }

    private static Stream<Generator> generators() {
        return Stream.of(
                new Generator(element("feColorMatrix").attributes("values='0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1'"),
                        0xffff0000),
                new Generator(element("feComponentTransfer").children(
                        element("feFuncA").attributes("type='linear'", "slope='0'", "intercept='1'")),
                        0xff000000),
                new Generator(element("feComposite").attributes("in2='seed'", "operator='arithmetic'", "k4='1'"),
                        0xffffffff),
                new Generator(
                        element("feDiffuseLighting").children(element("feDistantLight").attributes("elevation='90'")),
                        0xffffffff));
    }

    @TestFactory
    Stream<DynamicTest> generatesPixelsOutsideTheInput() {
        return generators().map(generator -> DynamicTest.dynamicTest(generator.primitive().build(), () -> {
            Rendered rendered = render(element("feFlood").attributes(SMALL, "result='seed'").build()
                    + generator.primitive().attributes(REGION).build(), "", SOURCE);
            assertReference(rendered.image(), "generated-" + generator.color(),
                    rectangle(50, 50, 20, 20, generator.color()));
        }));
    }

    @TestFactory
    Stream<DynamicTest> generatesPixelsFromAnEmptyInput() {
        return generators().map(generator -> DynamicTest.dynamicTest(generator.primitive().build(), () -> {
            Rendered rendered = render("<feFlood width='0' result='seed'/>"
                    + generator.primitive().attributes(REGION).build(), "", SOURCE);
            assertReference(rendered.image(), "empty-input-" + generator.color(),
                    rectangle(50, 50, 20, 20, generator.color()));
        }));
    }

    @TestFactory
    Stream<DynamicTest> transferFunctionsCanGenerateAlpha() {
        return Stream.of("type='table' tableValues='1 1'", "type='discrete' tableValues='1 1'",
                "type='gamma' amplitude='0' offset='1'")
                .map(function -> DynamicTest.dynamicTest(function, () -> {
                    Rendered rendered = render(element("feFlood").attributes(SMALL).build()
                            + element("feComponentTransfer").attributes(REGION)
                                    .children(element("feFuncA").attributes(function)).build(),
                            "", SOURCE);
                    assertReference(rendered.image(), "transfer-" + function.split("'")[1],
                            rectangle(50, 50, 20, 20, 0xff000000));
                }));
    }

    @TestFactory
    Stream<DynamicTest> generatedPixelsAndEarlierResultsCanBeReused() {
        return generators().map(generator -> DynamicTest.dynamicTest(generator.primitive().build(), () -> {
            Rendered rendered = render(element("feFlood").attributes(SMALL, "result='seed'").build()
                    + generator.primitive().attributes(REGION, "result='generated'").build()
                    + "<feMerge><feMergeNode in='seed'/><feMergeNode in='generated'/></feMerge>", "", SOURCE);
            assertReference(rendered.image(), "reused-" + generator.color(),
                    rectangle(10, 10, 10, 10, 0xff000000)
                            + rectangle(50, 50, 20, 20, generator.color()));
        }));
    }

    @TestFactory
    Stream<DynamicTest> generatedPixelsCanBeOffset() {
        return Stream.of(-5, 5).flatMap(
                dx -> generators()
                        .map(generator -> DynamicTest.dynamicTest(generator.primitive().build() + " dx=" + dx, () -> {
                            Rendered rendered = render(element("feFlood").attributes(SMALL, "result='seed'").build()
                                    + generator.primitive().attributes(REGION).build()
                                    + element("feOffset")
                                            .attributes(
                                                    Map.of("dx", dx, "x", 50 + dx, "y", 50, "width", 20, "height", 20))
                                            .build(),
                                    "", SOURCE);
                            assertReference(rendered.image(), "offset-" + dx + "-" + generator.color(),
                                    rectangle(50 + dx, 50, 20, 20, generator.color()));
                        })));
    }

    @TestFactory
    Stream<DynamicTest> transparentBlackPreservingFiltersDoNotAllocateTheirHugeRegion() {
        return Stream.of(
                "<feColorMatrix type='saturate' values='0'/>",
                "<feComponentTransfer><feFuncR type='linear' slope='0'/></feComponentTransfer>",
                "<feComposite in2='SourceGraphic' operator='arithmetic' k2='1'/>",
                "<feComposite in2='SourceGraphic' operator='arithmetic' k2='2' k4='-1'/>")
                .map(primitive -> DynamicTest.dynamicTest(primitive, () -> {
                    Rendered rendered = render(primitive, HUGE, SOURCE);
                    assertAllocationWithin(rendered, 4 * 10 * 10);
                }));
    }

    @Test
    void aChainOfColorOperationsKeepsTheSmallBackingStore() {
        Rendered rendered = render("<feComponentTransfer><feFuncR type='linear' slope='0'/>"
                + "</feComponentTransfer><feColorMatrix type='saturate' values='0'/>"
                + "<feOffset dx='5'/>", HUGE, SOURCE);
        assertAllocationWithin(rendered, 4 * 15 * 10);
    }

    @TestFactory
    Stream<DynamicTest> hugeGenerativeRegionsAreLimitedToTheViewport() {
        return generators().map(generator -> DynamicTest.dynamicTest(generator.primitive().build(), () -> {
            Rendered rendered = render(
                    element("feFlood").attributes(SMALL, "result='seed'").build()
                            + generator.primitive().attributes(HUGE).build(),
                    HUGE, SOURCE);
            assertAllocationWithin(rendered, 4 * 100 * 100);
        }));
    }

    @Test
    void separateFiltersAllocateSeparateSmallBuffers() {
        Rendered rendered = render(
                "<feComponentTransfer><feFuncR type='linear' slope='0'/>" + "</feComponentTransfer>",
                HUGE,
                SOURCE + "<rect x='60' y='60' width='15' height='20' fill='red' filter='url(#f)'/>");
        assertAllocationWithin(rendered, 4 * (10 * 10 + 15 * 20));
    }

    private static void assertAllocationWithin(Rendered rendered, long pixelBudget) {
        assertFalse(rendered.buffers().isEmpty(), "The test must exercise filter image allocation");
        long pixels = rendered.buffers().stream().mapToLong(Long::longValue).sum();
        // Allow padding and conservative bounds, but reject allocations proportional to the huge filter
        // region.
        assertTrue(pixels <= pixelBudget,
                () -> "Needlessly large backing stores: " + pixels + " pixels (max allowed " + pixelBudget + ")");
    }

    private static void assertReference(BufferedImage image, String name, String shapes) throws IOException {
        String svg = wrapTag(100, 100, shapes);
        BufferedImage reference = expected(new ImageSource.MemoryImageSource(name + "-reference", svg),
                RenderType.JSVG).render(null);
        assertEquals(SUCCESS, compareImageRasterization(reference, image, name, 0, 0));
    }

    private record Rendered(BufferedImage image, List<Long> buffers) {
    }

    private static Rendered render(String primitives, String filterRegion, String source) {
        String svg = filterDocument(100, 100, primitives, source, filterRegion,
                "color-interpolation-filters='sRGB'");
        SVGDocument document = new SVGLoader().load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null, LoaderContext.builder().build());
        assertNotNull(document);
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        List<Long> buffers = new ArrayList<>();
        Output output = new RecordingOutput(image.createGraphics(), buffers);
        output.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        try {
            document.renderWithPlatform(NullPlatformSupport.INSTANCE, output, null);
        } finally {
            output.dispose();
        }
        return new Rendered(image, buffers);
    }

    // Inspect the actual filter image being blitted, without depending on the layout representation.
    private static final class RecordingOutput extends Graphics2DOutput {
        private final List<Long> buffers;

        private RecordingOutput(Graphics2D graphics, List<Long> buffers) {
            super(graphics);
            this.buffers = buffers;
        }

        @Override
        public @NotNull Output createChild() {
            return new RecordingOutput((Graphics2D) graphics().create(), buffers);
        }

        @Override
        public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
            assertTrue(image instanceof BufferedImage);
            BufferedImage buffer = (BufferedImage) image;
            buffers.add((long) buffer.getRaster().getDataBuffer().getSize());
            super.drawImage(image, observer);
        }
    }
}
