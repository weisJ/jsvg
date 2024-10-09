/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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

import static com.github.weisj.jsvg.ReferenceTest.ImageSource.*;
import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import com.github.weisj.jsvg.ReferenceTest.RenderType;

class ReSvgTestSuite {

    private static final String RESVG_TEST_SUITE_PATH = System.getenv("RESVG_TEST_SUITE_PATH");

    static Collection<DynamicTest> checkDirectory(@NotNull String name, Collection<String> exclude) {
        try {
            Path basePath = Path.of(new URI("file://" + RESVG_TEST_SUITE_PATH));
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
        } catch (URISyntaxException e) {
            Assertions.fail(e);
        }
        return Collections.emptyList();
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

    private static final class ReSVGRefTest implements Executable {
        private final @NotNull Path testFile;

        private ReSVGRefTest(@NotNull Path testFile) {
            this.testFile = testFile;
        }

        @Override
        public void execute() throws Throwable {
            var pngRef = testFile.resolveSibling(testFile.getFileName().toString().replace(".svg", ".png"));
            var result = ReferenceTest.compareImages(new ReferenceTest.CompareInfo(
                    new ReferenceTest.ImageInfo(
                            new UrlImageSource(pngRef.toUri().toURL()),
                            RenderType.DiskImage),
                    new ReferenceTest.ImageInfo(
                            new UrlImageSource(testFile.toUri().toURL()),
                            RenderType.JSVG),
                    1f, 10f));
            assertEquals(SUCCESS, result);
        }
    }
}
