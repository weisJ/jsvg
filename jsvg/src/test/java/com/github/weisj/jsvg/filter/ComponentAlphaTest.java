/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.filter;

import static com.github.weisj.jsvg.ImageComparison.*;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.*;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

class ComponentAlphaTest {
    @Test
    void cachedAlphaTablePreservesTheRenderedResult() throws Exception {
        PathImageSource source = new PathImageSource("filter/componentAlpha/linearAlpha.svg");
        SVGDocument document = new SVGLoader().load(source.url());
        BufferedImage reference = expected(new PathImageSource("filter/componentAlpha/linearAlpha_ref.svg"),
                RenderType.JSVG).render(null);
        for (String frame : new String[] {"initial", "cached"}) {
            BufferedImage actual = new BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = actual.createGraphics();
            graphics.setRenderingHints(referenceHintSet());
            document.render(null, graphics);
            graphics.dispose();
            assertEquals(SUCCESS, compareImageRasterization(reference, actual, "alpha-" + frame, 0, 0));
        }
    }

    @TestFactory
    Stream<DynamicTest> references() {
        return Stream.of("linearAlpha", "sRGBAlpha", "discreteAlpha").flatMap(name -> Stream
                .of(RenderType.JSVG, RenderType.Batik).map(renderer -> DynamicTest.dynamicTest(name + renderer, () -> {
                    String prefix = "filter/componentAlpha/" + name;
                    assertEquals(SUCCESS, compareImages(new CompareInfo(
                            expected(new PathImageSource(prefix + "_ref.svg"), RenderType.JSVG),
                            actual(new PathImageSource(prefix + ".svg"), renderer), 0, 0)));
                })));
    }
}
