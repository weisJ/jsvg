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
        return bounds(content, context());
    }

    private static ElementBounds bounds(String content, RenderContext context) {
        AtomicReference<DomDocument> dom = new AtomicReference<>();
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'>" + content + "</svg>";
        assertNotNull(new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null,
                LoaderContext.builder().preProcessor(root -> dom.set(root.document())).build()));
        SVGNode node = dom.get().getElementById(SVGNode.class, "target");
        assertNotNull(node);
        return new ElementBounds(node, NodeRenderer.setupRenderContext(node, context));
    }

    private static void assertBox(Rectangle2D actual, double x, double y, double width, double height) {
        assertAll(() -> assertEquals(x, actual.getX(), 1e-6),
                () -> assertEquals(y, actual.getY(), 1e-6),
                () -> assertEquals(width, actual.getWidth(), 1e-6),
                () -> assertEquals(height, actual.getHeight(), 1e-6));
    }

    @Test
    void strokeBoxUsesTheUndashedOutline() {
        ElementBounds bounds = bounds("""
                <path id="target" d="M10 20H40" stroke="black" stroke-width="10"
                    stroke-linecap="round" stroke-dasharray="1 100" stroke-dashoffset="50" pathLength="1"/>
                """);
        assertBox(bounds.strokeBox(), 5, 15, 40, 10);
        assertBox(bounds.boundingBox(), 10, 20, 30, 0);
    }

    @Test
    void strokeBoxesOfClosedBasicShapes() {
        for (String join : new String[] {"miter", "round", "bevel"}) {
            String stroke = " id='target' stroke='black' stroke-width='10' stroke-linejoin='" + join
                    + "' stroke-miterlimit='1' stroke-linecap='square' stroke-dasharray='1 100'/>";
            assertBox(bounds("<rect x='10' y='20' width='30' height='40'" + stroke).strokeBox(), 5, 15, 40, 50);
            assertBox(bounds("<rect x='10' y='20' width='30' height='40' rx='8' ry='4'" + stroke).strokeBox(),
                    5, 15, 40, 50);
            assertBox(bounds("<circle cx='25' cy='40' r='15'" + stroke).strokeBox(), 5, 20, 40, 40);
            assertBox(bounds("<ellipse cx='25' cy='40' rx='15' ry='20'" + stroke).strokeBox(), 5, 15, 40, 50);
        }
    }

    @Test
    void nonScalingStrokeBoxAccountsForTheCoordinateSystem() {
        RenderContext context = context();
        context.userSpaceTransform().scale(2, 4);
        assertBox(bounds("""
                <rect id="target" x="10" y="20" width="30" height="40"
                    stroke="black" stroke-width="10" vector-effect="non-scaling-stroke"/>
                """, context).strokeBox(), 7.5, 18.75, 35, 42.5);
    }

    @Test
    void strokeBoxRespectsButtCapsAndMiterJoins() {
        assertBox(bounds("""
                <path id="target" d="M10 20H40" stroke="black" stroke-width="10" stroke-linecap="butt"/>
                """).strokeBox(), 10, 15, 30, 10);
        // The two segments have 3:4 slopes. The outer miter extends 5 / 0.6 above the apex.
        assertBox(bounds("""
                <path id="target" d="M20 40L35 20L50 40" fill="none"
                    stroke="black" stroke-width="10" stroke-linejoin="miter" stroke-miterlimit="4"/>
                """).strokeBox(), 16, 20 - 5 / 0.6, 38, 23 + 5 / 0.6);
    }

    @Test
    void strokeBoxIgnoresOpacityAndVisibility() {
        assertBox(bounds("""
                <rect id="target" x="10" y="20" width="30" height="40" stroke="black"
                    stroke-width="10" stroke-opacity="0" opacity="0" visibility="hidden"/>
                """).strokeBox(), 5, 15, 40, 50);
        assertBox(bounds("""
                <rect id="target" x="10" y="20" width="30" height="40"
                    stroke="rgba(0,0,0,0)" stroke-width="10"/>
                """).strokeBox(), 5, 15, 40, 50);
    }

    @Test
    void strokeBoxExcludesAbsentAndZeroWidthStrokes() {
        assertBox(bounds("""
                <rect id="target" x="10" y="20" width="30" height="40" stroke="none" stroke-width="20"/>
                """).strokeBox(), 10, 20, 30, 40);
        assertBox(bounds("""
                <rect id="target" x="10" y="20" width="30" height="40" stroke="black" stroke-width="0"/>
                """).strokeBox(), 10, 20, 30, 40);
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
