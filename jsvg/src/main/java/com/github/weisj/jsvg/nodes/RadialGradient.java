/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.jdk.SVGRadialGradientPaint;

@ElementCategories(Category.Gradient)
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {Stop.class, Animate.class, AnimateTransform.class, Set.class,}
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
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected void buildGradient(@NotNull AttributeNode attributeNode, @Nullable RadialGradient template) {
        cx = attributeNode.getLength("cx", PercentageDimension.WIDTH,
                template != null ? template.cx : Unit.PERCENTAGE_WIDTH.valueOf(50));
        cy = attributeNode.getLength("cy", PercentageDimension.HEIGHT,
                template != null ? template.cy : Unit.PERCENTAGE_HEIGHT.valueOf(50));
        r = attributeNode.getLength("r", PercentageDimension.LENGTH,
                template != null ? template.r : Unit.PERCENTAGE_LENGTH.valueOf(50));
        fr = attributeNode.getLength("fr", PercentageDimension.LENGTH,
                template != null ? template.fr : Unit.PERCENTAGE_LENGTH.valueOf(0));
        fx = attributeNode.getLength("fx", PercentageDimension.WIDTH,
                template != null ? template.fx : cx);
        fy = attributeNode.getLength("fy", PercentageDimension.HEIGHT,
                template != null ? template.fy : cy);
    }

    @Override
    protected @NotNull Paint gradientForBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds,
            Percentage[] gradOffsets, @NotNull Color[] gradColors) {
        assert gradColors.length > 0;
        Point2D.Float center = new Point2D.Float(cx.resolve(measure), cy.resolve(measure));
        Point2D.Float focusCenter = new Point2D.Float(fx.resolve(measure), fy.resolve(measure));

        float radius = r.resolve(measure);
        float focusRadius = fr.resolve(measure);

        if (radius <= 0) {
            return gradColors[gradColors.length - 1];
        }

        if (focusRadius == 0) {
            // If possible use built-in RadialGradientPaint as it profits from hardware acceleration
            return new RadialGradientPaint(center, radius, focusCenter,
                    offsetsToFractions(gradOffsets), gradColors, spreadMethod.cycleMethod(),
                    MultipleGradientPaint.ColorSpaceType.SRGB, computeViewTransform(measure, bounds));
        }

        return new SVGRadialGradientPaint(center, radius, focusCenter, focusRadius,
                offsetsToFractions(gradOffsets), gradColors, spreadMethod.cycleMethod(),
                MultipleGradientPaint.ColorSpaceType.SRGB, computeViewTransform(measure, bounds));
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
