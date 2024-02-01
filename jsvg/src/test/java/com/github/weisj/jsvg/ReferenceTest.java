/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.github.romankh3.image.comparison.model.Rectangle;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public final class ReferenceTest {

    private static final double DEFAULT_TOLERANCE = 0.5;

    @Test
    void testIcons() {
        String[] iconNames = {"desktop.svg", "drive.svg", "folder.svg", "general.svg", "homeFolder.svg", "image.svg",
                "missingImage.svg", "newFolder.svg", "pendingImage.svg", "text.svg", "unknown.svg", "upFolder.svg"};
        for (String iconName : iconNames) {
            Assertions.assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/" + iconName));
        }
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String fileName) {
        return compareImages(fileName, DEFAULT_TOLERANCE);
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String fileName, double tolerance) {
        return compareImages(fileName, Objects.requireNonNull(ReferenceTest.class.getResource(fileName), fileName),
                tolerance);
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String name, @NotNull URL url,
            double tolerance) {
        try {
            BufferedImage expected = renderReference(url.openStream());
            BufferedImage actual = render(url.openStream());
            return compareImageRasterization(expected, actual, name, tolerance);
        } catch (Exception e) {
            Assertions.fail(name, e);
            return ReferenceTestResult.FAILURE;
        }
    }

    static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String name, @NotNull String svgContent) {
        return compareImages(name, svgContent, DEFAULT_TOLERANCE);
    }

    static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String name, @NotNull String svgContent,
            double tolerance) {
        try {
            BufferedImage expected =
                    renderReference(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
            BufferedImage actual = render(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
            return compareImageRasterization(expected, actual, name, tolerance);
        } catch (Exception e) {
            Assertions.fail(name, e);
            return ReferenceTestResult.FAILURE;
        }
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImageRasterization(@NotNull BufferedImage expected,
            @NotNull BufferedImage actual,
            @NotNull String name, double tolerance) {
        ImageComparison comp = new ImageComparison(expected, actual);
        comp.setAllowingPercentOfDifferentPixels(tolerance);
        ImageComparisonResult comparison = comp.compareImages();
        ImageComparisonState state = comparison.getImageComparisonState();
        if (state == ImageComparisonState.MISMATCH && comparison.getDifferencePercent() <= tolerance) {
            state = ImageComparisonState.MATCH;
        }
        return new ReferenceTestResult(state, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Image: ").append(name).append('\n');
            sb.append("Expected size: ").append(expected.getWidth()).append('x').append(expected.getHeight())
                    .append('\n');
            sb.append("Actual size: ").append(actual.getWidth()).append('x').append(actual.getHeight())
                    .append('\n');
            sb.append("Difference: ").append(comparison.getDifferencePercent()).append('%')
                    .append('\n');
            List<Rectangle> rects = comparison.getRectangles();
            if (rects != null) {
                for (Rectangle rectangle : rects) {
                    sb.append("Different in region: ")
                            .append(rectangle.getMinPoint().x)
                            .append(',')
                            .append(rectangle.getMinPoint().y)
                            .append(',')
                            .append(rectangle.getMaxPoint().x)
                            .append(',')
                            .append(rectangle.getMaxPoint().y)
                            .append('\n');
                }
                ImageComparisonUtil.saveImage(new File(name.replaceAll("[- /]", "_") + "_diff.png"),
                        comparison.getResult());
                ImageComparisonUtil.saveImage(new File(name.replaceAll("[- /]", "_") + "_expected.png"),
                        comparison.getExpected());
                ImageComparisonUtil.saveImage(new File(name.replaceAll("[- /]", "_") + "_actual.png"),
                        comparison.getActual());
            }
            return sb.toString();
        });
    }

    public static BufferedImage render(@NotNull String path) {
        try {
            return render(Objects.requireNonNull(ReferenceTest.class.getResource(path)).openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull RenderingHints referenceHintSet() {
        return new RenderingHints(Map.of(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON,
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE,
                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON,
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
    }

    private static BufferedImage render(@NotNull InputStream inputStream) {
        SVGDocument document = Objects.requireNonNull(new SVGLoader().load(inputStream));
        FloatSize size = document.size();
        BufferedImage image = new ReferenceImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        document.render(null, g, new ViewBox(size));
        g.dispose();
        return image;
    }

    private static BufferedImage renderReference(@NotNull InputStream inputStream) throws IOException {
        final BufferedImage[] imagePointer = new BufferedImage[1];

        TranscodingHints transcoderHints = new TranscodingHints();
        transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
        transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
        transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
        transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");

        try {
            TranscoderInput input = new TranscoderInput(inputStream);
            ImageTranscoder t = new ImageTranscoder() {

                @Override
                public BufferedImage createImage(int w, int h) {
                    return new ReferenceImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }

                @Override
                public void writeImage(BufferedImage image, TranscoderOutput out) {
                    imagePointer[0] = image;
                }
            };
            t.setTranscodingHints(transcoderHints);
            t.transcode(input, null);
        } catch (TranscoderException ex) {
            throw new IOException("Couldn't convert image", ex);
        }
        return imagePointer[0];
    }

    private static class ReferenceImage extends BufferedImage {

        public ReferenceImage(int width, int height, int imageType) {
            super(width, height, imageType);
        }

        @Override
        public Graphics2D createGraphics() {
            Graphics2D g = super.createGraphics();
            g.setRenderingHints(referenceHintSet());
            return g;
        }
    }

    public static final class ReferenceTestResult {
        public static final @NotNull ReferenceTest.ReferenceTestResult SUCCESS =
                new ReferenceTestResult(ImageComparisonState.MATCH, () -> "SUCCESS");
        public static final @NotNull ReferenceTest.ReferenceTestResult FAILURE =
                new ReferenceTestResult(ImageComparisonState.MATCH, () -> "FAILURE");

        private final @NotNull ImageComparisonState comparisonState;
        private final @NotNull Supplier<@NotNull String> failureLogSupplier;
        private String failureMessage;

        ReferenceTestResult(
                @NotNull ImageComparisonState comparisonState,
                @NotNull Supplier<@NotNull String> failureLogSupplier) {
            this.comparisonState = comparisonState;
            this.failureLogSupplier = failureLogSupplier;
        }

        @Override
        public String toString() {
            if (failureMessage == null) {
                failureMessage = failureLogSupplier.get();
            }
            return failureMessage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReferenceTestResult that)) return false;
            return comparisonState == that.comparisonState;
        }

        @Override
        public int hashCode() {
            return comparisonState.hashCode();
        }
    }
}
