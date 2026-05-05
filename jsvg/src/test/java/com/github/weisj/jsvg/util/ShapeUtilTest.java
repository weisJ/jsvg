/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
package com.github.weisj.jsvg.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.geom.*;

import org.junit.jupiter.api.Test;

class ShapeUtilTest {

    // -------------------------------------------------------------------------
    // isInvalidArea
    // -------------------------------------------------------------------------

    @Test
    void testIsInvalidAreaEmpty() {
        assertTrue(ShapeUtil.isInvalidArea(new Rectangle2D.Double(0, 0, 0, 5)));
        assertTrue(ShapeUtil.isInvalidArea(new Rectangle2D.Double(0, 0, 5, 0)));
        assertTrue(ShapeUtil.isInvalidArea(new Rectangle2D.Double(0, 0, 0, 0)));
    }

    @Test
    void testIsInvalidAreaNaN() {
        assertTrue(ShapeUtil.isInvalidArea(new Rectangle2D.Double(0, 0, Double.NaN, 5)));
        assertTrue(ShapeUtil.isInvalidArea(new Rectangle2D.Double(0, 0, 5, Double.NaN)));
    }

    @Test
    void testIsInvalidAreaValid() {
        assertFalse(ShapeUtil.isInvalidArea(new Rectangle2D.Double(0, 0, 1, 1)));
        assertFalse(ShapeUtil.isInvalidArea(new Rectangle2D.Double(-10, -10, 20, 20)));
    }

    // -------------------------------------------------------------------------
    // intersect – two Rectangles (java.awt.Rectangle)
    // -------------------------------------------------------------------------

    @Test
    void testIntersectTwoRectangles() {
        Rectangle r1 = new Rectangle(0, 0, 10, 10);
        Rectangle r2 = new Rectangle(5, 5, 10, 10);
        Shape result = ShapeUtil.intersect(r1, r2, true, true);
        assertTrue(result instanceof Rectangle);
        Rectangle expected = new Rectangle(5, 5, 5, 5);
        assertEquals(expected, result);
    }

    @Test
    void testIntersectTwoRectanglesNoOverlap() {
        Rectangle r1 = new Rectangle(0, 0, 5, 5);
        Rectangle r2 = new Rectangle(10, 10, 5, 5);
        Shape result = ShapeUtil.intersect(r1, r2, true, true);
        assertTrue(result instanceof Rectangle);
        assertTrue(((Rectangle) result).isEmpty());
    }

    // -------------------------------------------------------------------------
    // intersect – one Rectangle2D and a general Shape
    // -------------------------------------------------------------------------

    @Test
    void testIntersectRectContainsShape() {
        // When rect fully contains the shape, the shape itself is returned (possibly cloned)
        Rectangle2D bigRect = new Rectangle2D.Double(0, 0, 100, 100);
        Ellipse2D smallEllipse = new Ellipse2D.Double(10, 10, 20, 20);
        Shape result = ShapeUtil.intersect(bigRect, smallEllipse, true, true);
        // Result should approximate the ellipse bounds
        Rectangle2D resultBounds = result.getBounds2D();
        Rectangle2D ellipseBounds = smallEllipse.getBounds2D();
        assertEquals(ellipseBounds.getX(), resultBounds.getX(), 0.1);
        assertEquals(ellipseBounds.getY(), resultBounds.getY(), 0.1);
        assertEquals(ellipseBounds.getWidth(), resultBounds.getWidth(), 0.1);
        assertEquals(ellipseBounds.getHeight(), resultBounds.getHeight(), 0.1);
    }

    @Test
    void testIntersectTwoRectangle2D() {
        Rectangle2D r1 = new Rectangle2D.Double(0, 0, 10, 10);
        Rectangle2D r2 = new Rectangle2D.Double(4, 4, 10, 10);
        Shape result = ShapeUtil.intersect(r1, r2, true, true);
        assertTrue(result instanceof Rectangle2D);
        Rectangle2D rect = (Rectangle2D) result;
        assertEquals(4.0, rect.getX(), 0.001);
        assertEquals(4.0, rect.getY(), 0.001);
        assertEquals(6.0, rect.getWidth(), 0.001);
        assertEquals(6.0, rect.getHeight(), 0.001);
    }

    @Test
    void testIntersectTwoRectangle2DNoOverlap() {
        Rectangle2D r1 = new Rectangle2D.Double(0, 0, 5, 5);
        Rectangle2D r2 = new Rectangle2D.Double(10, 10, 5, 5);
        Shape result = ShapeUtil.intersect(r1, r2, true, true);
        assertTrue(result instanceof Rectangle2D);
        assertTrue(((Rectangle2D) result).isEmpty());
    }

    // -------------------------------------------------------------------------
    // transformShape – identity and translation
    // -------------------------------------------------------------------------

    @Test
    void testTransformShapeIdentity() {
        Rectangle2D rect = new Rectangle2D.Double(5, 10, 20, 30);
        AffineTransform identity = new AffineTransform();
        Shape result = ShapeUtil.transformShape(rect, identity);
        Rectangle2D bounds = result.getBounds2D();
        assertEquals(5.0, bounds.getX(), 0.001);
        assertEquals(10.0, bounds.getY(), 0.001);
        assertEquals(20.0, bounds.getWidth(), 0.001);
        assertEquals(30.0, bounds.getHeight(), 0.001);
    }

    @Test
    void testTransformShapeTranslation() {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        AffineTransform tx = AffineTransform.getTranslateInstance(5, 7);
        Shape result = ShapeUtil.transformShape(rect, tx);
        assertTrue(result instanceof Rectangle2D);
        Rectangle2D bounds = result.getBounds2D();
        assertEquals(5.0, bounds.getX(), 0.001);
        assertEquals(7.0, bounds.getY(), 0.001);
        assertEquals(10.0, bounds.getWidth(), 0.001);
        assertEquals(10.0, bounds.getHeight(), 0.001);
    }

    @Test
    void testTransformShapeScale() {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 4, 6);
        AffineTransform scale = AffineTransform.getScaleInstance(2, 3);
        Shape result = ShapeUtil.transformShape(rect, scale);
        Rectangle2D bounds = result.getBounds2D();
        assertEquals(0.0, bounds.getX(), 0.001);
        assertEquals(0.0, bounds.getY(), 0.001);
        assertEquals(8.0, bounds.getWidth(), 0.001);
        assertEquals(18.0, bounds.getHeight(), 0.001);
    }

    @Test
    void testTransformShapeRotation() {
        Rectangle2D rect = new Rectangle2D.Double(1, 1, 4, 2);
        AffineTransform rot = AffineTransform.getRotateInstance(Math.PI / 2);
        Shape result = ShapeUtil.transformShape(rect, rot);
        assertNotNull(result);
        // After 90-degree rotation the bounding box width/height should be swapped
        Rectangle2D bounds = result.getBounds2D();
        assertEquals(2.0, bounds.getWidth(), 0.5);
        assertEquals(4.0, bounds.getHeight(), 0.5);
    }
}
