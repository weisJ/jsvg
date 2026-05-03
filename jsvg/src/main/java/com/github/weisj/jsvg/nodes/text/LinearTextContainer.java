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
package com.github.weisj.jsvg.nodes.text;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.impl.AttributeNode;

abstract class LinearTextContainer<T> extends TextContainer<T> implements CursorContext {
    protected Length[] x;
    protected Length[] y;
    protected Length[] dx;
    protected Length[] dy;
    protected float[] rotate;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLengthList("x", PercentageDimension.WIDTH);
        y = attributeNode.getLengthList("y", PercentageDimension.HEIGHT);
        dx = attributeNode.getLengthList("dx", PercentageDimension.WIDTH);
        dy = attributeNode.getLengthList("dy", PercentageDimension.HEIGHT);
        rotate = attributeNode.getFloatList("rotate");
    }

    @Override
    public @NotNull GlyphCursor createLocalCursor(boolean isInitial, @NotNull GlyphCursor current) {
        GlyphCursor local = current.derive();
        if (x.length != 0) {
            local.xLocations = x;
            local.xOff = 0;
        }
        if (y.length != 0) {
            local.yLocations = y;
            local.yOff = 0;
        }
        if (dx.length != 0) {
            local.xDeltas = dx;
            local.dyOff = 0;
        }
        if (dy.length != 0) {
            local.yDeltas = dy;
            local.dyOff = 0;
        }
        if (rotate.length != 0) {
            local.rotations = rotate;
            local.rotOff = 0;
        }
        return local;
    }

    @Override
    public void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local) {
        current.updateFrom(local);
        if (x.length == 0) current.xOff = local.xOff;
        if (y.length == 0) current.yOff = local.yOff;
        if (dx.length == 0) current.dxOff = local.dxOff;
        if (dy.length == 0) current.dyOff = local.dyOff;
        if (rotate.length == 0) current.rotOff = local.rotOff;
    }
}
