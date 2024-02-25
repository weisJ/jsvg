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
import java.awt.geom.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.MaskedPaint;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.ShapeUtil;

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

    public @NotNull Shape clipShape(@NotNull RenderContext context, @NotNull Rectangle2D elementBounds,
            boolean useSoftClip) {
        // Todo: Handle bounding-box stuff as well (i.e. combined stroke etc.)
        Shape shape = ShapedContainer.super.elementShape(context);
        if (!useSoftClip && clipPathUnits == UnitType.ObjectBoundingBox) {
            shape = clipPathUnits.viewTransform(elementBounds).createTransformedShape(shape);
        }
        Area areaShape = new Area(shape);
        if (areaShape.isRectangular()) {
            return areaShape.getBounds2D();
        }
        return areaShape;
    }

    public @NotNull Paint createPaintForSoftClipping(@NotNull Output output, @NotNull RenderContext context,
            @NotNull Rectangle2D objectBounds, @NotNull Shape clipShape) {
        Rectangle2D transformedClipBounds = GeometryUtil.containingBoundsAfterTransform(
                clipPathUnits.viewTransform(objectBounds),
                clipShape.getBounds2D());
        BlittableImage blitImage = BlittableImage.create(
                ImageUtil::createLuminosityBuffer, context, output.clipBounds(),
                transformedClipBounds, objectBounds, clipPathUnits);
        Rectangle2D clipBoundsInUserSpace = blitImage.boundsInUserSpace();

        if (ShapeUtil.isInvalidArea(clipBoundsInUserSpace)) return PaintParser.DEFAULT_COLOR;

        blitImage.render(output, g -> {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, blitImage.image().getWidth(), blitImage.image().getHeight());
            g.setColor(Color.WHITE);
            g.fill(clipShape);
        });

        Point2D offset = new Point2D.Double(clipBoundsInUserSpace.getX(), clipBoundsInUserSpace.getY());
        context.rootTransform().transform(offset, offset);

        return new MaskedPaint(PaintParser.DEFAULT_COLOR, blitImage.image().getRaster(), offset);
    }
}
