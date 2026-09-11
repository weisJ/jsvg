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

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.parser.DomDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.view.FloatSize;

class ElementBoundsTest {
    private static RenderContext context() {
        return RenderContextAccessor.instance().createInitial(null, NullPlatformSupport.INSTANCE,
                MeasureContext.createInitial(new FloatSize(200, 200), 16, 8, AnimationState.NO_ANIMATION));
    }

    private static ElementBounds bounds(String content) {
        AtomicReference<DomDocument> dom = new AtomicReference<>();
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'>" + content + "</svg>";
        assertNotNull(new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null,
                LoaderContext.builder().preProcessor(root -> dom.set(root.document())).build()));
        SVGNode node = dom.get().getElementById(SVGNode.class, "target");
        assertNotNull(node);
        return new ElementBounds(node, NodeRenderer.setupRenderContext(node, context()));
    }

    private static void assertBox(Rectangle2D actual, double x, double y, double width, double height) {
        assertAll(() -> assertEquals(x, actual.getX(), 1e-6),
                () -> assertEquals(y, actual.getY(), 1e-6),
                () -> assertEquals(width, actual.getWidth(), 1e-6),
                () -> assertEquals(height, actual.getHeight(), 1e-6));
    }

    @Test
    void filterOutputDoesNotBecomeItsOwnSource() {
        ElementBounds bounds = bounds("""
                <defs>
                    <filter id="offset" filterUnits="userSpaceOnUse" x="0" y="0" width="200" height="200">
                        <feOffset dx="50"/>
                    </filter>
                </defs>
                <g id="target" filter="url(#offset)">
                    <rect x="10" y="20" width="30" height="40" stroke-width="0"/>
                </g>
                """);
        assertBox(bounds.sourceBox(), 10, 20, 30, 40);
        assertTrue(bounds.outputBox().getMaxX() >= 90);
        assertBox(bounds.sourceBox(), 10, 20, 30, 40);
        assertBox(bounds.boundingBox(), 10, 20, 30, 40);
    }

    @Test
    void containerSourceIncludesChildOutputButNotItsOwnFilter() {
        ElementBounds bounds = bounds("""
                <defs>
                    <filter id="flood" filterUnits="userSpaceOnUse" x="60" y="20" width="30" height="40">
                        <feFlood flood-color="red"/>
                    </filter>
                    <filter id="offset" filterUnits="userSpaceOnUse" x="0" y="0" width="200" height="200">
                        <feOffset dx="100"/>
                    </filter>
                </defs>
                <g id="target" filter="url(#offset)">
                    <rect x="10" y="20" width="10" height="10" stroke-width="0"/>
                    <g filter="url(#flood)"/>
                </g>
                """);
        assertTrue(bounds.outputBox().getMaxX() >= 190);
        assertTrue(bounds.sourceBox().getMaxX() >= 90);
        assertTrue(bounds.sourceBox().getMaxX() < 100);
        assertBox(bounds.boundingBox(), 10, 20, 10, 10);
    }

    @Test
    void transformedQueriesPreserveTheRequestedBox() {
        ElementBounds bounds = bounds("""
                <defs>
                    <filter id="offset" filterUnits="userSpaceOnUse" x="0" y="0" width="200" height="200">
                        <feOffset dx="50"/>
                    </filter>
                </defs>
                <rect id="target" x="10" y="20" width="30" height="40" stroke-width="0"
                    transform="translate(5 7)" filter="url(#offset)"/>
                """);
        assertBox(bounds.transformedBounds(HasShape.Box.SourceBox), 15, 27, 30, 40);
        assertTrue(bounds.transformedBounds(HasShape.Box.OutputBox).getMaxX() >= 95);
        assertBox(bounds.boundingBox(), 10, 20, 30, 40);
    }
}
