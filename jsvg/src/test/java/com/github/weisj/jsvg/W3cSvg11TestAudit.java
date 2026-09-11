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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.opentest4j.TestAbortedException;

import com.github.weisj.jsvg.SvgTestAudit.Reference;
import com.github.weisj.jsvg.W3cSvg11TestSuite.W3cSvg11AnimationFrame;
import com.github.weisj.jsvg.W3cSvg11TestSuite.W3cSvg11RefTest;

/** Optional audit launched by :jsvg:w3cSvg11TestAudit; not part of the regular test task. */
public final class W3cSvg11TestAudit {
    private W3cSvg11TestAudit() {}

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && SvgTestAudit.WORKER.equals(args[0])) {
            SvgTestAudit.runReference(() -> {
                Path source = Path.of(args[1]);
                if (args.length == 3) {
                    new W3cSvg11AnimationFrame(source, Long.parseLong(args[2])).execute();
                } else {
                    String category = source.getFileName().toString().split("-", 2)[0];
                    if (Set.of("animate", "interact", "imp", "script", "svgdom").contains(category)) {
                        throw new TestAbortedException("Requires curated animation frames, interaction or scripting.");
                    }
                    new W3cSvg11RefTest(source).execute();
                }
            });
            return;
        }
        Path base = SvgTestAudit.suitePath("W3C_SVG_11_TEST_SUITE_PATH");
        if (base == null) return;
        Path report = Path.of(System.getProperty("w3c.audit.reportDir")).toAbsolutePath();
        Set<Reference> enabled = new HashSet<>();
        Set<Path> animatedSources = new HashSet<>();
        for (var test : SvgTestAudit.discoveredTests(new W3cSvg11TestSuite())) {
            if (test instanceof W3cSvg11RefTest reference && !reference.excluded()) {
                enabled.add(reference(base, reference.testFile(), null));
            } else if (test instanceof W3cSvg11AnimationFrame(Path testFile, long timestamp)) {
                enabled.add(reference(base, testFile, timestamp));
                animatedSources.add(testFile);
            }
        }
        List<Reference> candidates = new ArrayList<>();
        try (var paths = Files.list(base.resolve("svg"))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".svg") || path.toString().endsWith(".svgz"))
                    .filter(Predicate.not(animatedSources::contains))
                    .map(path -> reference(base, path, null)).forEach(candidates::add);
        }
        // Curated terminal frames for the timelines already covered by the suite. Never promote
        // an animation based on its initial static rendering; add explicit samples when auditing more.
        candidates.add(reference(base, base.resolve("svg/animate-elem-22-b.svg"), 9000L));
        candidates.add(reference(base, base.resolve("svg/animate-elem-25-t.svg"), 6000L));
        candidates.add(reference(base, base.resolve("svg/animate-elem-25-t.svg"), 9000L));
        candidates.add(reference(base, base.resolve("svg/animate-elem-26-t.svg"), 5000L));
        candidates.add(reference(base, base.resolve("svg/animate-elem-26-t.svg"), 7000L));
        candidates.add(reference(base, base.resolve("svg/animate-elem-28-t.svg"), 4000L));
        candidates.add(reference(base, base.resolve("svg/animate-elem-88-t.svg"), 4000L));
        candidates.removeIf(enabled::contains);
        candidates.removeIf(reference -> args.length > 0 && !reference.name().startsWith(args[0]));
        candidates.sort(Comparator.comparing(Reference::name));
        Reference control = enabled.stream().filter(reference -> reference.timestamp() == null)
                .sorted(Comparator.comparing(Reference::name)).findFirst().orElseThrow();
        SvgTestAudit.audit("W3C SVG 1.1", W3cSvg11TestAudit.class, report, candidates, control);
    }

    private static Reference reference(Path base, Path source, Long timestamp) {
        String name = base.relativize(source).toString().replace('\\', '/');
        if (timestamp != null) name += "@" + timestamp + "ms";
        return new Reference(name, source, timestamp);
    }
}
