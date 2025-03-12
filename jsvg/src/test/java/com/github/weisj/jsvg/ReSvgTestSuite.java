/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import static com.github.weisj.jsvg.ReferenceTest.ImageInfo.actual;
import static com.github.weisj.jsvg.ReferenceTest.ImageInfo.expected;
import static com.github.weisj.jsvg.ReferenceTest.ImageSource.*;
import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import com.github.weisj.jsvg.ReferenceTest.RenderType;

class ReSvgTestSuite {

    private static final Logger LOGGER = Logging.getLogger(ReSvgTestSuite.class);
    private static final String RESVG_TEST_SUITE_PATH = System.getenv("RESVG_TEST_SUITE_PATH");

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
    static void checkForReSVGRepository() {
        var exists = Path.of(RESVG_TEST_SUITE_PATH).toFile().exists();
        var message = """
                The resvg submodule was not found. Skipping ReSVG test suite.
                Please run `git submodule update --init --recursive` to fetch the submodule.
                """.stripIndent();
        if (!exists) {
            LOGGER.warn(message);
        }
        assumeTrue(exists, message);
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
                // We don't do proper kerning
                "on-text.svg",
                // Needs investigation
                "with-linearGradient.svg",
                "with-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> strokeOpacity() {
        return checkDirectory("painting/stroke-opacity", Set.of(
                // We don't do proper kerning
                "on-text.svg",
                // Needs investigation
                "with-linearGradient.svg",
                "with-opacity.svg"));
    }

    private record ReSVGRefTest(@NotNull Path testFile) implements Executable {

        @Override
        public void execute() throws Throwable {
            var pngRef = testFile.resolveSibling(testFile.getFileName().toString().replace(".svg", ".png"));
            var result = ReferenceTest.compareImages(new ReferenceTest.CompareInfo(
                    expected(new UrlImageSource(pngRef.toUri().toURL()),
                            RenderType.DiskImage),
                    actual(new UrlImageSource(testFile.toUri().toURL()),
                            RenderType.JSVG)));
            assertEquals(SUCCESS, result);
        }
    }
}
