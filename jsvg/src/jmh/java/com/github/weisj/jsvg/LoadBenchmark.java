/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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

import java.net.URISyntaxException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

import com.github.weisj.jsvg.parser.SVGLoader;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

public final class LoadBenchmark {

    private static final String SVG_IMAGE = "benchmark_image.svg";

    @Benchmark
    @Fork(value = 1)
    @BenchmarkMode(Mode.AverageTime)
    public void jsvgLoading(@NotNull Blackhole blackhole) {
        SVGDocument document = new SVGLoader()
                .load(Objects.requireNonNull(LoadBenchmark.class.getResourceAsStream(SVG_IMAGE)));
        blackhole.consume(document);
    }

    @Benchmark
    @Fork(value = 1)
    @BenchmarkMode(Mode.AverageTime)
    public void svgSalamanderLoading(@NotNull Blackhole blackhole) throws URISyntaxException {
        SVGUniverse universe = new SVGUniverse();
        SVGDiagram diagram =
                universe.getDiagram(Objects.requireNonNull(LoadBenchmark.class.getResource(SVG_IMAGE)).toURI());
        blackhole.consume(diagram);
    }
}
