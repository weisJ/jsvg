/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.container.CommonInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.FloatSize;

@ElementCategories({Category.Container, Category.Structural})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <font>, <font-face>, <foreignObject>, <script>,
     * <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Mask.class, Marker.class, Pattern.class,
            Style.class, Text.class, View.class}
)
public final class SVG extends CommonInnerViewContainer {
    public static final String TAG = "svg";

    private static final @NotNull Coordinate<LengthValue> TOP_LEVEL_TRANSFORM_ORIGIN = new Coordinate<>(
            Unit.PERCENTAGE_WIDTH.valueOf(50),
            Unit.PERCENTAGE_WIDTH.valueOf(50));
    private static final float FALLBACK_WIDTH = 300;
    private static final float FALLBACK_HEIGHT = 150;

    private boolean isTopLevel;
    private AnimationPeriod animationPeriod;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public boolean isTopLevel() {
        return isTopLevel;
    }

    public @NotNull AnimationPeriod animationPeriod() {
        return animationPeriod;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        isTopLevel = attributeNode.element().parent() == null;
        super.build(attributeNode);
        animationPeriod = attributeNode.document().animationPeriod();
    }

    @Override
    protected @NotNull Point2D outerLocation(@NotNull MeasureContext context) {
        if (isTopLevel) return new Point(0, 0);
        return super.outerLocation(context);
    }

    @Override
    public @NotNull Coordinate<LengthValue> transformOrigin() {
        if (isTopLevel) return TOP_LEVEL_TRANSFORM_ORIGIN;
        return super.transformOrigin();
    }

    @Override
    protected @NotNull Overflow defaultOverflow() {
        return Overflow.Hidden;
    }

    public @NotNull FloatSize sizeForTopLevel(float em, float ex) {
        // Use a viewport of size 100x100 to interpret percentage values as raw pixels.
        MeasureContext topLevelContext = MeasureContext.createInitial(new FloatSize(100, 100),
                em, ex, AnimationState.NO_ANIMATION);
        return new FloatSize(
                width.orElseIfUnspecified(viewBox != null ? viewBox.width : FALLBACK_WIDTH)
                        .resolve(topLevelContext),
                height.orElseIfUnspecified(viewBox != null ? viewBox.height : FALLBACK_HEIGHT)
                        .resolve(topLevelContext));
    }
}
