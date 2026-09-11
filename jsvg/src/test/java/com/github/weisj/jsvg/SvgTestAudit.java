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
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/** Shared process isolation and reporting for the optional reference-suite audits. */
final class SvgTestAudit {
    static final String WORKER = "--case";
    // HotSpot exits with code 3 on -XX:+ExitOnOutOfMemoryError; keep intentional skips distinct.
    private static final int SKIPPED_EXIT_CODE = 10;

    private SvgTestAudit() {}

    record Reference(String name, Path source, Long timestamp) {
        Reference(String name, Path source) {
            this(name, source, null);
        }
    }

    private enum Status {
        PASS,
        FAIL,
        ERROR,
        SKIP,
        TIMEOUT
    }

    static Path suitePath(String variable) {
        String path = System.getenv(variable);
        if (path == null || path.isBlank() || !Files.exists(Path.of(path))) {
            System.out.println("Skipping audit: optional suite " + variable + " is absent.");
            return null;
        }
        return Path.of(path).toAbsolutePath();
    }

    static List<Executable> discoveredTests(Object suite) throws Exception {
        List<Executable> discovered = new ArrayList<>();
        for (Method method : suite.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(TestFactory.class) || method.isAnnotationPresent(Disabled.class)) continue;
            for (Object test : (Collection<?>) method.invoke(suite)) {
                discovered.add(((DynamicTest) test).getExecutable());
            }
        }
        return discovered;
    }

    static void audit(String suiteName, Class<?> worker, Path report, List<Reference> candidates,
            Reference control) throws Exception {
        Files.createDirectories(report);
        if (check(worker, control, report.resolve("control")) != Status.PASS) {
            throw new IOException("The enabled control failed: " + control.name() + "; see control/render.log");
        }
        System.out.println("Checking " + candidates.size() + " excluded " + suiteName + " cases.");
        List<String> passing = new ArrayList<>();
        EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
        // Each worker may use up to 512 MB. Bound parallelism even on machines with many cores.
        int parallelism = Math.min(4, Runtime.getRuntime().availableProcessors());
        try (var executor = Executors.newFixedThreadPool(parallelism);
                BufferedWriter results = Files.newBufferedWriter(report.resolve("results.tsv"))) {
            results.write("status\tpath\n");
            List<Future<Status>> checks = new ArrayList<>();
            for (Reference candidate : candidates) {
                checks.add(executor
                        .submit(() -> check(worker, candidate, report.resolve("cases").resolve(candidate.name()))));
            }
            for (int i = 0; i < candidates.size(); i++) {
                Reference candidate = candidates.get(i);
                Status status = checks.get(i).get();
                results.write(status + "\t" + candidate.name() + "\n");
                results.flush();
                counts.merge(status, 1, Integer::sum);
                if (status == Status.PASS) passing.add(candidate.name());
                if ((i + 1) % 100 == 0) {
                    System.out
                            .println("Checked " + (i + 1) + "/" + candidates.size() + "; " + passing.size() + " pass.");
                }
            }
        }
        Files.write(report.resolve("passing.txt"), passing);
        StringBuilder summary = new StringBuilder("# Excluded ").append(suiteName).append(" tests\n\n")
                .append("Checked ").append(candidates.size()).append(" excluded cases: **")
                .append(passing.size()).append(" pass**, ").append(counts.getOrDefault(Status.FAIL, 0))
                .append(" fail, ")
                .append(counts.getOrDefault(Status.SKIP, 0)).append(" skipped, ")
                .append(counts.getOrDefault(Status.TIMEOUT, 0)).append(" timed out, ")
                .append(counts.getOrDefault(Status.ERROR, 0)).append(" exited with errors.\n\n")
                .append("Runner: ").append(System.getProperty("os.name")).append(", Java ")
                .append(System.getProperty("java.version")).append(".\n\n")
                .append("These are candidates for enabling in the suite. Animation entries identify individual frames. ")
                .append("Skipped cases are not passes; see each case's `render.log` for the reason.\n\n");
        if (passing.isEmpty()) {
            summary.append("No excluded cases passed.\n");
        } else {
            summary.append("## Passing candidates\n\n");
            for (String path : passing)
                summary.append("- `").append(path).append("`\n");
        }
        Files.writeString(report.resolve("summary.md"), summary);
        System.out.println(passing.size() + " passing candidates. Report: " + report.resolve("summary.md"));
    }

    private static Status check(Class<?> worker, Reference reference, Path directory) throws Exception {
        Files.createDirectories(directory);
        String java = ProcessHandle.current().info().command().orElseThrow();
        List<String> command = new ArrayList<>(List.of(java, "-Xmx512m", "-XX:+ExitOnOutOfMemoryError",
                "-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"), worker.getName(), WORKER,
                reference.source().toString()));
        if (reference.timestamp() != null) command.add(Long.toString(reference.timestamp()));
        Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
                .redirectOutput(directory.resolve("render.log").toFile()).start();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) return Status.TIMEOUT;
            return switch (process.exitValue()) {
                case 0 -> Status.PASS;
                case 1 -> Status.FAIL;
                case SKIPPED_EXIT_CODE -> Status.SKIP;
                default -> Status.ERROR;
            };
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }

    static void runReference(Executable reference) {
        try {
            reference.execute();
        } catch (TestAbortedException skipped) {
            System.out.println(skipped.getMessage());
            System.exit(SKIPPED_EXIT_CODE);
        } catch (AssertionFailedError failure) {
            failure.printStackTrace();
            System.exit(1);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(2);
        }
    }
}
