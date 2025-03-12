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
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.BaseInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.AttributeNode.ElementRelation;
import com.github.weisj.jsvg.renderer.*;
import com.github.weisj.jsvg.renderer.impl.RenderContext;
import com.github.weisj.jsvg.renderer.impl.TransformedPaint;
import com.github.weisj.jsvg.util.BlittableImage;
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
public final class Pattern extends BaseInnerViewContainer implements SVGPaint, ShapedContainer<SVGNode>, Instantiator {
    public static final String TAG = "pattern";

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private UnitType patternUnits;
    private UnitType patternContentUnits;
    private TransformValue patternTransform;

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
                width.resolve(context.measureContext()),
                height.resolve(context.measureContext()));
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        Pattern template = parseTemplate(attributeNode);

        if (viewBox == null && template != null) viewBox = template.viewBox;
        preserveAspectRatio = template != null ? template.preserveAspectRatio : preserveAspectRatio;

        patternUnits = attributeNode.getEnum("patternUnits",
                template != null ? template.patternUnits : UnitType.ObjectBoundingBox);
        patternContentUnits = attributeNode.getEnum("patternContentUnits",
                template != null ? template.patternContentUnits : UnitType.UserSpaceOnUse);

        x = attributeNode
                .getLength("x", PercentageDimension.WIDTH, template != null ? template.x : Length.ZERO)
                .coercePercentageToCorrectUnit(patternUnits, PercentageDimension.WIDTH);
        y = attributeNode
                .getLength("y", PercentageDimension.HEIGHT, template != null ? template.y : Length.ZERO)
                .coercePercentageToCorrectUnit(patternUnits, PercentageDimension.HEIGHT);
        // Note: width == 0 || height == 0 implies nothing should be painted.
        width = attributeNode
                .getLength("width", PercentageDimension.WIDTH, template != null ? template.width : Length.ZERO)
                .coerceNonNegative()
                .coercePercentageToCorrectUnit(patternUnits, PercentageDimension.WIDTH);
        height = attributeNode
                .getLength("height", PercentageDimension.HEIGHT, template != null ? template.height : Length.ZERO)
                .coerceNonNegative()
                .coercePercentageToCorrectUnit(patternUnits, PercentageDimension.HEIGHT);

        patternTransform = attributeNode.parseTransform("patternTransform");
        if (patternTransform == null && template != null) patternTransform = template.patternTransform;
    }

    private @Nullable Pattern parseTemplate(@NotNull AttributeNode attributeNode) {
        Pattern template =
                attributeNode.getElementByHref(Pattern.class, attributeNode.getHref(), ElementRelation.TEMPLATE);
        return template != this ? template : null;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return !width.isZero() && !height.isZero() && SVGPaint.super.isVisible(context);
    }

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        // TODO: Might not fully want to avoid this here if shape computation should become more fine
        // grained.
        output.setPaint(() -> paintForBounds(output, context, b));
        output.fillShape(shape);
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        output.setPaint(() -> paintForBounds(output, context, b));
        output.drawShape(shape);
    }

    private @NotNull Paint paintForBounds(@NotNull Output output, @NotNull RenderContext context,
            @NotNull Rectangle2D bounds) {
        MeasureContext measure = context.measureContext();
        Rectangle2D.Double patternBounds = patternUnits.computeViewBounds(measure, bounds, x, y, width, height);

        // TODO: With overflow = visible this does not result in the correct behaviour
        BlittableImage blittableImage = BlittableImage.create(
                ImageUtil::createCompatibleTransparentImage, context, null,
                patternBounds, bounds, patternContentUnits);

        if (blittableImage == null) return PaintParser.DEFAULT_COLOR;

        blittableImage.render(output, (out, ctx) -> {
            if (patternContentUnits == UnitType.UserSpaceOnUse) {
                ctx.translate(out, patternBounds.getX(), patternBounds.getY());
            }
            renderWithSize(new FloatSize(patternBounds), viewBox, ctx, out);
        });

        // Fixme: When patternTransform != null antialiasing is broken
        return patternTransform != null
                ? new TransformedPaint(new TexturePaint(blittableImage.image(), patternBounds),
                        patternTransform.get(measure))
                : new TexturePaint(blittableImage.image(), patternBounds);
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
