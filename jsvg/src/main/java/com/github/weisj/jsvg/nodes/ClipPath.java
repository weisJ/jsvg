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
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories({/* None */})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape},
    anyOf = {Use.class, Text.class}
)
public final class ClipPath extends ContainerNode implements ShapedContainer<SVGNode> {
    public static final String TAG = "clippath";
    private boolean isValid;

    private UnitType clipPathUnits;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public boolean isValid() {
        return isValid;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        isValid = checkIsValid();
        clipPathUnits = attributeNode.getEnum("clipPathUnits", UnitType.UserSpaceOnUse);
    }

    private boolean checkIsValid() {
        for (SVGNode child : children()) {
            if (!(child instanceof Use)) continue;
            SVGNode referenced = ((Use) child).referencedNode();
            if (referenced == null) continue;
            if (!isAcceptableType(referenced)) {
                return false;
            }
        }
        return true;
    }

    public @NotNull Shape clipShape(@NotNull RenderContext context, @NotNull Rectangle2D elementBounds) {
        // Todo: Handle bounding-box stuff as well (i.e. combined stroke etc.)
        Shape shape = ShapedContainer.super.elementShape(context);
        if (clipPathUnits == UnitType.ObjectBoundingBox) {
            return clipPathUnits.viewTransform(elementBounds).createTransformedShape(shape);
        } else {
            return shape;
        }
    }
}
