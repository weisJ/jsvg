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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/** Verify the WPT adapter even when the optional submodule is absent. */
class WptSvgTestSuiteTest {
    @TempDir
    Path directory;

    @Test
    void linkedArtworkAndRelativeResources() throws Throwable {
        Files.createDirectories(directory.resolve("references"));
        write("shape.svg", "<rect id='blue' width='16' height='16' fill='blue'/>");
        write("references/shape.svg", "<path id='blue' d='M0 0H16V16H0Z' fill='blue'/>");
        write("references/ref.svg", "<use href='shape.svg#blue'/>");
        Path test = write("test.svg", """
                <h:link rel="match" href="references/ref.svg"/>
                <use href="shape.svg#blue"/>
                """);
        new WptSvgTestSuite.WptSvgRefTest(test).execute();
        // Both documents must retain their own URI, and the linked artwork must be the oracle.
        write("references/shape.svg", "<path id='blue' d='M0 0H16V16H0Z' fill='red'/>");
        assertThrows(AssertionFailedError.class, new WptSvgTestSuite.WptSvgRefTest(test));
    }

    @Test
    void fuzzyLimitsCountPixelsAndMaximumChannelDifference() throws Throwable {
        write("ref.svg", "<rect width='1' height='1' fill='rgb(100,100,100)'/>");
        Path test = write("test.svg", """
                <h:link rel="match" href="ref.svg"/>
                <h:meta name="fuzzy" content="maxDifference=0-2; totalPixels=0-1"/>
                <rect width="1" height="1" fill="rgb(102,100,100)"/>
                """);
        new WptSvgTestSuite.WptSvgRefTest(test).execute();
        String source = Files.readString(test);
        Files.writeString(test, source.replace("0-2", "0-1"));
        assertThrows(AssertionFailedError.class, new WptSvgTestSuite.WptSvgRefTest(test));
        write("ref.svg", "<rect width='2' height='1' fill='rgb(100,100,100)'/>");
        Files.writeString(test, source.replace("width=\"1\"", "width=\"2\""));
        assertThrows(AssertionFailedError.class, new WptSvgTestSuite.WptSvgRefTest(test));
    }

    @Test
    void exactComparisonDetectsOnePixel() throws Exception {
        write("ref.svg", "");
        Path test = write("test.svg", """
                <h:link rel="match" href="ref.svg"/>
                <rect width="1" height="1"/>
                """);
        assertThrows(AssertionFailedError.class, new WptSvgTestSuite.WptSvgRefTest(test));
    }

    @Test
    void scriptsInReferencesAreSkipped() throws Exception {
        write("ref.svg", "<script>document.documentElement.setAttribute('fill', 'green');</script>");
        Path test = write("test.svg", "<h:link rel='match' href='ref.svg'/>");
        assertThrows(TestAbortedException.class, new WptSvgTestSuite.WptSvgRefTest(test));
        write("ref.svg", "");
        Files.writeString(test, Files.readString(test).replace("<svg ", "<svg onload='run()' "));
        assertThrows(TestAbortedException.class, new WptSvgTestSuite.WptSvgRefTest(test));
    }

    @Test
    void unsupportedReferenceSemanticsAreSkipped() throws Exception {
        write("ref.svg", "");
        Path test = write("test.svg", "<h:link rel='mismatch' href='ref.svg'/>");
        assertThrows(TestAbortedException.class, new WptSvgTestSuite.WptSvgRefTest(test));
        write("test.svg", "<h:link rel='match' href='ref.html'/>");
        assertThrows(TestAbortedException.class, new WptSvgTestSuite.WptSvgRefTest(test));
        write("test.svg", "<h:link rel='match' href='ref.svg'/><h:link rel='match' href='other.svg'/>");
        assertThrows(TestAbortedException.class, new WptSvgTestSuite.WptSvgRefTest(test));
    }

    @Test
    void missingSvgReferenceFails() throws Exception {
        Path test = write("test.svg", "<h:link rel='match' href='missing.svg'/>");
        assertThrows(AssertionFailedError.class, new WptSvgTestSuite.WptSvgRefTest(test));
    }

    private Path write(String name, String content) throws Exception {
        return Files.writeString(directory.resolve(name), """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:h="http://www.w3.org/1999/xhtml">
                %s
                </svg>
                """.formatted(content));
    }
}
