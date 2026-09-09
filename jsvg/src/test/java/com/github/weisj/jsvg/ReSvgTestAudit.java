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

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;

/** Optional audit launched by :jsvg:resvgTestAudit; not part of the regular test task. */
public final class ReSvgTestAudit {
    private static final String WORKER = "--case";

    private ReSvgTestAudit() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && WORKER.equals(args[0])) {
            runReference(Path.of(args[1]));
            return;
        }
        Path base = Path.of(System.getenv("RESVG_TEST_SUITE_PATH")).toAbsolutePath();
        if (!Files.isDirectory(base) || !Files.isDirectory(base.getParent().resolve("fonts"))) {
            throw new IOException("Initialize the resvg-test-suite submodule, including its fonts, before auditing.");
        }
        Path report = Path.of(System.getProperty("resvg.audit.reportDir")).toAbsolutePath();
        Files.createDirectories(report);
        Set<Path> enabled = enabledTests();
        List<Path> candidates;
        try (var paths = Files.walk(base)) {
            candidates = paths.filter(path -> path.toString().endsWith(".svg"))
                    .filter(path -> !enabled.contains(path))
                    .filter(path -> args.length == 0 || base.relativize(path).startsWith(args[0]))
                    .sorted().toList();
        }
        // Check the runner passes an enabled test to avoid invalid reports.
        Path control = enabled.stream().sorted().findFirst().orElseThrow();
        if (!"PASS".equals(check(control, report.resolve("control")))) {
            throw new IOException(
                    "The enabled control failed: " + base.relativize(control) + "; see control/render.log");
        }
        System.out.println("Checking " + candidates.size() + " resvg cases omitted by ReSvgTestSuite.");
        List<String> passing = new ArrayList<>();
        int timeouts = 0;
        int errors = 0;
        try (var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                BufferedWriter results = Files.newBufferedWriter(report.resolve("results.tsv"))) {
            results.write("status\tpath\n");
            List<Future<String>> checks = new ArrayList<>();
            for (Path candidate : candidates) {
                checks.add(executor
                        .submit(() -> check(candidate, report.resolve("cases").resolve(base.relativize(candidate)))));
            }
            for (int i = 0; i < candidates.size(); i++) {
                String name = base.relativize(candidates.get(i)).toString().replace('\\', '/');
                String status = checks.get(i).get();
                results.write(status + "\t" + name + "\n");
                results.flush();
                if ("PASS".equals(status)) {
                    passing.add(name);
                } else if ("TIMEOUT".equals(status)) {
                    timeouts++;
                } else if ("ERROR".equals(status)) {
                    errors++;
                }
                if ((i + 1) % 100 == 0) {
                    System.out
                            .println("Checked " + (i + 1) + "/" + candidates.size() + "; " + passing.size() + " pass.");
                }
            }
        }
        Files.write(report.resolve("passing.txt"), passing);
        StringBuilder summary = new StringBuilder("# Excluded resvg tests\n\n")
                .append("Checked ").append(candidates.size()).append(" excluded cases: **")
                .append(passing.size()).append(" pass**, ").append(timeouts).append(" timed out, ")
                .append(errors).append(" exited with errors. The rest failed.\n\n")
                .append("Runner: ").append(System.getProperty("os.name")).append(", Java ")
                .append(System.getProperty("java.version")).append(".\n\n")
                .append("These are candidates for enabling in `ReSvgTestSuite`.");
        if (passing.isEmpty()) {
            summary.append("No excluded cases passed.\n");
        } else {
            summary.append("## Passing candidates\n\n");
            for (String path : passing) {
                summary.append("- `").append(path).append("`\n");
            }
        }
        Files.writeString(report.resolve("summary.md"), summary);
        System.out.println(passing.size() + " passing candidates. Report: " + report.resolve("summary.md"));
    }

    private static Set<Path> enabledTests() throws Exception {
        Set<Path> enabled = new HashSet<>();
        ReSvgTestSuite suite = new ReSvgTestSuite();
        // Read the same factories JUnit uses, so directory additions and exclusions have one source of
        // truth.
        for (Method method : ReSvgTestSuite.class.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(TestFactory.class) || method.isAnnotationPresent(Disabled.class)) {
                continue;
            }
            for (Object test : (Collection<?>) method.invoke(suite)) {
                var reference = (ReSvgTestSuite.ReSVGRefTest) ((DynamicTest) test).getExecutable();
                enabled.add(reference.testFile().toAbsolutePath());
            }
        }
        return enabled;
    }

    private static String check(Path source, Path directory) throws Exception {
        Files.createDirectories(directory);
        String java = ProcessHandle.current().info().command().orElseThrow();
        Process process = new ProcessBuilder(java, "-Xmx512m", "-XX:+ExitOnOutOfMemoryError",
                "-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"),
                ReSvgTestAudit.class.getName(), WORKER, source.toString())
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(directory.resolve("render.log").toFile())
                .start();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) return "TIMEOUT";
            return switch (process.exitValue()) {
                case 0 -> "PASS";
                case 1 -> "FAIL";
                default -> "ERROR";
            };
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }

    private static void runReference(Path source) {
        try {
            ReSvgTestSuite.checkForReSVGRepositoryAndRegisterFonts();
            new ReSvgTestSuite.ReSVGRefTest(source).execute();
        } catch (AssertionFailedError failure) {
            failure.printStackTrace();
            System.exit(1);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(2);
        }
    }
}
