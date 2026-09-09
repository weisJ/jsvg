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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.weisj.jsvg.SvgTestAudit.Reference;

/** Optional audit launched by :jsvg:resvgTestAudit; not part of the regular test task. */
public final class ReSvgTestAudit {
    private ReSvgTestAudit() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && SvgTestAudit.WORKER.equals(args[0])) {
            SvgTestAudit.runReference(() -> {
                ReSvgTestSuite.checkForReSVGRepositoryAndRegisterFonts();
                new ReSvgTestSuite.ReSVGRefTest(Path.of(args[1])).execute();
            });
            return;
        }
        Path base = SvgTestAudit.suitePath("RESVG_TEST_SUITE_PATH");
        if (base == null) return;
        Path report = Path.of(System.getProperty("resvg.audit.reportDir")).toAbsolutePath();
        Set<Path> enabled = SvgTestAudit.enabledTests(new ReSvgTestSuite()).stream()
                .map(test -> ((ReSvgTestSuite.ReSVGRefTest) test).testFile().toAbsolutePath())
                .collect(Collectors.toSet());
        List<Reference> candidates;
        try (var paths = Files.walk(base)) {
            candidates = paths.filter(path -> path.toString().endsWith(".svg"))
                    .filter(path -> !enabled.contains(path))
                    .filter(path -> args.length == 0 || base.relativize(path).startsWith(args[0]))
                    .map(path -> new Reference(base.relativize(path).toString().replace('\\', '/'), path))
                    .sorted(Comparator.comparing(Reference::name)).toList();
        }
        Path control = enabled.stream().sorted().findFirst().orElseThrow();
        SvgTestAudit.audit("resvg", ReSvgTestAudit.class, report, candidates,
                new Reference(base.relativize(control).toString(), control));
    }
}
