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
package com.github.weisj.jsvg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.renderer.RenderConfig;
import com.github.weisj.jsvg.view.PreserveAspectRatio;
import com.github.weisj.jsvg.view.View;
import com.github.weisj.jsvg.view.ViewBox;

class ViewTest {
    private static final String DOCUMENT = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                 viewBox="0 0 100 100" preserveAspectRatio="none">
              <view id="right" viewBox="50 0 50 100" preserveAspectRatio="none"/>
              <view id="rightInherited" viewBox="50 0 50 100"/>
              <rect width="50" height="100" fill="red"/>
              <rect id="blue" x="50" width="50" height="100" fill="blue"/>
            </svg>
            """;

    @Test
    void rendersNamedView() {
        BufferedImage image = render(View.named("right"));

        assertEquals(Color.BLUE, colorAt(image, 25, 50));
        assertEquals(Color.BLUE, colorAt(image, 75, 50));
    }

    @Test
    void rendersExplicitView() {
        BufferedImage image = render(View.of(
                new ViewBox(50, 0, 50, 100), PreserveAspectRatio.none()));

        assertEquals(Color.BLUE, colorAt(image, 25, 50));
        assertEquals(Color.BLUE, colorAt(image, 75, 50));
    }

    @Test
    void rendersTransformedViewAndDefensivelyCopiesTransform() {
        AffineTransform transform = AffineTransform.getTranslateInstance(-50, 0);
        View view = View.of(transform);
        transform.setToIdentity();

        BufferedImage image = render(view);

        assertEquals(Color.BLUE, colorAt(image, 25, 50));
        assertEquals(new Color(0, true), colorAt(image, 75, 50));
    }

    @Test
    void rendersExplicitTransformedView() {
        BufferedImage image = render(View.of(
                new ViewBox(0, 0, 100, 100),
                PreserveAspectRatio.none(),
                AffineTransform.getTranslateInstance(-50, 0)));

        assertEquals(Color.BLUE, colorAt(image, 25, 50));
        assertEquals(new Color(0, true), colorAt(image, 75, 50));
    }

    @Test
    void namedViewInheritsUnspecifiedAspectRatioFromRoot() {
        BufferedImage image = render(View.named("rightInherited"));

        assertEquals(Color.BLUE, colorAt(image, 5, 50));
        assertEquals(Color.BLUE, colorAt(image, 95, 50));
    }

    @Test
    void nonViewFragmentFallsBackToRootView() {
        BufferedImage image = render(View.named("blue"));

        assertEquals(Color.RED, colorAt(image, 25, 50));
        assertEquals(Color.BLUE, colorAt(image, 75, 50));
    }

    @Test
    void missingViewFallsBackToRootView() {
        BufferedImage image = render(View.named("missing"));

        assertEquals(Color.RED, colorAt(image, 25, 50));
        assertEquals(Color.BLUE, colorAt(image, 75, 50));
    }

    @Test
    void exposesDeclaredViewNamesInDocumentOrder() {
        SVGDocument document = loadDocument();

        assertIterableEquals(List.of("right", "rightInherited"), document.viewNames());
        assertThrows(UnsupportedOperationException.class, () -> document.viewNames().remove("right"));
    }

    private static BufferedImage render(View view) {
        SVGDocument document = loadDocument();
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        document.render(graphics, RenderConfig.builder()
                .viewport(new ViewBox(100, 100))
                .view(view)
                .build());
        graphics.dispose();
        return image;
    }

    private static SVGDocument loadDocument() {
        return Objects.requireNonNull(new SVGLoader().load(
                new ByteArrayInputStream(DOCUMENT.getBytes(StandardCharsets.UTF_8)), null,
                com.github.weisj.jsvg.parser.LoaderContext.createDefault()));
    }

    private static Color colorAt(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y), true);
    }
}
