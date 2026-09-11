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
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.PlatformSupport;

/** Standalone, static SVG reftests from Web Platform Tests. */
class WptSvgTestSuite {
    private static final String WPT_TEST_SUITE_PATH = System.getenv("WPT_TEST_SUITE_PATH");
    private static final String SVG = "http://www.w3.org/2000/svg";
    private static final String HTML = "http://www.w3.org/1999/xhtml";
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    public static final Pattern FONT_FACE_PATTERN = Pattern.compile("@font-face\\s*\\{([^}]+)}");
    private static final @NotNull PlatformSupport.FontLoader NULL_FONT_LOADER = family -> null;
    private static @NotNull PlatformSupport.FontLoader fonts = NULL_FONT_LOADER;

    // Current rendering mismatches. Keep upstream artwork and fuzzy limits unchanged.
    private static final Set<String> EXCLUDED = Set.of(
            // embedded: current reference mismatches.
            "embedded/image-embedding-svg-viewref-with-viewbox.svg",
            "embedded/image-embedding-svg-with-auto-height.svg",
            "embedded/image-embedding-svg-with-fractional-viewbox.svg",
            "embedded/image-embedding-svg-with-viewport-units-inline-style.svg",
            "embedded/image-embedding-svg-with-viewport-units.svg",
            "embedded/image-fractional-width-vertical-fidelity.svg",
            "embedded/image-large-bitmap.svg",
            // fonts: current reference mismatches.
            "fonts/font-size-adjust-scale-text.svg",
            // geometry: current reference mismatches.
            "geometry/reftests/circle-004.svg",
            "geometry/reftests/ellipse-004.svg",
            "geometry/reftests/rect-004.svg",
            // painting: current reference mismatches.
            "painting/currentColor-override-pserver-fill.svg",
            "painting/currentColor-override-pserver-stroke.svg",
            "painting/marker-001.svg",
            "painting/marker-002.svg",
            "painting/marker-006.svg",
            "painting/marker-007.svg",
            "painting/marker-008.svg",
            "painting/marker-orient-001.svg",
            "painting/reftests/marker-external-reference.svg",
            "painting/reftests/marker-path-001.svg",
            "painting/reftests/marker-path-002.svg",
            "painting/reftests/marker-path-003.svg",
            "painting/reftests/marker-path-011.svg",
            "painting/reftests/marker-path-012.svg",
            "painting/reftests/marker-path-013.svg",
            "painting/reftests/marker-path-021.svg",
            "painting/reftests/marker-path-022.svg",
            "painting/reftests/marker-path-023.svg",
            "painting/reftests/markers-orient-001.svg",
            "painting/reftests/markers-orient-002.svg",
            "painting/reftests/paint-context-003.svg",
            "painting/reftests/paint-context-006.svg",
            "painting/reftests/paint-context-007.svg",
            "painting/reftests/percentage-attribute.svg",
            "painting/reftests/percentage.svg",
            // path: current reference mismatches.
            "path/bearing/absolute.svg",
            "path/bearing/relative.svg",
            "path/bearing/zero.svg",
            "path/distance/path-length-css-overrides-presentation-attribute.tentative.svg",
            "path/distance/path-length-css-property.tentative.svg",
            "path/distance/path-length-css-zero.tentative.svg",
            "path/distance/path-length-css-zoom.tentative.svg",
            "path/distance/pathLength-positive.svg",
            "path/distance/pathLength-zero-percentage.svg",
            "path/distance/pathLength-zero.svg",
            // Zero pathLength stalls Java2D while rasterizing dashes.
            "path/distance/pathlength-path-zero.svg",
            "path/property/marker-path.svg",
            "path/property/priority.svg",
            // pservers: current reference mismatches.
            "pservers/reftests/fill-fallback-none-3.svg",
            "pservers/reftests/gradient-transform-01.svg",
            "pservers/reftests/gradient-transform-02.svg",
            "pservers/reftests/pattern-text-01.svg",
            "pservers/reftests/pattern-transform-02.svg",
            "pservers/reftests/radialgradient-basic-002.svg",
            "pservers/reftests/stroke-fallback-invalid-uri.svg",
            // render: current reference mismatches.
            "render/order/z-index.svg",
            "render/reftests/blending-001.svg",
            "render/reftests/blending-002.svg",
            // shapes: current reference mismatches.
            "shapes/ellipse-09.svg",
            "shapes/rect-05.svg",
            "shapes/reftests/disabled-shapes-01.svg",
            "shapes/reftests/pathlength-002.svg",
            // struct: current reference mismatches.
            "struct/reftests/image-symbol.svg",
            "struct/reftests/requiredextensions-empty-string.svg",
            "struct/reftests/use-encoding.svg",
            "struct/reftests/use-inheritance-001.svg",
            "struct/reftests/use-inheritance-nth-child-of.svg",
            "struct/reftests/use-inheritance-nth-last-child-of.svg",
            "struct/reftests/use-svg-dimensions-override-001.svg",
            "struct/reftests/use-svg-dimensions-override-002.svg",
            "struct/reftests/use-svg-inline-css.svg",
            "struct/reftests/use-switch.svg",
            "struct/reftests/use-symbol-dimensions-override-001.svg",
            "struct/reftests/use-symbol-dimensions-override-002.svg",
            "struct/reftests/use-symbol-inline-css-001.svg",
            "struct/reftests/use-symbol-inline-css-002.svg",
            "struct/reftests/use-symbol-inline-css-003.svg",
            // styling: current reference mismatches.
            "styling/css-var-on-length-attributes-01.svg",
            "styling/css-var-on-length-attributes-03.svg",
            "styling/nested-svg-sizing-auto.tentative.svg",
            "styling/nested-svg-sizing-calc-size.tentative.svg",
            "styling/nested-svg-sizing-ems.tentative.svg",
            "styling/nested-svg-sizing-fit-content.tentative.svg",
            "styling/nested-svg-sizing-inherit.tentative.svg",
            "styling/nested-svg-sizing-initial.tentative.svg",
            "styling/nested-svg-sizing-max-content.tentative.svg",
            "styling/nested-svg-sizing-min-content.tentative.svg",
            "styling/nested-svg-sizing-percent.tentative.svg",
            "styling/nested-svg-sizing-rems.tentative.svg",
            "styling/nested-svg-sizing-stretch.tentative.svg",
            "styling/nested-svg-sizing-with-use.svg",
            "styling/nested-svg-sizing.svg",
            "styling/outermost-svg-sizing-viewport-units.svg",
            // text: current reference mismatches.
            "text/reftests/dominant-baseline-text-after-edge.svg",
            "text/reftests/dominant-baseline-text-before-edge.svg",
            "text/reftests/first-letter.svg",
            "text/reftests/lengthAdjust-large-font-vertical.svg",
            "text/reftests/lengthAdjust-large-font.svg",
            "text/reftests/lengthAdjust-vertical.svg",
            "text/reftests/text-bidi-controls-anchors-1.svg",
            "text/reftests/text-bidi-controls-anchors-2.svg",
            "text/reftests/text-complex-001.svg",
            "text/reftests/text-complex-002.svg",
            "text/reftests/text-inline-size-001.svg",
            "text/reftests/text-inline-size-002.svg",
            "text/reftests/text-inline-size-003.svg",
            "text/reftests/text-inline-size-005.svg",
            "text/reftests/text-inline-size-006.svg",
            "text/reftests/text-inline-size-007.svg",
            "text/reftests/text-inline-size-101.svg",
            "text/reftests/text-inline-size-201.svg",
            "text/reftests/text-multiline-001.svg",
            "text/reftests/text-multiline-002.svg",
            "text/reftests/text-multiline-003.svg",
            "text/reftests/text-shape-inside-001.svg",
            "text/reftests/text-shape-inside-002.svg",
            "text/reftests/text-xml-space-001.svg",
            "text/reftests/textpath-path-attr-empty-fallback.svg",
            "text/reftests/textpath-path-attr-invalid-path-fallback.svg",
            "text/reftests/textpath-pathlength-css-display-none.tentative.svg",
            "text/reftests/textpath-shape-001.svg",
            "text/reftests/tspan-opacity-mixed-direction.svg");

    private static @NotNull PlatformSupport asPlatformSupport(@NotNull PlatformSupport.FontLoader fontLoader) {
        return new PlatformSupport() {
            @Override
            public @Nullable ImageObserver imageObserver() {
                return null;
            }

            @Override
            public @Nullable TargetSurface targetSurface() {
                return null;
            }

            @Override
            public @Nullable FontLoader fontLoader() {
                return fontLoader;
            }
        };
    }

    static @NotNull Set<String> excludedTests() {
        return EXCLUDED;
    }

    @BeforeAll
    static void checkForWptRepository() throws Exception {
        assumeTrue(WPT_TEST_SUITE_PATH != null && !WPT_TEST_SUITE_PATH.isBlank()
                && Files.isDirectory(Path.of(WPT_TEST_SUITE_PATH).resolve("svg")), """
                        The WPT submodule was not found. Skipping WPT SVG test suite.
                        Please run `git submodule update --init wpt-test-suite` to fetch the submodule.
                        """);
        fonts = WptFontSupport.load(Path.of(WPT_TEST_SUITE_PATH));
        FontResolver.clearFontCache();
    }

    @AfterAll
    static void clearFonts() {
        fonts = NULL_FONT_LOADER;
        FontResolver.clearFontCache();
    }

    @TestFactory
    Collection<DynamicTest> references() throws Exception {
        Path base = Path.of(WPT_TEST_SUITE_PATH).resolve("svg");
        List<DynamicTest> tests = new ArrayList<>();
        try (var files = Files.walk(base)) {
            for (Path path : files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".svg"))
                    // Manual SVG 1.1 imports already have their own suite; crash tests have no oracle.
                    .filter(p -> !p.startsWith(base.resolve("import")) && !p.startsWith(base.resolve("crashtests")))
                    .sorted().toList()) {
                if (referenceLinks(read(path)).isEmpty()) continue;
                String name = base.relativize(path).toString().replace('\\', '/');
                tests.add(DynamicTest.dynamicTest(name, () -> {
                    assumeTrue(!EXCLUDED.contains(name), "Known WPT reference mismatch: " + name);
                    new WptSvgRefTest(path).execute();
                }));
            }
        }
        assertFalse(tests.isEmpty(), "No WPT SVG reftests found in " + base);
        return tests;
    }

    @TestFactory
    Collection<DynamicTest> registeredFonts() {
        return Stream.of("Ahem", "FreeSans").map(family -> DynamicTest.dynamicTest(family, () -> {
            // Compare JSVG's family lookup with Java2D drawing directly from the bundled font.
            var source = new MemoryImageSource("wpt-font-" + family, """
                    <svg xmlns="http://www.w3.org/2000/svg" width="160" height="80">
                      <text x="10" y="50" font-family="%s" font-size="40">B</text>
                    </svg>
                    """.formatted(family));
            var expected = new BufferedImage(160, 80, BufferedImage.TYPE_INT_ARGB);
            var graphics = expected.createGraphics();
            graphics.setRenderingHints(ImageComparison.referenceHintSet());
            graphics.setColor(Color.BLACK);
            graphics.setFont(java.util.Objects.requireNonNull(fonts.customFont(family)).deriveFont(40f));
            graphics.fill(
                    graphics.getFont().createGlyphVector(graphics.getFontRenderContext(), "B").getOutline(10, 50));
            graphics.dispose();
            FontResolver.clearFontCache();
            var renderer = new RenderType.JSVGType(LoaderContext.builder().build(), asPlatformSupport(fonts));
            var rendered = actual(source, renderer).render(null);
            assertEquals(SUCCESS, ImageComparison.compareImageRasterization(
                    expected, rendered, source.name(), 0, 0));
        })).toList();
    }

    private static @NotNull Document read(@NotNull Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        try (var input = Files.newInputStream(path)) {
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private static @NotNull List<Element> referenceLinks(@NotNull Document document) {
        List<Element> references = new ArrayList<>();
        var links = document.getElementsByTagNameNS(HTML, "link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            if (Set.of("match", "mismatch").contains(link.getAttribute("rel"))) references.add(link);
        }
        return references;
    }

    private static void requireStaticSvg(@NotNull Path path, @NotNull Document document) {
        Element root = document.getDocumentElement();
        assumeTrue(!path.toString().endsWith("-visited.svg"), "Requires browser link history.");
        assumeTrue(SVG.equals(root.getNamespaceURI()) && "svg".equals(root.getLocalName()),
                "Requires HTML layout: " + path);
        assumeTrue(!path.toString().contains(".sub.") && !path.toString().contains("-manual."),
                "Requires WPT server substitution or manual interaction.");
        var elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            assumeTrue(!Set.of("script", "animate", "animateColor", "animateTransform", "animateMotion", "set",
                    "foreignObject").contains(element.getLocalName()),
                    "Requires scripting, animation frames or HTML layout: " + path);
            assumeTrue(!element.getAttribute("class").contains("reftest-wait"), "Requires a browser render event.");
            var attributes = element.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                var attribute = attributes.item(j);
                assumeTrue(!attribute.getNodeName().startsWith("on"), "Requires event handlers: " + path);
                assumeTrue(!attribute.getNodeValue().contains("{{"), "Requires WPT server substitution.");
                if ("href".equals(attribute.getLocalName()) && !Set.of("link", "a").contains(element.getLocalName())) {
                    String href = attribute.getNodeValue();
                    assumeTrue(!href.startsWith("/") && !href.startsWith("http:") && !href.startsWith("https:"),
                            "Requires WPT server resource resolution: " + path);
                }
            }
            if ("link".equals(element.getLocalName()) && "stylesheet".equals(element.getAttribute("rel"))) {
                assumeTrue("/fonts/ahem.css".equals(element.getAttribute("href")) && fonts.customFont("ahem") != null,
                        "Requires a browser stylesheet loader or an unregistered font.");
            }
            if ("style".equals(element.getLocalName())) {
                var faces = FONT_FACE_PATTERN.matcher(element.getTextContent());
                while (faces.find()) {
                    // These are the only inline font-face declarations in the selected SVG pairs.
                    assumeTrue(faces.group(1).contains("font-family: FreeSans;")
                            && faces.group(1).contains("FreeSans.woff") && fonts.customFont("freesans") != null,
                            "Requires an unregistered custom font: " + path);
                }
            }
        }
    }

    private static @NotNull BufferedImage render(@NotNull Path path, @NotNull Document document) throws Exception {
        requireStaticSvg(path, document);
        Element root = document.getDocumentElement();
        // Resolve the top-level SVG's default/percentage dimensions against WPT's 800x600 viewport.
        // Retain absolute dimensions and the original viewBox, then place the image on the page.
        resolveDimension(root, "width", WIDTH);
        resolveDimension(root, "height", HEIGHT);
        StringWriter xml = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(xml));
        var source = new MemoryImageSource(path.toString(), xml.toString(), path.toUri().toURL());
        // External document caches belong to one rendering, just as each browser reftest loads a new page.
        var renderer = new RenderType.JSVGType(LoaderContext.builder()
                .externalResourcePolicy(ResourcePolicy.ALLOW_RELATIVE).build(), asPlatformSupport(fonts));
        FontResolver.clearFontCache();
        BufferedImage rendered = actual(source, renderer).render(null);
        BufferedImage page = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var graphics = page.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.drawImage(rendered, 0, 0, null);
        graphics.dispose();
        return page;
    }

    private static void resolveDimension(@NotNull Element root, @NotNull String attribute, int viewport) {
        String value = root.getAttribute(attribute).trim();
        if (value.isEmpty()) {
            root.setAttribute(attribute, Integer.toString(viewport));
        } else if (value.endsWith("%")) {
            root.setAttribute(attribute,
                    Float.toString(viewport * Float.parseFloat(value.substring(0, value.length() - 1)) / 100));
        }
    }

    record WptSvgRefTest(@NotNull Path testFile) implements Executable {
        @Override
        public void execute() throws Throwable {
            Document document = read(testFile);
            requireStaticSvg(testFile, document);
            List<Element> links = referenceLinks(document);
            // Supporting one match covers the static SVG pairs in the imported revision.
            // Never mistake a mismatch assertion or one alternative of several for a match test.
            assumeTrue(links.size() == 1 && "match".equals(links.getFirst().getAttribute("rel")),
                    "Requires multiple-reference or mismatch semantics.");
            String href = links.getFirst().getAttribute("href");
            assumeTrue(!href.startsWith("/"), "Requires WPT server resource resolution.");
            URI reference = testFile.toUri().resolve(href);
            assumeTrue("file".equals(reference.getScheme()) && reference.getQuery() == null
                    && reference.getFragment() == null && reference.getPath().endsWith(".svg"),
                    "Requires an HTML reference, URL fragment or WPT server.");
            Path referenceFile = Path.of(reference);
            assertTrue(Files.isRegularFile(referenceFile), "Missing WPT reference: " + referenceFile);
            Document referenceDocument = read(referenceFile);
            BufferedImage expected = render(referenceFile, referenceDocument);
            BufferedImage actual = render(testFile, document);
            Fuzzy fuzzy = Fuzzy.from(document);
            assertTrue(fuzzy.matches(expected, actual),
                    () -> "WPT fuzzy limits: " + fuzzy + "\n" + ImageComparison.compareImageRasterization(
                            expected, actual, diagnosticName(), 0, 0));
        }

        private @NotNull String diagnosticName() {
            String path = testFile.toString().replace('\\', '/');
            int svg = path.lastIndexOf("/svg/");
            return "wpt-" + (svg >= 0 ? path.substring(svg + 5) : testFile.getFileName());
        }
    }

    private record Fuzzy(int minDifference, int maxDifference, int minPixels, int maxPixels) {
        static @NotNull Fuzzy from(@NotNull Document document) {
            var metadata = document.getElementsByTagNameNS(HTML, "meta");
            for (int i = 0; i < metadata.getLength(); i++) {
                Element meta = (Element) metadata.item(i);
                if (!"fuzzy".equals(meta.getAttribute("name"))) continue;
                String value = meta.getAttribute("content");
                assumeTrue(!value.contains(":"), "Reference-specific fuzzy metadata is not supported.");
                String[] parts = value.replace("maxDifference=", "").replace("totalPixels=", "").split(";");
                assertEquals(2, parts.length, "Invalid WPT fuzzy metadata: " + value);
                int[] difference = range(parts[0]);
                int[] pixels = range(parts[1]);
                return new Fuzzy(difference[0], difference[1], pixels[0], pixels[1]);
            }
            return new Fuzzy(0, 0, 0, 0);
        }

        private static int[] range(@NotNull String value) {
            String[] values = value.trim().split("-");
            int minimum = Integer.parseInt(values[0].trim());
            return new int[] {minimum, Integer.parseInt(values[values.length - 1].trim())};
        }

        boolean matches(@NotNull BufferedImage expected, @NotNull BufferedImage actual) {
            if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) return false;
            int difference = 0;
            int pixels = 0;
            for (int y = 0; y < expected.getHeight(); y++) {
                for (int x = 0; x < expected.getWidth(); x++) {
                    int a = expected.getRGB(x, y);
                    int b = actual.getRGB(x, y);
                    if (a == b) continue;
                    pixels++;
                    for (int shift : new int[] {0, 8, 16}) {
                        difference = Math.max(difference, Math.abs(((a >> shift) & 255) - ((b >> shift) & 255)));
                    }
                }
            }
            return difference >= minDifference && difference <= maxDifference
                    && pixels >= minPixels && pixels <= maxPixels;
        }
    }
}
