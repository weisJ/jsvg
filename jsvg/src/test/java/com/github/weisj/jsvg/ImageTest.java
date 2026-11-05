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
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static com.github.weisj.jsvg.ImageComparison.renderJsvg;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ImageComparison.ReferenceTestResult;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;

class ImageTest {

    @Test
    void externalImageTest() {
        // Can't compare with batik as it fails with cross origin requests
        assertDoesNotThrow(() -> renderJsvg("image/imageExternal.svg"));
    }

    @Test
    void externalImageBlockedTest() {
        // Can't compare with batik as it fails with cross origin requests
        assertEquals(ReferenceTestResult.SUCCESS, compareImages(
                new CompareInfo(
                        expected(new PathImageSource("image/imageExternal.svg"), new RenderType.JSVGType(
                                LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_EXTERNAL).build())),
                        actual(new PathImageSource("image/imageExternalNoLoad.svg"), RenderType.JSVG))));
    }

    @Test
    void embeddedImageTest() {
        // Batik doesn't support data uris
        assertDoesNotThrow(() -> renderJsvg("image/imageBase64.svg"));
    }

    @Test
    void embeddedImageIsDecoded() throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.setColor(Color.RED);
        imageGraphics.fillRect(0, 0, 8, 8);
        imageGraphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'"
                + " width='8' height='8'><image width='8' height='8' xlink:href='data:image/png;base64,"
                + Base64.getEncoder().encodeToString(png.toByteArray()) + "'/></svg>";
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                null, LoaderContext.createDefault());
        BufferedImage rendered = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rendered.createGraphics();
        document.render(null, g);
        g.dispose();
        assertEquals(0xffff0000, rendered.getRGB(4, 4));
    }
}
