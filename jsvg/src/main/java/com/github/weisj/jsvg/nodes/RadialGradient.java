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
public final class RadialGradient extends AbstractGradient {
    public static final String TAG = "radialgradient";

    private @Percentage float cx;
    private @Percentage float cy;
    private @Percentage float fr;
    private @Percentage float fx;
    private @Percentage float fy;

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        cx = attributeNode.getPercentage("cx", 0.5f);
        cy = attributeNode.getPercentage("cy", 0.5f);
        fr = attributeNode.getPercentage("fr", 0);
        fx = attributeNode.getPercentage("fx", cx);
        fy = attributeNode.getPercentage("fy", cy);
    }

    @Override
    protected @NotNull Paint gradientForBounds(@NotNull Rectangle2D bounds,
            @Percentage float[] gradOffsets, @NotNull Color[] gradColors) {
        Point2D.Float pt1 = new Point2D.Float(cy, cy);
        Point2D.Float pt2 = new Point2D.Float(fx, fy);
        return new RadialGradientPaint(pt1, fr, pt2, gradOffsets, gradColors, spreadMethod,
                MultipleGradientPaint.ColorSpaceType.SRGB, computeViewTransform(bounds));
    }

    @Override
    public String toString() {
        return "RadialGradient{" +
                "spreadMethod=" + spreadMethod +
                ", gradientTransform=" + gradientTransform +
                ", cx=" + cx +
                ", cy=" + cy +
                ", fr=" + fr +
                ", fx=" + fx +
                ", fy=" + fy +
                ", colors=" + Arrays.toString(colors()) +
                ", offsets=" + Arrays.toString(offsets()) +
                '}';
    }
}
