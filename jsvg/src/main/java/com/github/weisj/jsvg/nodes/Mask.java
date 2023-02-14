/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.container.CommonRenderableContainerNode;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.MaskedPaint;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.Container)
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <font>, <font-face>, <foreignObject>, <script>,
     * <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Marker.class, Mask.class, Pattern.class,
            Style.class, Text.class, View.class}
)
public final class Mask extends CommonRenderableContainerNode implements Instantiator {
    private static final boolean DEBUG = false;
    public static final String TAG = "mask";

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private UnitType maskContentUnits;
    private UnitType maskUnits;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", Unit.PERCENTAGE.valueOf(-10));
        y = attributeNode.getLength("y", Unit.PERCENTAGE.valueOf(-10));
        width = attributeNode.getLength("width", Unit.PERCENTAGE.valueOf(120));
        height = attributeNode.getLength("height", Unit.PERCENTAGE.valueOf(120));

        maskContentUnits = attributeNode.getEnum("maskContentUnits", UnitType.UserSpaceOnUse);
        maskUnits = attributeNode.getEnum("maskUnits", UnitType.ObjectBoundingBox);
    }

    public @NotNull Paint createMaskPaint(@NotNull Graphics2D g, @NotNull RenderContext context,
            @NotNull Rectangle2D objectBounds) {
        MeasureContext measure = context.measureContext();

        Rectangle2D.Double maskBounds = maskUnits.computeViewBounds(measure, objectBounds, x, y, width, height);
        if (isInvalidMaskingArea(maskBounds)) return PaintParser.DEFAULT_COLOR;

        if (maskUnits == UnitType.ObjectBoundingBox) {
            maskBounds.x += objectBounds.getX();
            maskBounds.y += objectBounds.getY();
        }

        AffineTransform userSpaceTransform = context.userSpaceTransform();
        Rectangle2D maskBoundsInUserSpace = new Rectangle2D.Float();
        Rectangle2D.intersect(
                GeometryUtil.containingBoundsAfterTransform(userSpaceTransform, maskBounds),
                GeometryUtil.containingBoundsAfterTransform(userSpaceTransform, objectBounds),
                maskBoundsInUserSpace);

        if (isInvalidMaskingArea(maskBoundsInUserSpace)) return PaintParser.DEFAULT_COLOR;

        RenderContext maskContext =
                RenderContext.createInitial(context.targetComponent(), maskContentUnits.deriveMeasure(measure));

        BufferedImage img = ImageUtil.createLuminosityBuffer(context.rootTransform(),
                maskBoundsInUserSpace.getWidth(), maskBoundsInUserSpace.getHeight());
        Graphics2D imgGraphics = (Graphics2D) img.getGraphics();
        imgGraphics.setRenderingHints(g.getRenderingHints());

        if (maskContentUnits == UnitType.ObjectBoundingBox) {
            imgGraphics.scale(
                    objectBounds.getWidth() * img.getWidth() / maskBoundsInUserSpace.getWidth(),
                    objectBounds.getWidth() * img.getHeight() / maskBoundsInUserSpace.getHeight());
        } else {
            imgGraphics.scale(
                    img.getWidth() / maskBoundsInUserSpace.getWidth(),
                    img.getHeight() / maskBoundsInUserSpace.getHeight());
            imgGraphics.translate(-maskBoundsInUserSpace.getX(), -maskBoundsInUserSpace.getY());
            imgGraphics.transform(userSpaceTransform);
        }

        try (NodeRenderer.Info info = NodeRenderer.createRenderInfo(this, maskContext, imgGraphics, this)) {
            if (info != null) info.renderable.render(info.context, info.graphics());
        }
        imgGraphics.dispose();

        if (DEBUG) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setTransform(context.rootTransform());
            gg.translate(maskBoundsInUserSpace.getX(), maskBoundsInUserSpace.getY());
            gg.scale(
                    maskBoundsInUserSpace.getWidth() / img.getWidth(),
                    maskBoundsInUserSpace.getHeight() / img.getHeight());

            gg.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            gg.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null, null);

            gg.dispose();
        }

        Point2D offset = new Point2D.Double(maskBoundsInUserSpace.getX(), maskBoundsInUserSpace.getY());
        context.rootTransform().transform(offset, offset);
        return new MaskedPaint(PaintParser.DEFAULT_COLOR, img.getRaster(), offset);
    }

    private boolean isInvalidMaskingArea(@NotNull Rectangle2D area) {
        return area.isEmpty() || Double.isNaN(area.getWidth()) || Double.isNaN(area.getHeight());
    }

    @Override
    public boolean requiresInstantiation() {
        return true;
    }

    @Override
    public boolean canInstantiate(@NotNull SVGNode node) {
        return node == this;
    }
}
