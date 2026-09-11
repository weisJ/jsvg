/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static com.github.weisj.jsvg.ImageComparison.renderJsvg;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.paint.impl.AwtSVGPaint;

class PaintsTest {
    @TempDir
    Path directory;

    @Test
    void testCurrentColor() {
        assertEquals(SUCCESS, compareImages("paint/currentColor.svg"));
    }

    @Test
    void testContextColors() {
        BufferedImage img = renderJsvg("paint/context.svg");
        assertEquals(new Color(0, 255, 0), new Color(img.getRGB(25, 25)));
        assertEquals(new Color(255, 0, 0), new Color(img.getRGB(125, 25)));
        assertEquals(new Color(0, 0, 255), new Color(img.getRGB(225, 25)));
        assertEquals(new Color(255, 255, 0), new Color(img.getRGB(325, 25)));
    }

    @Test
    void testStringRepresentation() {
        assertEquals("AwtSVGPaint{paint=Color{r=0,g=0,b=0,a=255}}", new AwtSVGPaint(Color.BLACK).toString());
    }

    @Test
    void urlResolution() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("paint/urlResolution_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("paint/urlResolution.svg"), RenderType.JSVG), 0, 0)));
    }

    @Test
    void quotedAndEscapedPaintUrlsAndFallbacks() {
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("paint/urlsAndFallbacks_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("paint/urlsAndFallbacks.svg"), RenderType.JSVG), 0, 0)));
    }

    @TestFactory
    Stream<DynamicTest> quotedAndEscapedExternalPaintUrls() throws Exception {
        Files.writeString(directory.resolve("paint).svg"), """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <linearGradient id="blue"><stop stop-color="blue"/></linearGradient>
                </svg>
                """);
        var reference = new MemoryImageSource("blue", """
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
                  <rect width="16" height="16" fill="blue"/>
                </svg>
                """);
        return Stream.of("url('paint).svg#blue') red", "url(paint\\).svg#blue) red")
                .map(paint -> DynamicTest.dynamicTest(paint, () -> {
                    var source = new MemoryImageSource("external-paint", """
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
                              <rect width="16" height="16" fill="%s"/>
                            </svg>
                            """.formatted(paint), directory.resolve("document.svg").toUri().toURL());
                    assertEquals(SUCCESS, compareImages(new CompareInfo(expected(reference, RenderType.Batik),
                            actual(source, RenderType.JSVG), 0, 0)));
                }));
    }
}
