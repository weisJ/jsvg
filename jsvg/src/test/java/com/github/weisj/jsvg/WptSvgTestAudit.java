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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.weisj.jsvg.SvgTestAudit.Reference;

/** Optional audit launched by :jsvg:wptSvgTestAudit; not part of the regular test task. */
public final class WptSvgTestAudit {
    private WptSvgTestAudit() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && SvgTestAudit.WORKER.equals(args[0])) {
            SvgTestAudit.runReference(() -> {
                WptSvgTestSuite.checkForWptRepository();
                new WptSvgTestSuite.WptSvgRefTest(Path.of(args[1])).execute();
            });
            return;
        }
        Path repository = SvgTestAudit.suitePath("WPT_TEST_SUITE_PATH");
        if (repository == null) return;
        Path base = repository.resolve("svg");
        if (!Files.isDirectory(base)) {
            System.out.println("Skipping audit: the optional WPT SVG checkout is absent.");
            return;
        }
        Path report = Path.of(System.getProperty("wpt.audit.reportDir")).toAbsolutePath();
        List<Reference> candidates = WptSvgTestSuite.excludedTests().stream()
                .filter(name -> args.length == 0 || name.startsWith(args[0]))
                .sorted()
                .map(name -> new Reference(name, base.resolve(name))).toList();
        // This enabled reference compares an Ahem glyph with a solid rectangle, checking that
        // bundled-font registration works in the isolated worker as well as in regular tests.
        String control = "text/reftests/dominant-baseline-hanging-small-font-size.svg";
        if (WptSvgTestSuite.excludedTests().contains(control)) {
            throw new IllegalStateException("The WPT audit control must be an enabled reference.");
        }
        SvgTestAudit.audit("WPT SVG", WptSvgTestAudit.class, report, candidates,
                new Reference(control, base.resolve(control)));
    }
}
