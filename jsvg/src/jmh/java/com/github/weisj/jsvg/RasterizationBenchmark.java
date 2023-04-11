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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.util.Objects;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.kitfox.svg.app.beans.SVGIcon;

public final class RasterizationBenchmark {
    private static final String SVG_IMAGE = "benchmark_image.svg";

    @State(Scope.Benchmark)
    public static final class JSVGRasterization {
        private SVGDocument document;

        @Setup
        public void loadDocument() {
            document = new SVGLoader()
                    .load(Objects.requireNonNull(RasterizationBenchmark.class.getResourceAsStream(SVG_IMAGE)));
        }

        @Benchmark
        @Fork(value = 1)
        @BenchmarkMode(Mode.AverageTime)
        public void rasterize(@NotNull Blackhole blackhole) {
            FloatSize size = document.size();
            BufferedImage img = new BufferedImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) img.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            document.render(null, g, new ViewBox(size));
            g.dispose();
            blackhole.consume(img);
        }
    }

    @State(Scope.Benchmark)
    public static final class SVGSalamanderRasterization {
        private SVGIcon icon;

        @Setup
        public void loadDocument() throws URISyntaxException {
            icon = new SVGIcon();
            icon.setAntiAlias(true);
            icon.setSvgURI(Objects.requireNonNull(RasterizationBenchmark.class.getResource(SVG_IMAGE)).toURI());
        }

        @Benchmark
        @Fork(value = 1)
        @BenchmarkMode(Mode.AverageTime)
        public void rasterize(@NotNull Blackhole blackhole) {
            blackhole.consume(icon.getImage());
        }
    }

    @State(Scope.Thread)
    public static final class BatikRasterization {

        private static final class BufferedImageTranscoder extends ImageTranscoder {
            private BufferedImage image;

            @Override
            public BufferedImage createImage(int w, int h) {
                return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }

            @Override
            public void writeImage(BufferedImage image, TranscoderOutput out) {
                this.image = image;
            }
        }

        private BufferedImageTranscoder transcoder;

        @Setup
        public void setup() {
            TranscodingHints hints = new TranscodingHints();
            hints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
            hints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
            hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
            hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
            transcoder = new BufferedImageTranscoder();
            transcoder.setTranscodingHints(hints);
        }

        @Benchmark
        @Fork(value = 1)
        @BenchmarkMode(Mode.AverageTime)
        public void rasterize(@NotNull Blackhole blackhole) throws TranscoderException {
            TranscoderInput input = new TranscoderInput(
                    Objects.requireNonNull(RasterizationBenchmark.class.getResourceAsStream(SVG_IMAGE)));
            transcoder.transcode(input, null);
            blackhole.consume(transcoder.image);
        }
    }

}
