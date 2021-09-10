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
import java.io.*;
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

class ReferenceTest {

    @Test
    void testLine() throws Exception {
        compareImages("line1", wrapTag(100, 100, "<line x1='05' y1='05' x2='70' y2='51' stroke='blue'/>"));
        compareImages("line2", wrapTag(100, 200, "<line x1='05' y1='05' x2='70' y2='51' stroke='green'/>"));
        compareImages("line3", wrapTag(200, 200, "<line x1='05' y1='05' x2='70' y2='51' stroke='yellow'/>"));
        compareImages("line4", wrapTag(100, 100, "<line x1='10' y1='15' x2='80' y2='51' stroke='red'/>"));
        compareImages("line5", wrapTag(100, 200, "<line x1='10' y1='15' x2='80' y2='51' stroke='#000000'/>"));
        compareImages("line6", wrapTag(200, 200, "<line x1='10' y1='15' x2='80' y2='51' stroke='black'/>"));
    }

    @Test
    void testRectangle() throws Exception {
        compareImages("rect1", wrapTag(100, 100, "<rect x='5' y='5' width='70' height='51' fill='blue'/>"));
        compareImages("rect2", wrapTag(100, 200, "<rect x='5' y='5' width='70' height='51' fill='green'/>"));
        compareImages("rect3", wrapTag(200, 200, "<rect x='5' y='5' width='70' height='51' fill='yellow'/>"));
        compareImages("rect4", wrapTag(100, 100, "<rect x='10' y='15' width='80' height='51' fill='red'/>"));
        compareImages("rect5", wrapTag(100, 200, "<rect x='10' y='15' width='80' height='51' fill='#000000'/>"));
        compareImages("rect6", wrapTag(200, 200, "<rect x='10' y='15' width='80' height='51' fill='black'/>"));
    }

    @Test
    void testRoundRectangle() throws Exception {
        compareImages("roundRect1",
                wrapTag(100, 100, "<rect x='5' y='5' width='70' height='51' rx='5' ry='0' fill='blue'/>"));
        compareImages("roundRect2",
                wrapTag(100, 200, "<rect x='5' y='5' width='70' height='51' rx='10' ry='5' fill='green'/>"));
        compareImages("roundRect3",
                wrapTag(200, 200, "<rect x='5' y='5' width='70' height='51' rx='20' ry='10' fill='yellow'/>"));
        compareImages("roundRect4",
                wrapTag(100, 100, "<rect x='10' y='15' width='80' height='51' rx='30' ry='0' fill='red'/>"));
        compareImages("roundRect5",
                wrapTag(100, 200, "<rect x='10' y='15' width='80' height='51' rx='15' ry='5' fill='#000000'/>"));
        compareImages("roundRect6",
                wrapTag(200, 200, "<rect x='10' y='15' width='80' height='51' rx='5' ry='10' fill='black'/>"));
    }

    @Test
    void testCircle() throws Exception {
        compareImages("circle1", wrapTag(100, 100, "<circle cx='40' cy='40' r='35' fill='blue'/>"));
        compareImages("circle2", wrapTag(100, 200, "<circle cx='40' cy='40' r='35' fill='green'/>"));
        compareImages("circle3", wrapTag(200, 200, "<circle cx='40' cy='40' r='35' fill='yellow'/>"));
        compareImages("circle4", wrapTag(100, 100, "<circle cx='50' cy='55' r='40' fill='red'/>"));
        compareImages("circle5", wrapTag(100, 200, "<circle cx='50' cy='55' r='40' fill='#000000'/>"));
        compareImages("circle6", wrapTag(200, 200, "<circle cx='50' cy='55' r='40' fill='black'/>"));
    }

    @Test
    void testEllipse() throws Exception {
        compareImages("ellipse1", wrapTag(100, 100, "<ellipse cx='40' cy='40' rx='35' ry='5' fill='blue'/>"));
        compareImages("ellipse2", wrapTag(100, 200, "<ellipse cx='40' cy='40' rx='35' ry='50' fill='green'/>"));
        compareImages("ellipse3", wrapTag(200, 200, "<ellipse cx='40' cy='40' rx='35' ry='80' fill='yellow'/>"));
        compareImages("ellipse4", wrapTag(100, 100, "<ellipse cx='50' cy='55' rx='40' ry='5' fill='red'/>"));
        compareImages("ellipse5", wrapTag(100, 200, "<ellipse cx='50' cy='55' rx='40' ry='45' fill='#000000'/>"));
        compareImages("ellipse6", wrapTag(200, 200, "<ellipse cx='50' cy='55' rx='40' ry='30' fill='black'/>"));
    }

    @Test
    void testIcons() throws Exception {
        String[] iconNames = {"desktop.svg", "drive.svg", "folder.svg", "general.svg", "homeFolder.svg", "image.svg",
                "missingImage.svg", "newFolder.svg", "pendingImage.svg", "text.svg", "unknown.svg", "upFolder.svg"};
        for (String iconName : iconNames) {
            compareImages(iconName, Objects.requireNonNull(getClass().getResource("icons/" + iconName), iconName));
        }
    }

    private String wrapTag(int width, int height, @NotNull String tag) {
        return "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' " +
                "width='" + width + "' height='" + height + "' viewBox='0 0 " + width + " " + height + "'>" + tag
                + "</svg>";
    }

    private void compareImages(@NotNull String name, @NotNull URL url) throws Exception {
        BufferedImage expected = renderReference(url.openStream());
        BufferedImage actual = render(url.openStream());
        compareImageRasterization(expected, actual, name);
    }

    private void compareImages(@NotNull String name, @NotNull String svgContent) throws Exception {
        BufferedImage expected = renderReference(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
        BufferedImage actual = render(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
        compareImageRasterization(expected, actual, name);
    }

    private void compareImageRasterization(@NotNull BufferedImage expected, @NotNull BufferedImage actual,
            @NotNull String name) {
        ImageComparison comp = new ImageComparison(expected, actual);
        comp.setAllowingPercentOfDifferentPixels(1f);
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

    private static BufferedImage render(@NotNull InputStream inputStream) throws Exception {
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
