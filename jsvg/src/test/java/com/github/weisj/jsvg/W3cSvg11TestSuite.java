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

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.LongStream;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.w3c.dom.Element;

import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.UrlImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.renderer.animation.AnimationState;

class W3cSvg11TestSuite {
    private static final String W3C_SVG_11_TEST_SUITE_PATH = System.getenv("W3C_SVG_11_TEST_SUITE_PATH");

    @BeforeAll
    static void checkForW3cRepository() {
        assumeTrue(W3C_SVG_11_TEST_SUITE_PATH != null && !W3C_SVG_11_TEST_SUITE_PATH.isBlank()
                && Files.exists(Path.of(W3C_SVG_11_TEST_SUITE_PATH)), """
                        The W3C SVG 1.1 submodule was not found. Skipping W3C SVG 1.1 test suite.
                        Please run `git submodule update --init w3c-svg-11-test-suite` to fetch the submodule.
                        """);
    }

    // As in ReSvgTestSuite, exclusions record current reference mismatches. Normalize the
    // suite's label font for both renderers and retain the standard comparison tolerances.
    private static Collection<DynamicTest> checkCategory(@NotNull String category, @NotNull Set<String> exclude)
            throws IOException {
        Path base = Path.of(W3C_SVG_11_TEST_SUITE_PATH);
        try (var files = Files.list(base.resolve("svg"))) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(category + "-"))
                    .filter(path -> path.toString().endsWith(".svg") || path.toString().endsWith(".svgz"))
                    .filter(path -> !exclude.contains(path.getFileName().toString()))
                    .sorted()
                    .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), new W3cSvg11RefTest(path)))
                    .toList();
        }
    }

    @Disabled("Other animation cases require unsupported features, interaction or have reference mismatches.")
    @TestFactory
    Collection<DynamicTest> animate() throws IOException {
        return checkCategory("animate", Set.of("animate-elem-22-b.svg", "animate-elem-25-t.svg",
                "animate-elem-26-t.svg", "animate-elem-28-t.svg", "animate-elem-88-t.svg"));
    }

    @TestFactory
    Collection<DynamicTest> animate_stroke_inheritance() {
        // This four-second animation fades an inherited stroke from yellow to black, then freezes.
        // Sample the start, intermediate frames, either side of the end, and the frozen state.
        return animationFrames("animate-elem-28-t.svg", 0, 1000, 2000, 3999, 4001, 6000);
    }

    @TestFactory
    Collection<DynamicTest> animate_geometry() {
        // x, y, width and height animate together for nine seconds.
        return animationFrames("animate-elem-22-b.svg", 0, 2250, 4500, 6750, 8999, 9001, 10000);
    }

    @TestFactory
    Collection<DynamicTest> animate_attribute_type() {
        // The two animations begin at 3 s and 6 s, each lasting three seconds.
        return animationFrames("animate-elem-25-t.svg", 0, 2999, 3000, 4500, 5999, 6001, 7500, 8999, 9001, 10000);
    }

    @TestFactory
    Collection<DynamicTest> animate_stroke_width() {
        // One stroke animates from 1-5 s, the other from 4-7 s.
        return animationFrames("animate-elem-26-t.svg", 0, 999, 1000, 3000, 4000, 4999, 5001, 5500, 6999, 7001, 8000);
    }

    @TestFactory
    Collection<DynamicTest> animate_value_whitespace() {
        return animationFrames("animate-elem-88-t.svg", 0, 1000, 2000, 3999, 4001, 6000);
    }

    private static Collection<DynamicTest> animationFrames(@NotNull String name, long... timestamps) {
        Path source = Path.of(W3C_SVG_11_TEST_SUITE_PATH).resolve("svg").resolve(name);
        return LongStream.of(timestamps)
                .mapToObj(timestamp -> DynamicTest.dynamicTest(name + " at " + timestamp + " ms",
                        new W3cSvg11AnimationFrame(source, timestamp)))
                .toList();
    }

    @Disabled("JSVG resets the animated stroke at exactly dur=4000 ms instead of freezing the final value.")
    @Test
    void animate_stroke_inheritance_end() throws Throwable {
        new W3cSvg11AnimationFrame(animationSource(), 4000).execute();
    }

    private static @NotNull Path animationSource() {
        return Path.of(W3C_SVG_11_TEST_SUITE_PATH).resolve("svg/animate-elem-28-t.svg");
    }

    @TestFactory
    Collection<DynamicTest> color() throws IOException {
        return checkCategory("color", Set.of(
                "color-prof-01-f.svg",
                "color-prop-01-b.svg",
                "color-prop-04-t.svg",
                "color-prop-05-t.svg"));
    }

    @TestFactory
    Collection<DynamicTest> conform() throws IOException {
        return checkCategory("conform", Set.of(
                "conform-viewers-02-f.svg"));
    }

    @TestFactory
    Collection<DynamicTest> coords() throws IOException {
        return checkCategory("coords", Set.of(
                "coords-units-01-b.svg",
                "coords-viewattr-02-b.svg"));
    }

    @TestFactory
    Collection<DynamicTest> extend() throws IOException {
        return checkCategory("extend", Set.of());
    }

    @TestFactory
    Collection<DynamicTest> filters() throws IOException {
        return checkCategory("filters", Set.of(
                "filters-background-01-f.svg",
                "filters-blend-01-b.svg",
                "filters-color-01-b.svg",
                "filters-composite-02-b.svg",
                "filters-composite-03-f.svg",
                "filters-composite-04-f.svg",
                "filters-conv-01-f.svg",
                "filters-conv-02-f.svg",
                "filters-conv-04-f.svg",
                "filters-conv-05-f.svg",
                "filters-displace-01-f.svg",
                "filters-displace-02-f.svg",
                "filters-felem-01-b.svg",
                "filters-felem-02-f.svg",
                "filters-image-01-b.svg",
                "filters-image-03-f.svg",
                "filters-image-04-f.svg",
                "filters-image-05-f.svg",
                "filters-light-01-f.svg",
                "filters-light-02-f.svg",
                "filters-light-03-f.svg",
                "filters-light-05-f.svg",
                "filters-morph-01-f.svg",
                "filters-offset-02-b.svg",
                "filters-overview-01-b.svg",
                "filters-overview-02-b.svg",
                "filters-overview-03-b.svg",
                "filters-specular-01-f.svg",
                "filters-turb-02-f.svg"));
    }

    @Disabled("SVG fonts are not supported.")
    @TestFactory
    Collection<DynamicTest> fonts() throws IOException {
        return checkCategory("fonts", Set.of());
    }

    @Disabled("Requires browser text selection.")
    @TestFactory
    Collection<DynamicTest> imp() throws IOException {
        return checkCategory("imp", Set.of());
    }

    @Disabled("Requires browser interaction and event handling.")
    @TestFactory
    Collection<DynamicTest> interact() throws IOException {
        return checkCategory("interact", Set.of());
    }

    @TestFactory
    Collection<DynamicTest> linking() throws IOException {
        return checkCategory("linking", Set.of(
                "linking-a-09-b.svg"));
    }

    @TestFactory
    Collection<DynamicTest> masking() throws IOException {
        return checkCategory("masking", Set.of(
                "masking-filter-01-f.svg",
                "masking-mask-01-b.svg",
                "masking-path-01-b.svg",
                "masking-path-03-b.svg",
                "masking-path-06-b.svg",
                "masking-path-07-b.svg",
                "masking-path-08-b.svg",
                "masking-path-10-b.svg",
                "masking-path-11-b.svg"));
    }

    @TestFactory
    Collection<DynamicTest> metadata() throws IOException {
        return checkCategory("metadata", Set.of());
    }

    @TestFactory
    Collection<DynamicTest> painting() throws IOException {
        return checkCategory("painting", Set.of(
                "painting-control-05-f.svg",
                "painting-control-06-f.svg",
                "painting-marker-03-f.svg",
                "painting-marker-04-f.svg",
                "painting-marker-07-f.svg",
                "painting-marker-properties-01-f.svg",
                "painting-render-01-b.svg",
                "painting-stroke-05-t.svg"));
    }

    @TestFactory
    Collection<DynamicTest> paths() throws IOException {
        return checkCategory("paths", Set.of(
                "paths-data-18-f.svg"));
    }

    @TestFactory
    Collection<DynamicTest> pservers() throws IOException {
        return checkCategory("pservers", Set.of(
                "pservers-grad-03-b.svg",
                "pservers-grad-05-b.svg",
                "pservers-grad-06-b.svg",
                "pservers-grad-08-b.svg",
                "pservers-grad-16-b.svg",
                "pservers-grad-17-b.svg",
                "pservers-grad-18-b.svg",
                "pservers-grad-20-b.svg",
                "pservers-grad-24-f.svg",
                "pservers-pattern-02-f.svg",
                "pservers-pattern-03-f.svg",
                "pservers-pattern-06-f.svg",
                "pservers-pattern-07-f.svg",
                "pservers-pattern-08-f.svg",
                "pservers-pattern-09-f.svg"));
    }

    @TestFactory
    Collection<DynamicTest> render() throws IOException {
        return checkCategory("render", Set.of(
                "render-elems-06-t.svg",
                "render-elems-07-t.svg",
                "render-elems-08-t.svg",
                "render-groups-01-b.svg",
                "render-groups-03-t.svg"));
    }

    @Disabled("Requires scripting.")
    @TestFactory
    Collection<DynamicTest> script() throws IOException {
        return checkCategory("script", Set.of());
    }

    @TestFactory
    Collection<DynamicTest> shapes() throws IOException {
        return checkCategory("shapes", Set.of(
                "shapes-grammar-01-f.svg",
                "shapes-polygon-03-t.svg"));
    }

    @TestFactory
    Collection<DynamicTest> struct() throws IOException {
        return checkCategory("struct", Set.of(
                "struct-cond-01-t.svg",
                "struct-cond-02-t.svg",
                "struct-cond-03-t.svg",
                "struct-cond-overview-02-f.svg",
                "struct-cond-overview-03-f.svg",
                "struct-cond-overview-04-f.svg",
                "struct-cond-overview-05-f.svg",
                "struct-image-02-b.svg",
                "struct-image-07-t.svg",
                "struct-image-10-t.svg",
                "struct-image-12-b.svg",
                "struct-image-15-f.svg",
                "struct-image-16-f.svg",
                "struct-image-18-f.svg",
                "struct-use-03-t.svg",
                "struct-use-04-b.svg",
                "struct-use-06-b.svg",
                "struct-use-08-b.svg",
                "struct-use-10-f.svg",
                "struct-use-11-f.svg",
                "struct-use-12-f.svg"));
    }

    @TestFactory
    Collection<DynamicTest> styling() throws IOException {
        return checkCategory("styling", Set.of(
                "styling-css-02-b.svg",
                "styling-css-03-b.svg",
                "styling-css-04-f.svg",
                "styling-css-05-b.svg",
                "styling-css-06-b.svg",
                "styling-css-08-f.svg",
                "styling-css-09-f.svg",
                "styling-css-10-f.svg",
                "styling-elem-01-b.svg",
                "styling-inherit-01-b.svg",
                "styling-pres-04-f.svg",
                "styling-pres-05-f.svg"));
    }

    @Disabled("Requires SVG DOM APIs.")
    @TestFactory
    Collection<DynamicTest> svgdom() throws IOException {
        return checkCategory("svgdom", Set.of());
    }

    @TestFactory
    Collection<DynamicTest> text() throws IOException {
        return checkCategory("text", Set.of(
                "text-align-02-b.svg",
                "text-align-03-b.svg",
                "text-align-04-b.svg",
                "text-align-05-b.svg",
                "text-align-06-b.svg",
                "text-align-08-b.svg",
                "text-altglyph-01-b.svg",
                "text-altglyph-02-b.svg",
                "text-altglyph-03-b.svg",
                "text-deco-01-b.svg",
                "text-fonts-01-t.svg",
                "text-fonts-03-t.svg",
                "text-fonts-04-t.svg",
                "text-fonts-05-f.svg",
                "text-fonts-203-t.svg",
                "text-intro-01-t.svg",
                "text-intro-03-b.svg",
                "text-intro-06-t.svg",
                "text-intro-07-t.svg",
                "text-intro-10-f.svg",
                "text-intro-11-t.svg",
                "text-intro-12-t.svg",
                "text-path-01-b.svg",
                "text-path-02-b.svg",
                "text-spacing-01-b.svg",
                "text-text-01-b.svg",
                "text-text-03-b.svg",
                "text-text-04-t.svg",
                "text-text-05-t.svg",
                "text-text-12-t.svg",
                "text-tref-02-b.svg",
                "text-tref-03-b.svg",
                "text-tspan-02-b.svg",
                "text-ws-02-t.svg",
                "text-ws-03-t.svg"));
    }

    @TestFactory
    Collection<DynamicTest> types() throws IOException {
        return checkCategory("types", Set.of(
                "types-basic-01-f.svg"));
    }

    private static @NotNull MemoryImageSource normalizedSource(@NotNull Path testFile, boolean animated)
            throws Exception {
        String svgNamespace = "http://www.w3.org/2000/svg";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        org.w3c.dom.Document document;
        try (InputStream file = Files.newInputStream(testFile);
                InputStream input = testFile.toString().endsWith(".svgz") ? new GZIPInputStream(file) : file) {
            document = factory.newDocumentBuilder().parse(input);
        }
        assumeTrue(document.getElementsByTagNameNS(svgNamespace, "script").getLength() == 0,
                "Requires scripting; a static rendering would not test the intended behavior.");
        if (!animated) {
            for (String tag : Set.of("animate", "animateColor", "animateTransform", "animateMotion", "set")) {
                assumeTrue(document.getElementsByTagNameNS(svgNamespace, tag).getLength() == 0,
                        "Requires explicit animation frames or interaction.");
            }
        }
        // Only replace the suite's common label font. Keep text, its styling and all other
        // font declarations intact, so typography and text effects remain testable.
        var faces = document.getElementsByTagNameNS(svgNamespace, "font-face");
        for (int i = faces.getLength() - 1; i >= 0; i--) {
            Element face = (Element) faces.item(i);
            if ("SVGFreeSansASCII".equals(face.getAttribute("font-family"))) {
                face.getParentNode().removeChild(face);
            }
        }
        var elements = document.getElementsByTagNameNS(svgNamespace, "*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            for (String attribute : Set.of("font-family", "style")) {
                if (element.hasAttribute(attribute)) {
                    element.setAttribute(attribute, element.getAttribute(attribute)
                            .replace("SVGFreeSansASCII", "SansSerif"));
                }
            }
            if ("style".equals(element.getLocalName())) {
                element.setTextContent(element.getTextContent().replace("SVGFreeSansASCII", "SansSerif"));
            }
        }
        StringWriter xml = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(xml));
        // Preserve the original document URI for relative images, stylesheets and external SVGs.
        return new MemoryImageSource(testFile.getFileName().toString(), xml.toString(), testFile.toUri().toURL());
    }

    record W3cSvg11AnimationFrame(@NotNull Path testFile, long timestamp) implements Executable {
        @Override
        public void execute() throws Throwable {
            var normalized = normalizedSource(testFile, true);
            var source = new MemoryImageSource(testFile.getFileName() + " at " + timestamp + " ms",
                    normalized.data(), normalized.url());
            var state = new AnimationState(0, timestamp);
            var result = ImageComparison.compareImages(new ImageComparison.CompareInfo(
                    expected(source, RenderType.Batik.withViewportSize(480, 360).withAnimationState(state)),
                    // Keep small animated features significant; static-suite tolerance could hide them.
                    actual(source, RenderType.JSVG.withAnimationState(state)), 0.01, 0.1));
            assertEquals(SUCCESS, result, testFile.getFileName() + " at " + timestamp + " ms");
        }
    }

    record W3cSvg11RefTest(@NotNull Path testFile) implements Executable {
        @Override
        public void execute() throws Throwable {
            // These references cover cases where Batik rejects the input or disagrees with W3C.
            // Choose the oracle explicitly; never retry a failed comparison against another renderer.
            if (Set.of("masking-mask-02-f.svg", "paths-data-20-f.svg", "struct-image-04-t.svg")
                    .contains(testFile.getFileName().toString())) {
                Path png = testFile.getParent().getParent().resolve("png")
                        .resolve(testFile.getFileName().toString().replace(".svg", ".png"));
                assertEquals(SUCCESS, ImageComparison.compareImages(new ImageComparison.CompareInfo(
                        expected(new UrlImageSource(png.toUri().toURL()), RenderType.DiskImage),
                        actual(new UrlImageSource(testFile.toUri().toURL()), RenderType.JSVG))));
                return;
            }
            var source = normalizedSource(testFile, false);
            var comparison = new ImageComparison.CompareInfo(
                    expected(source, RenderType.Batik.withViewportSize(480, 360)),
                    actual(source, RenderType.JSVG));
            // Dedicated font tests often contain only a few small glyphs. The normal static
            // tolerance can hide their complete replacement by a fallback font during an audit.
            if (testFile.getFileName().toString().startsWith("fonts-")) {
                comparison = new ImageComparison.CompareInfo(comparison.expected(), comparison.actual(), 0.01, 0.1);
            }
            var result = ImageComparison.compareImages(comparison);
            assertEquals(SUCCESS, result);
        }
    }
}
