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
package com.github.weisj.jsvg.geometry.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

class GeometryUtilTest {

    @Test
    void pixelAlignedBoundsToleratesFloatingPointNoiseAtIntegerEdges() {
        AffineTransform transform = AffineTransform.getScaleInstance(1.0 / 49, 1);
        Rectangle2D bounds = new Rectangle2D.Double(49, 2, 490, 6);

        assertEquals(new Rectangle(1, 2, 10, 6), GeometryUtil.pixelAlignedBounds(transform, bounds));
    }

    @Test
    void pixelAlignedBoundsAcceptsAxisAlignedTransforms() {
        assertEquals(new Rectangle(1, 0, 15, 40), GeometryUtil.pixelAlignedBounds(
                AffineTransform.getScaleInstance(2, 2), new Rectangle2D.Double(0.5, 0, 7.5, 20)));
        assertEquals(new Rectangle(48, 16, 24, 16), GeometryUtil.pixelAlignedBounds(
                new AffineTransform(-1, 0, 0, 1, 96, 0),
                new Rectangle2D.Double(24, 16, 24, 16)));
        assertEquals(new Rectangle(32, 24, 16, 24), GeometryUtil.pixelAlignedBounds(
                new AffineTransform(0, 1, -1, 0, 64, 0),
                new Rectangle2D.Double(24, 16, 24, 16)));
    }

    @Test
    void pixelAlignedBoundsRejectsSubpixelAndNonAxisAlignedBounds() {
        assertNull(GeometryUtil.pixelAlignedBounds(
                new AffineTransform(), new Rectangle2D.Double(1.000001, 2, 10, 6)));
        assertNull(GeometryUtil.pixelAlignedBounds(
                AffineTransform.getShearInstance(1, 0), new Rectangle2D.Double(1, 2, 10, 6)));
    }
}
