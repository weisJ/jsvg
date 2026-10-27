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
package com.github.weisj.jsvg.parser.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;

class ExternalStyleSheetTest {

    private static final String SVG = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
            + "<style>.a { fill: blue; }</style>"
            + "<rect class='a' width='10' height='10'/>"
            + "</svg>";

    @Test
    void externalStyleSheetOverridesRulesOfEqualSpecificity() {
        assertEquals(Color.RED, renderedColor(SVG, ".a { fill: red; }"));
    }

    @Test
    void lowerSpecificityDoesNotOverrideDocumentRules() {
        assertEquals(Color.BLUE, renderedColor(SVG, "rect { fill: red; }"));
        assertEquals(Color.RED, renderedColor(SVG, "rect { fill: red !important; }"));
    }

    @Test
    void worksForDocumentsWithoutStyleElement() {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect class='x' width='10' height='10'/></svg>";
        assertEquals(Color.GREEN, renderedColor(svg, ".x { fill: #0f0; }"));
    }

    @Test
    void inlineStyleWinsOverNonImportantRules() {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect class='x' style='fill: blue' width='10' height='10'/></svg>";
        assertEquals(Color.BLUE, renderedColor(svg, ".x { fill: red; }"));
    }

    @Test
    void noExternalStyleSheetKeepsDocumentStyles() {
        assertEquals(Color.BLUE, renderedColor(SVG, null));
    }

    private static @NotNull Color renderedColor(@NotNull String svg, @Nullable String css) {
        LoaderContext context = LoaderContext.builder().externalStyleSheet(css).build();
        SVGDocument doc = new SVGLoader().load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null, context);
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        doc.render(null, g);
        g.dispose();
        return new Color(img.getRGB(5, 5));
    }
}
