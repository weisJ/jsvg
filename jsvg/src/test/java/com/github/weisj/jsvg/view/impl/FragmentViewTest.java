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
package com.github.weisj.jsvg.view.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio.Align;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio.MeetOrSlice;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.renderer.RenderConfig;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

class FragmentViewTest {

    @Test
    void parsesUrlEscapedSvgViewAttributesAndTimeSegment() {
        ResolvedView view = resolve(
                "svgView(viewBox(10,0,10,10)%3BpreserveAspectRatio(xMaxYMax%20slice)"
                        + "%3Btransform(translate(2,3))%3BzoomAndPan(disable))&t=5",
                new FloatSize(20, 10));

        assertEquals(new ViewBox(10, 0, 10, 10), view.viewBox(null));
        PreserveAspectRatio preserveAspectRatio = view.preserveAspectRatio(PreserveAspectRatio.none());
        assertEquals(Align.xMaxYMax, preserveAspectRatio.align);
        assertEquals(MeetOrSlice.Slice, preserveAspectRatio.meetOrSlice);
        assertNotNull(view.transformOverride());
    }

    @Test
    void resolvesPixelAndPercentSpatialFragments() {
        ResolvedView clipped = resolve("xywh=pixel:15,2,10,10", new FloatSize(20, 10));
        assertEquals(new ViewBox(15, 2, 5, 8), clipped.viewBox(null));

        ResolvedView percent = resolve("%74=5&xywh=percent:50,0,50,100", new FloatSize(21, 11));
        assertEquals(new ViewBox(10, 0, 11, 11), percent.viewBox(null));
    }

    @Test
    void ignoresInvalidSpatialFragments() {
        ResolvedView invalidSyntax = resolve("xywh=percent:90,0,20,100", new FloatSize(20, 10));
        assertNull(invalidSyntax.viewBox(null));

        ResolvedView outsideMedia = resolve("xywh=20,0,10,10", new FloatSize(20, 10));
        assertNull(outsideMedia.viewBox(null));
    }

    @Test
    void unspecifiedSvgViewParametersKeepRootValues() {
        ResolvedView view = resolve("svgView(zoomAndPan(magnify))", new FloatSize(20, 10));

        ViewBox viewBoxFallback = new ViewBox(20, 10);
        PreserveAspectRatio preserveAspectRatioFallback = PreserveAspectRatio.none();
        assertSame(viewBoxFallback, view.viewBox(viewBoxFallback));
        assertSame(preserveAspectRatioFallback, view.preserveAspectRatio(preserveAspectRatioFallback));
        assertNull(view.transformOverride());
    }

    @Test
    void explicitNoneTransformOverridesRootTransform() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="10" viewBox="0 0 20 10"
                     preserveAspectRatio="none" transform="translate(10,0)">
                  <rect width="10" height="10" fill="red"/>
                  <rect x="10" width="10" height="10" fill="blue"/>
                </svg>
                """;
        SVGDocument document = Objects.requireNonNull(new SVGLoader().load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null, LoaderContext.createDefault()));
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        document.render(graphics, RenderConfig.builder()
                .viewport(new ViewBox(20, 10))
                .view(FragmentView.parse("svgView(transform(none))"))
                .build());
        graphics.dispose();

        assertEquals(Color.RED, new Color(image.getRGB(5, 5), true));
        assertEquals(Color.BLUE, new Color(image.getRGB(15, 5), true));

        BufferedImage translated = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        graphics = translated.createGraphics();
        document.render(graphics, RenderConfig.builder()
                .viewport(new ViewBox(20, 10))
                .view(FragmentView.parse("svgView(transform(translate(-10,0)))"))
                .build());
        graphics.dispose();

        assertEquals(Color.BLUE, new Color(translated.getRGB(5, 5), true));
        assertEquals(new Color(0, true), new Color(translated.getRGB(15, 5), true));
    }

    private static ResolvedView resolve(String fragment, FloatSize documentSize) {
        return FragmentView.parse(fragment).resolve(Collections.emptyMap(), documentSize);
    }
}
