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
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.BaseInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.TransformedPaint;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.Container)
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <font>, <font-face>, <foreignObject>, <script>,
     * <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Mask.class, Marker.class, Pattern.class,
            Style.class, Text.class, View.class}
)
public final class Pattern extends BaseInnerViewContainer implements SVGPaint, ShapedContainer<SVGNode> {
    public static final String TAG = "pattern";

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private UnitType patternUnits;
    private UnitType patternContentUnits;
    private AffineTransform patternTransform;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected @NotNull Point2D outerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(0, 0);
    }

    @Override
    protected @Nullable Point2D anchorLocation(@NotNull MeasureContext context) {
        return null;
    }

    @Override
    protected @NotNull Overflow defaultOverflow() {
        return Overflow.Hidden;
    }

    @Override
    public @NotNull FloatSize size(@NotNull RenderContext context) {
        return new FloatSize(
                width.resolveWidth(context.measureContext()),
                height.resolveHeight(context.measureContext()));
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        Pattern template = parseTemplate(attributeNode);

        if (viewBox == null && template != null) viewBox = template.viewBox;
        preserveAspectRatio = template != null ? template.preserveAspectRatio : preserveAspectRatio;

        x = attributeNode.getLength("x", template != null ? template.x : Length.ZERO);
        y = attributeNode.getLength("y", template != null ? template.y : Length.ZERO);
        // Note: width == 0 || height == 0 implies nothing should be painted.
        width = attributeNode.getLength("width", template != null ? template.width : Length.ZERO)
                .coerceNonNegative();
        height = attributeNode.getLength("height", template != null ? template.height : Length.ZERO)
                .coerceNonNegative();

        patternTransform = attributeNode.parseTransform("patternTransform");
        if (patternTransform == null && template != null) patternTransform = template.patternTransform;

        patternUnits = attributeNode.getEnum("patternUnits",
                template != null ? template.patternUnits : UnitType.ObjectBoundingBox);
        patternContentUnits = attributeNode.getEnum("patternContentUnits",
                template != null ? template.patternContentUnits : UnitType.UserSpaceOnUse);
    }

    private @Nullable Pattern parseTemplate(@NotNull AttributeNode attributeNode) {
        Pattern template = attributeNode.getElementByHref(Pattern.class, attributeNode.getHref());
        return template != this ? template : null;
    }

    @Override
    public boolean isVisible() {
        return !width.isZero() && !height.isZero() && SVGPaint.super.isVisible();
    }

    @Override
    public boolean requiresInstantiation() {
        return true;
    }

    @Override
    public void fillShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        GraphicsUtil.safelySetPaint(g, paintForBounds(g, context, b));
        g.fill(shape);
    }

    @Override
    public void drawShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        GraphicsUtil.safelySetPaint(g, paintForBounds(g, context, b));
        g.setPaint(paintForBounds(g, context, b));
        g.draw(shape);
    }

    private @NotNull Paint paintForBounds(@NotNull Graphics2D g, @NotNull RenderContext context,
            @NotNull Rectangle2D bounds) {
        MeasureContext measure = context.measureContext();
        Rectangle2D.Double patternBounds = patternUnits.computeViewBounds(measure, bounds, x, y, width, height);

        // TODO: With overflow = visible this does not result in the correct behaviour
        BufferedImage img = ImageUtil.createCompatibleTransparentImage(g, patternBounds.width, patternBounds.height);
        Graphics2D imgGraphics = GraphicsUtil.createGraphics(img);
        imgGraphics.setRenderingHints(g.getRenderingHints());
        imgGraphics.scale(img.getWidth() / patternBounds.width, img.getHeight() / patternBounds.height);

        RenderContext patternContext = RenderContext.createInitial(null, patternContentUnits.deriveMeasure(measure));
        // TODO: What is the correct root transform here?
        patternContext.setRootTransform(imgGraphics.getTransform());

        FloatSize size;
        ViewBox view = viewBox;
        PreserveAspectRatio aspectRation = this.preserveAspectRatio;
        if (view == null && patternContentUnits == UnitType.ObjectBoundingBox) {
            size = new FloatSize(img.getWidth(), img.getHeight());
            view = new ViewBox(0, 0, 1, 1);
            aspectRation = PreserveAspectRatio.none();
        } else {
            size = new FloatSize((float) patternBounds.getWidth(), (float) patternBounds.getHeight());
        }

        renderWithSize(size, view, aspectRation, patternContext, imgGraphics);
        imgGraphics.dispose();

        // Fixme: When patternTransform != null antialiasing is broken
        return patternTransform != null
                ? new TransformedPaint(new TexturePaint(img, patternBounds), patternTransform)
                : new TexturePaint(img, patternBounds);
    }
}
