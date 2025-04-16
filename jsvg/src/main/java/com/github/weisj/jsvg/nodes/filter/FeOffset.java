/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jannis Weis
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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageFilter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeOffset extends AbstractFilterPrimitive {
    public static final String TAG = "feOffset";

    private float dx;
    private float dy;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        dx = attributeNode.getFloat("dx", 0);
        dy = attributeNode.getFloat("dy", 0);
    }

    private Point2D.Double offset(@Nullable AffineTransform at, @NotNull UnitType filterPrimitiveUnits,
            @NotNull Rectangle2D elementBounds) {
        Point2D.Double off = new Point2D.Double(dx, dy);
        if (at != null) {
            off.x *= GeometryUtil.scaleXOfTransform(at);
            off.y *= GeometryUtil.scaleYOfTransform(at);
        }

        if (filterPrimitiveUnits == UnitType.ObjectBoundingBox) {
            off.x *= elementBounds.getWidth();
            off.y *= elementBounds.getHeight();
        }

        return off;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        Point2D.Double off = offset(null, filterLayoutContext.primitiveUnits(), filterLayoutContext.elementBounds());
        LayoutBounds result = input.translate((float) off.x, (float) off.y, filterLayoutContext);
        impl().saveLayoutResult(result, filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        Channel in = impl().inputChannel(filterContext);
        Channel result = in;
        if (dx != 0 || dy != 0) {
            AffineTransform at = filterContext.info().output().transform();
            Point2D.Double off = offset(at, filterContext.primitiveUnits(), filterContext.info().elementBounds());
            AffineTransform transform = AffineTransform.getTranslateInstance(off.x, off.y);

            AffineTransformOp op = new AffineTransformOp(transform, filterContext.renderingHints());
            result = in.applyFilter(new BufferedImageFilter(op));
        }

        impl().saveResult(result, filterContext);
    }
}
