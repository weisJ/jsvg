/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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
import static com.github.weisj.jsvg.ImageComparison.ImageSource.*;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.renderer.PlatformSupport;

class ReSvgTestSuite {

    private static final Logger LOGGER = Logging.getLogger(ReSvgTestSuite.class);
    private static final String RESVG_TEST_SUITE_PATH = System.getenv("RESVG_TEST_SUITE_PATH");

    // JSVG render type carrying the fonts bundled with the suite (set up in @BeforeAll).
    private static @NotNull RenderType jsvgRenderType = RenderType.JSVG;

    static Collection<DynamicTest> checkDirectory(@NotNull String name) {
        return checkDirectory(name, Collections.emptySet());
    }

    static Collection<DynamicTest> checkDirectory(@NotNull String name, Collection<String> exclude) {
        Path basePath = Path.of(RESVG_TEST_SUITE_PATH);
        Path tests = basePath.resolve(name);
        try (var files = Files.walk(tests)) {
            return files
                    .filter(p -> p.toString().endsWith(".svg"))
                    .filter(p -> !exclude.contains(p.getFileName().toString()))
                    .map(p -> {
                        String testName = basePath.relativize(p).toString();
                        return DynamicTest.dynamicTest(testName, new ReSVGRefTest(p));
                    })
                    .toList();
        } catch (IOException e) {
            Assertions.fail(e);
        }
        return Collections.emptyList();
    }

    @BeforeAll
    static void checkForReSVGRepositoryAndRegisterFonts() {
        var exists = Path.of(RESVG_TEST_SUITE_PATH).toFile().exists();
        var message = """
                The resvg submodule was not found. Skipping ReSVG test suite.
                Please run `git submodule update --init --recursive` to fetch the submodule.
                """.stripIndent();
        if (!exists) {
            LOGGER.warn(message);
        }
        assumeTrue(exists, message);

        jsvgRenderType = new RenderType.JSVGType(RenderType.JSVG.loaderContext(), loadBundledFonts());
        // Drop any fallback fonts a prior test cached for these families in the shared JVM.
        FontResolver.clearFontCache();
    }

    private static @NotNull PlatformSupport loadBundledFonts() {
        Map<String, Font> fonts = new HashMap<>();
        Path fontDir = Path.of(RESVG_TEST_SUITE_PATH).getParent().resolve("fonts");
        try (var files = Files.walk(fontDir)) {
            files.filter(p -> p.toString().endsWith(".ttf")).forEach(p -> {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, p.toFile());
                    // FontResolver looks up the CSS-canonicalized (lower-cased) family name.
                    String family = font.getFamily().toLowerCase(Locale.US);
                    // A family may span multiple files; prefer the regular variant.
                    if (p.getFileName().toString().contains("Regular") || !fonts.containsKey(family)) {
                        fonts.put(family, font);
                    }
                } catch (IOException | FontFormatException e) {
                    LOGGER.warn("Failed to load font " + p, e);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to walk font directory " + fontDir, e);
        }
        return new BundledFontSupport(fonts);
    }

    private record BundledFontSupport(@NotNull Map<String, Font> fonts) implements PlatformSupport {
        @Override
        public float fontSize() {
            // The reference images are rendered with resvg, whose default font size is 12.
            return 12;
        }

        @Override
        public @Nullable ImageObserver imageObserver() {
            return null;
        }

        @Override
        public @Nullable TargetSurface targetSurface() {
            return null;
        }

        @Override
        public @Nullable Font customFont(@NotNull String family) {
            return fonts.get(family.toLowerCase(Locale.US));
        }
    }

    @TestFactory
    Collection<DynamicTest> circle() {
        return checkDirectory("shapes/circle");
    }

    @TestFactory
    Collection<DynamicTest> ellipse() {
        return checkDirectory("shapes/ellipse");
    }

    @TestFactory
    Collection<DynamicTest> line() {
        return checkDirectory("shapes/line", Set.of(
                // We don't handle transforms of strokes correctly
                "with-transform.svg"));
    }

    @TestFactory
    Collection<DynamicTest> path() {
        return checkDirectory("shapes/path");
    }

    @TestFactory
    Collection<DynamicTest> polygon() {
        return checkDirectory("shapes/polygon");
    }

    @TestFactory
    Collection<DynamicTest> polyline() {
        return checkDirectory("shapes/polyline");
    }

    @TestFactory
    Collection<DynamicTest> mask() {
        return checkDirectory("masking/mask", Set.of(
                "maskUnits=userSpaceOnUse-with-rect.svg",
                "maskUnits=userSpaceOnUse-without-rect.svg",
                "recursive-on-child.svg", // UB
                "mask-on-child.svg",
                "recursive.svg",
                "mask-type=invalid.svg",
                "with-opacity-1.svg",
                "color-interpolation=linearRGB.svg",
                "with-opacity-3.svg",
                "half-width-region-with-rotation.svg",
                "on-a-horizontal-line.svg",
                "simple-case.svg",
                "mask-on-self-with-mixed-mask-type.svg",
                "recursive-on-self.svg",
                "transform-has-no-effect.svg",
                "mask-type=luminance.svg",
                "self-recursive.svg"));
    }

    @TestFactory
    Collection<DynamicTest> rect() {
        return checkDirectory("shapes/rect", Set.of(
                // Excluded because we don't support them
                "ch-values.svg",
                // Excluded because the expected result is incorrect
                "cap-values.svg",
                "vw-and-vh-values.svg",
                "vmin-and-vmax-values.svg",
                "vi-and-vb-values.svg",
                "ic-values.svg",
                "lh-values.svg",
                "rlh-values.svg"));
    }

    @TestFactory
    Collection<DynamicTest> color() {
        return checkDirectory("painting/color");
    }

    @TestFactory
    Collection<DynamicTest> fillOpacity() {
        return checkDirectory("painting/fill-opacity", Set.of(
                // Needs investigation
                "with-linearGradient.svg",
                "with-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> strokeOpacity() {
        return checkDirectory("painting/stroke-opacity", Set.of(
                // Needs investigation
                "with-linearGradient.svg",
                "with-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> fontSize() {
        return checkDirectory("text/font-size", Set.of(
                "negative-size.svg" // UB
        ));
    }

    @TestFactory
    Collection<DynamicTest> defs() {
        return checkDirectory("structure/defs", Set.of(
                // Gradient color ramp differs from the reference. Needs investigation
                "multiple-defs.svg",
                "nested-defs.svg",
                "out-of-order.svg",
                "simple-case.svg"));
    }

    @TestFactory
    Collection<DynamicTest> style() {
        return checkDirectory("structure/style", Set.of(
                // @import of external stylesheets is not supported
                "external-CSS.svg",
                // We follow SVG 2, where geometry properties can be set via CSS
                "non-presentational-attribute.svg"));
    }

    @TestFactory
    Collection<DynamicTest> styleAttribute() {
        return checkDirectory("structure/style-attribute", Set.of(
                // We follow SVG 2, where geometry properties can be set via CSS
                "non-presentational-attribute.svg"));
    }

    @TestFactory
    Collection<DynamicTest> use() {
        return checkDirectory("structure/use", Set.of(
                // References into external documents are not supported
                "xlink-to-an-external-file.svg"));
    }

    private record ReSVGRefTest(@NotNull Path testFile) implements Executable {

        @Override
        public void execute() throws Throwable {
            var pngRef = testFile.resolveSibling(testFile.getFileName().toString().replace(".svg", ".png"));
            var result = ImageComparison.compareImages(new ImageComparison.CompareInfo(
                    expected(new UrlImageSource(pngRef.toUri().toURL()),
                            RenderType.DiskImage),
                    actual(new UrlImageSource(testFile.toUri().toURL()),
                            jsvgRenderType)));
            assertEquals(SUCCESS, result);
        }
    }
}
