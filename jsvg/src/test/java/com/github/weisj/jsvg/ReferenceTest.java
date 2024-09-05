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

import static com.github.weisj.jsvg.ReferenceTest.RenderType.Batik;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.github.romankh3.image.comparison.model.Rectangle;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.ExternalResourcePolicy;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public final class ReferenceTest {

    private static final double DEFAULT_TOLERANCE = 0.3;
    private static final double DEFAULT_PIXEL_TOLERANCE = 0.1;
    public static Object SOFT_CLIPPING_VALUE = SVGRenderingHints.VALUE_SOFT_CLIPPING_OFF;

    public sealed interface RenderType {
        BatikType Batik = new BatikType();
        JSVGType JSVG = new JSVGType(LoaderContext.builder()
                .externalResourcePolicy(ExternalResourcePolicy.ALLOW_ALL)
                .build());

        record BatikType() implements RenderType {
        }
        record JSVGType(@NotNull LoaderContext loaderContext) implements RenderType {
        }
    }

    public sealed interface ImageSource {
        @NotNull
        String name();

        @NotNull
        InputStream openStream() throws IOException;

        @Nullable
        URL url();

        record PathImageSource(@NotNull String path) implements ImageSource {

            @Override
            public @NotNull String name() {
                return path;
            }

            @Override
            public @Nullable URL url() {
                return ReferenceTest.class.getResource(path);
            }

            @Override
            public @NotNull InputStream openStream() throws IOException {
                return Objects.requireNonNull(url()).openStream();
            }
        }

        record MemoryImageSource(@NotNull String name, @NotNull String data) implements ImageSource {

            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @Nullable URL url() {
                return null;
            }

            @Override
            public @NotNull InputStream openStream() {
                return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public static class ImageInfo {
        private final @NotNull ImageSource source;
        private final @NotNull RenderType renderType;
        private final @Nullable Consumer<Graphics2D> graphicsMutator;

        public ImageInfo(@NotNull ImageSource source, @NotNull RenderType renderType) {
            this(source, renderType, null);
        }

        public ImageInfo(@NotNull ImageSource source, @NotNull RenderType renderType,
                @Nullable Consumer<Graphics2D> graphicsMutator) {
            this.source = source;
            this.renderType = renderType;
            this.graphicsMutator = graphicsMutator;
        }

        @NotNull
        BufferedImage render() throws IOException {
            return switch (renderType) {
                case RenderType.BatikType() -> renderBatik(source.openStream());
                case RenderType.JSVGType(LoaderContext loaderContext) ->
                    renderJsvg(source, graphicsMutator, loaderContext);
            };
        }
    }

    public record CompareInfo(@NotNull ImageInfo expected, @NotNull ImageInfo actual, double tolerance,
            double pixelTolerance) {

        public CompareInfo(@NotNull ImageInfo expected, @NotNull ImageInfo actual) {
            this(expected, actual, DEFAULT_TOLERANCE, DEFAULT_PIXEL_TOLERANCE);
        }

        @NotNull
        String name() {
            String aName = expected.source.name();
            String bName = actual.source.name();
            return aName.equals(bName) ? aName : aName + " vs " + bName;
        }
    }

    @Test
    void testIcons() {
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/desktop.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/drive.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/folder.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/general.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/homeFolder.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/image.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/missingImage.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/newFolder.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/pendingImage.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/text.svg"));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/unknown.svg", 0.4));
        assertEquals(ReferenceTestResult.SUCCESS, compareImages("icons/upFolder.svg"));
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String fileName) {
        return compareImages(fileName, DEFAULT_TOLERANCE);
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String fileName, double tolerance) {
        return compareImages(fileName, tolerance, DEFAULT_PIXEL_TOLERANCE);
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String fileName, double tolerance,
            double pixelTolerance) {
        return compareImages(new CompareInfo(
                new ImageInfo(new ImageSource.PathImageSource(fileName), Batik),
                new ImageInfo(new ImageSource.PathImageSource(fileName), RenderType.JSVG),
                tolerance, pixelTolerance));
    }

    static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String name, @NotNull String svgContent) {
        return compareImages(name, svgContent, DEFAULT_TOLERANCE);
    }

    static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull CompareInfo compareInfo) {
        try {
            BufferedImage expected = compareInfo.expected.render();
            BufferedImage actual = compareInfo.actual.render();
            return compareImageRasterization(expected, actual, compareInfo.name(),
                    compareInfo.tolerance(), compareInfo.pixelTolerance());
        } catch (Exception e) {
            Assertions.fail(compareInfo.name(), e);
            return ReferenceTestResult.FAILURE;
        }
    }

    static @NotNull ReferenceTest.ReferenceTestResult compareImages(@NotNull String name, @NotNull String svgContent,
            double tolerance) {
        return compareImages(new CompareInfo(
                new ImageInfo(new ImageSource.MemoryImageSource(name, svgContent), Batik),
                new ImageInfo(new ImageSource.MemoryImageSource(name, svgContent), RenderType.JSVG),
                tolerance, DEFAULT_PIXEL_TOLERANCE));
    }

    public static @NotNull ReferenceTest.ReferenceTestResult compareImageRasterization(
            @NotNull BufferedImage expected, @NotNull BufferedImage actual,
            @NotNull String name, double tolerance, double pixelTolerance) {
        ImageComparison comp = new ImageComparison(expected, actual);
        comp.setAllowingPercentOfDifferentPixels(tolerance);
        comp.setPixelToleranceLevel(pixelTolerance);
        ImageComparisonResult comparison = comp.compareImages();
        ImageComparisonState state = comparison.getImageComparisonState();
        if (state == ImageComparisonState.MISMATCH && comparison.getDifferencePercent() <= tolerance) {
            return ReferenceTestResult.SUCCESS;
        }
        return new ReferenceTestResult(state, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Image: ").append(name).append('\n');
            sb.append("Expected size: ").append(expected.getWidth()).append('x').append(expected.getHeight())
                    .append('\n');
            sb.append("Actual size: ").append(actual.getWidth()).append('x').append(actual.getHeight())
                    .append('\n');
            sb.append("Difference: ").append(comparison.getDifferencePercent()).append('%')
                    .append(" > ").append(tolerance).append('%')
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
                            .append(rectangle.getMaxPoint().y);
                    Point p = rectangle.getMinPoint();
                    Color expectedPixel = new Color(expected.getRGB(p.x, p.y), true);
                    Color actualPixel = new Color(actual.getRGB(p.x, p.y), true);
                    Consumer<Color> printPixel = (Color c) -> {
                        sb.append('[')
                                .append(c.getRed()).append(' ')
                                .append(c.getGreen()).append(' ')
                                .append(c.getBlue()).append(' ')
                                .append(c.getAlpha())
                                .append(']');
                    };
                    sb.append(" => Expected pixel: ");
                    printPixel.accept(expectedPixel);
                    sb.append(" Actual pixel: ");
                    printPixel.accept(actualPixel);
                    sb.append('\n');
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

    public static @NotNull BufferedImage renderJsvg(@NotNull String path) {
        try {
            return renderJsvg(new ImageSource.PathImageSource(path), null, RenderType.JSVG.loaderContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull RenderingHints referenceHintSet() {
        return new RenderingHints(Map.of(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON,
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE,
                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON,
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
                SVGRenderingHints.KEY_SOFT_CLIPPING, SOFT_CLIPPING_VALUE));
    }

    private static BufferedImage renderJsvg(@NotNull ImageSource imageSource,
            @Nullable Consumer<Graphics2D> graphicsMutator, LoaderContext loaderContext) throws IOException {
        SVGDocument document;

        URL url = imageSource.url();
        if (url != null) {
            document = Objects.requireNonNull(new SVGLoader().load(url, loaderContext));
        } else {
            document = Objects.requireNonNull(new SVGLoader().load(imageSource.openStream(), null, loaderContext));
        }

        FloatSize size = document.size();
        BufferedImage image = new ReferenceImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        if (graphicsMutator != null) graphicsMutator.accept(g);
        document.render((Component) null, g, new ViewBox(size));
        g.dispose();
        return image;
    }

    private static BufferedImage renderBatik(@NotNull InputStream inputStream) throws IOException {
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
