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
package com.github.weisj.jsvg.nodes.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class ClippedChannelTest {

    @Test
    void preservesConstantColorMetadataWithoutPreservingInfiniteExtent() {
        int color = 0xff4080c0;
        Channel clipped = new ClippedChannel(
                ConstantColorChannel.infinite(10, 10, color), new Rectangle(2, 3, 4, 5), 10, 10);

        assertTrue(clipped.isConstant());
        assertEquals(color, clipped.constantColor());
        assertFalse(clipped.isInfiniteExtent());

        int replacement = 0xffc08040;
        Channel recolored = clipped.withConstantColor(replacement);
        assertTrue(recolored.isConstant());
        assertEquals(replacement, recolored.constantColor());
        assertFalse(recolored.isInfiniteExtent());
    }
}
