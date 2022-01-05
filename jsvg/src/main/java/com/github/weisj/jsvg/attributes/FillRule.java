/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.attributes;

import java.awt.geom.Path2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.AttributeNode;

public enum FillRule {
    /**
     * The value nonzero determines the "insideness" of a point in the shape by drawing a ray from
     * that point to infinity in any direction, and then examining the places where a segment of
     * the shape crosses the ray.
     * Starting with a count of zero, add one each time a path segment crosses the ray from left to
     * right and subtract one each time a path segment crosses the ray from right to left.
     * After counting the crossings, if the result is zero then the point is outside the path.
     * Otherwise, it is inside.
     */
    @Default
    Nonzero(Path2D.WIND_NON_ZERO),
    /**
     * The value evenodd determines the "insideness" of a point in the shape by drawing a ray from
     * that point to infinity in any direction and counting the number of path segments from the given
     * shape that the ray crosses. If this number is odd, the point is inside; if even, the point is outside.
     */
    EvenOdd(Path2D.WIND_EVEN_ODD),
    Inherit(Nonzero.awtWindingRule);

    public final int awtWindingRule;

    FillRule(int awtWindingRule) {
        this.awtWindingRule = awtWindingRule;
    }

    public static @NotNull FillRule parse(@NotNull AttributeNode attributeNode) {
        return attributeNode.getEnum("fill-rule", FillRule.Inherit);
    }
}
