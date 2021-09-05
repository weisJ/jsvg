/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
package com.github.weisj.jsvg.nodes;


import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.geometry.MeasurableShape;
import com.github.weisj.jsvg.geometry.SVGRectangle;
import com.github.weisj.jsvg.geometry.SVGRoundRectangle;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories({Category.BasicShape, Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Rect extends ShapeNode {
    public static final String TAG = "rect";

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected @NotNull MeasurableShape buildShape(@NotNull AttributeNode node) {
        Length x = node.getLength("x", 0);
        Length y = node.getLength("y", 0);
        Length width = node.getLength("width", 0);
        Length height = node.getLength("height", 0);

        Length rx = node.getLength("rx", Length.UNSPECIFIED);
        Length ry = node.getLength("ry", rx); // Use rx as fallback
        if (rx.isUnspecified()) {
            rx = ry; // If rx is not specified use
        }

        rx = rx.coerceNonNegative().orElseIfUnspecified(0);
        ry = ry.coerceNonNegative().orElseIfUnspecified(0);

        if (rx.isZero() && ry.isZero()) {
            return new SVGRectangle(x, y, width, height);
        } else {
            return new SVGRoundRectangle(x, y, width, height, rx, ry);
        }
    }
}
