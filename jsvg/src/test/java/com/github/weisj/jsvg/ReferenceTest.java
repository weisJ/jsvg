/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
import java.util.Objects;

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

final class ReferenceTest {

    @Test
    void testIcons() {
        String[] iconNames = {"desktop.svg", "drive.svg", "folder.svg", "general.svg", "homeFolder.svg", "image.svg",
                "missingImage.svg", "newFolder.svg", "pendingImage.svg", "text.svg", "unknown.svg", "upFolder.svg"};
        for (String iconName : iconNames) {
            compareImages("icons/" + iconName);
        }
    }

    static String wrapTag(int width, int height, @NotNull String tag) {
        return "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' " +
                "width='" + width + "' height='" + height + "' viewBox='0 0 " + width + " " + height + "'>" + tag
                + "</svg>";
    }

    static void compareImages(@NotNull String fileName) {
        compareImages(fileName, Objects.requireNonNull(ReferenceTest.class.getResource(fileName), fileName));
    }


    static void compareImages(@NotNull String name, @NotNull URL url) {
        try {
            BufferedImage expected = renderReference(url.openStream());
            BufferedImage actual = render(url.openStream());
            compareImageRasterization(expected, actual, name);
        } catch (Exception e) {
            Assertions.fail(name, e);
        }
    }

    static void compareImages(@NotNull String name, @NotNull String svgContent) {
        try {
            BufferedImage expected =
                    renderReference(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
            BufferedImage actual = render(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
            compareImageRasterization(expected, actual, name);
        } catch (Exception e) {
            Assertions.fail(name, e);
        }
    }

    private static void compareImageRasterization(@NotNull BufferedImage expected, @NotNull BufferedImage actual,
            @NotNull String name) {
        ImageComparison comp = new ImageComparison(expected, actual);
        comp.setAllowingPercentOfDifferentPixels(0.5f);
        ImageComparisonResult comparison = comp.compareImages();
        Assertions.assertEquals(ImageComparisonState.MATCH, comparison.getImageComparisonState(), () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Image: ").append(name).append('\n');
            sb.append("Expected size: ").append(expected.getWidth()).append('x').append(expected.getHeight())
                    .append('\n');
            sb.append("Actual size: ").append(actual.getWidth()).append('x').append(actual.getHeight())
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
            }
            return sb.toString();
        });
    }

    private static BufferedImage render(@NotNull InputStream inputStream) {
        SVGDocument document = Objects.requireNonNull(new SVGLoader().load(inputStream));
        FloatSize size = document.size();
        BufferedImage image = new BufferedImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }

                @Override
                public void writeImage(BufferedImage image, TranscoderOutput out) {
                    imagePointer[0] = image;
                }
            };
            t.setTranscodingHints(transcoderHints);
            t.transcode(input, null);
        } catch (TranscoderException ex) {
            ex.printStackTrace();
            throw new IOException("Couldn't convert image");
        }
        return imagePointer[0];
    }
}
