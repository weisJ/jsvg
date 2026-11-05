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
package com.github.weisj.jsvg.parser.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;

/** Inline styles cascade the same whether or not the document has a stylesheet. */
class InlineCascadeTest {
    private static int pixel(String rectAttributes, String styleSheet) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='4' height='4'>" + styleSheet
                + "<rect width='4' height='4' " + rectAttributes + "/></svg>";
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                null, LoaderContext.createDefault());
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        document.render(null, g);
        g.dispose();
        return image.getRGB(2, 2);
    }

    @Test
    void sameResultWithUnrelatedStyleSheet() {
        for (String attributes : new String[] {
                "style='fill:#ff0000;fill:#0000ff'",
                "style='fill:#ff0000;fill:#0000ff;fill:#00ff00'",
                "fill='#00ff00' style='fill:#ff0000'",
                "fill='#00ff00' style='stroke:#ff0000'",
                "style='fill:#ff0000 !important;fill:#0000ff'",
                "style='fill:#ff0000;fill:#0000ff !important'",
                "style='FILL:#ff0000;fill:#0000ff'",
                "style=''"}) {
            assertEquals(pixel(attributes, "<style>unrelated{fill:#123456}</style>"), pixel(attributes, ""),
                    attributes);
        }
    }
}
