/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageFilter;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = { /* <animate>, <set> */ }
)
public class FeOffset extends AbstractFilterPrimitive {
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

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        Channel in = impl().inputChannel(filterContext);
        Channel result = in;
        if (dx != 0 || dy != 0) {
            AffineTransform at = filterContext.info().graphics().getTransform();
            double effectiveDx = GeometryUtil.scaleXOfTransform(at) * dx;
            double effectiveDy = GeometryUtil.scaleYOfTransform(at) * dy;

            if (filterContext.primitiveUnits() == UnitType.ObjectBoundingBox) {
                Rectangle2D elementBounds = filterContext.info().elementBounds();
                effectiveDx *= elementBounds.getWidth();
                effectiveDy *= elementBounds.getHeight();
            }

            AffineTransform transform = AffineTransform.getTranslateInstance(effectiveDx, effectiveDy);

            AffineTransformOp op = new AffineTransformOp(transform, filterContext.renderingHints());
            result = in.applyFilter(new BufferedImageFilter(op));
        }

        impl().saveResult(result, filterContext);
    }
}
