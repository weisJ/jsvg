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
package com.github.weisj.jsvg.nodes;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.container.CommonInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;

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
public final class Symbol extends CommonInnerViewContainer {
    public static final String TAG = "symbol";

    private Length refX;
    private Length refY;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected @NotNull Point2D anchorLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(
                -refX.orElseIfUnspecified(0).resolve(context),
                -refY.orElseIfUnspecified(0).resolve(context));
    }

    @Override
    protected @Nullable Point2D anchorLocation(@NotNull MeasureContext context,
            @Nullable AffineTransform viewTransform) {
        if (refX.isUnspecified() && refY.isUnspecified()) return null;
        Point2D anchor = super.anchorLocation(context, viewTransform);
        assert anchor != null;
        // An omitted component leaves the corresponding viewport edge at the use-site position.
        anchor.setLocation(
                refX.isSpecified() ? anchor.getX() : 0,
                refY.isSpecified() ? anchor.getY() : 0);
        return anchor;
    }

    @Override
    protected @NotNull Overflow defaultOverflow() {
        return Overflow.Hidden;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        refX = attributeNode.getHorizontalReferenceLengthFromKey("refX", Length.UNSPECIFIED);
        refY = attributeNode.getVerticalReferenceLengthFromKey("refY", Length.UNSPECIFIED);
    }

    @Override
    public boolean requiresInstantiation() {
        return true;
    }
}
