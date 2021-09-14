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
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories(Category.Gradient)
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {Stop.class /* <animate>, <animateTransform>, <set> */ }
)
public final class RadialGradient extends AbstractGradient<RadialGradient> {
    public static final String TAG = "radialgradient";

    private Length cx;
    private Length cy;
    private Length r;
    private Length fr;
    private Length fx;
    private Length fy;

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected void buildGradient(@NotNull AttributeNode attributeNode, @Nullable RadialGradient template) {
        cx = attributeNode.getLength("cx", template != null ? template.cx : Unit.PERCENTAGE.valueOf(50));
        cy = attributeNode.getLength("cy", template != null ? template.cy : Unit.PERCENTAGE.valueOf(50));
        r = attributeNode.getLength("r", template != null ? template.r : Unit.PERCENTAGE.valueOf(50));
        fr = attributeNode.getLength("fr", template != null ? template.fr : Unit.PERCENTAGE.valueOf(0));
        fx = attributeNode.getLength("fx", template != null ? template.fx : cx);
        fy = attributeNode.getLength("fy", template != null ? template.fy : cy);
    }

    @Override
    protected @NotNull Paint gradientForBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds,
            @Percentage float[] gradOffsets, @NotNull Color[] gradColors) {
        Point2D.Float center = new Point2D.Float(cx.resolveWidth(measure), cy.resolveHeight(measure));
        Point2D.Float focusCenter = new Point2D.Float(fx.resolveWidth(measure), fy.resolveHeight(measure));

        float radius = r.resolveLength(measure);
        float focusRadius = fr.resolveLength(measure);

        float[] offsets = gradOffsets;
        Color[] offsetColors = gradColors;
        if (focusRadius > 0) {
            // Todo: This isn't entirely correct. The 0% offset should lie on the boundary of the circle, which
            // is skewed the way we handle it currently.
            float[] newOffsets = new float[offsets.length + 1];
            newOffsets[0] = Math.nextAfter(focusRadius / radius, Float.NEGATIVE_INFINITY);
            float offset = focusRadius / radius;
            float factor = 1 - offset;
            for (int i = 1; i < newOffsets.length; i++) {
                newOffsets[i] = offset + offsets[i - 1] * factor;
            }
            offsets = newOffsets;

            Color[] newColors = new Color[gradColors.length + 1];
            System.arraycopy(offsetColors, 0, newColors, 1, gradColors.length);
            offsetColors = newColors;
            newColors[0] = gradColors[0];
        }
        return new RadialGradientPaint(center, radius, focusCenter,
                offsets, offsetColors, spreadMethod.cycleMethod(),
                MultipleGradientPaint.ColorSpaceType.SRGB, computeViewTransform(bounds));
    }

    @Override
    public String toString() {
        return "RadialGradient{" +
                "spreadMethod=" + spreadMethod +
                ", gradientTransform=" + gradientTransform +
                ", cx=" + cx +
                ", cy=" + cy +
                ", r=" + r +
                ", fr=" + fr +
                ", fx=" + fx +
                ", fy=" + fy +
                ", colors=" + Arrays.toString(colors()) +
                ", offsets=" + Arrays.toString(offsets()) +
                '}';
    }
}
