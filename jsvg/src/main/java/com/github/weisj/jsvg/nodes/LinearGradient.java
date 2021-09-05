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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories(Category.Gradient)
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {Stop.class /* <animate>, <animateTransform>, <set> */ }
)
public final class LinearGradient extends AbstractGradient<LinearGradient> {
    public static final String TAG = "lineargradient";

    private @Percentage float x1;
    private @Percentage float x2;
    private @Percentage float y1;
    private @Percentage float y2;

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected void buildGradient(@NotNull AttributeNode attributeNode, @Nullable LinearGradient template) {
        x1 = attributeNode.getPercentage("x1", template != null ? template.x1 : 0);
        x2 = attributeNode.getPercentage("x2", template != null ? template.x2 : 1);
        y1 = attributeNode.getPercentage("y1", template != null ? template.y1 : 0);
        y2 = attributeNode.getPercentage("y2", template != null ? template.y2 : 0);
    }

    @Override
    protected @NotNull Paint gradientForBounds(@NotNull Rectangle2D bounds,
            @Percentage float[] gradOffsets, @NotNull Color[] gradColors) {
        Point2D.Float pt1 = new Point2D.Float(x1, y1);
        Point2D.Float pt2 = new Point2D.Float(x2, y2);
        if (pt1.equals(pt2)) return gradColors[0];

        return new LinearGradientPaint(pt1, pt2, gradOffsets, gradColors, spreadMethod.cycleMethod(),
                MultipleGradientPaint.ColorSpaceType.SRGB, computeViewTransform(bounds));
    }

    @Override
    public String toString() {
        return "LinearGradient{" +
                "spreadMethod=" + spreadMethod +
                ", gradientTransform=" + gradientTransform +
                ", x1=" + x1 +
                ", x2=" + x2 +
                ", y1=" + y1 +
                ", y2=" + y2 +
                ", colors=" + Arrays.toString(colors()) +
                ", offsets=" + Arrays.toString(offsets()) +
                '}';
    }
}
