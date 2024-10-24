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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
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
import com.github.weisj.jsvg.renderer.ElementBounds;
import com.github.weisj.jsvg.renderer.MaskedPaint;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.CachedSurfaceSupplier;
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

    private final CachedSurfaceSupplier surfaceSupplier =
            new CachedSurfaceSupplier(ImageUtil::createLuminosityBuffer);
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
        maskContentUnits = attributeNode.getEnum("maskContentUnits", UnitType.UserSpaceOnUse);
        maskUnits = attributeNode.getEnum("maskUnits", UnitType.ObjectBoundingBox);

        x = attributeNode.getLength("x", PercentageDimension.WIDTH, Unit.PERCENTAGE_WIDTH.valueOf(-10))
                .coercePercentageToCorrectUnit(maskUnits, PercentageDimension.WIDTH);
        y = attributeNode.getLength("y", PercentageDimension.HEIGHT, Unit.PERCENTAGE_HEIGHT.valueOf(-10))
                .coercePercentageToCorrectUnit(maskUnits, PercentageDimension.HEIGHT);
        width = attributeNode.getLength("width", PercentageDimension.WIDTH, Unit.PERCENTAGE_WIDTH.valueOf(120))
                .coercePercentageToCorrectUnit(maskUnits, PercentageDimension.WIDTH);
        height = attributeNode.getLength("height", PercentageDimension.HEIGHT, Unit.PERCENTAGE_HEIGHT.valueOf(120))
                .coercePercentageToCorrectUnit(maskUnits, PercentageDimension.HEIGHT);
    }

    public @NotNull Paint createMaskPaint(@NotNull Output output, @NotNull RenderContext context,
            @NotNull ElementBounds elementBounds) {
        Rectangle2D.Double maskBounds = maskUnits.computeViewBounds(
                context.measureContext(), elementBounds.boundingBox(), x, y, width, height);

        boolean useCache = surfaceSupplier.useCache(output, context);
        BlittableImage blitImage = BlittableImage.create(
                surfaceSupplier.surfaceSupplier(useCache), context, output.clipBounds(),
                maskBounds.createIntersection(elementBounds.geometryBox()), elementBounds.boundingBox(),
                maskContentUnits);

        if (blitImage == null) return PaintParser.DEFAULT_COLOR;

        blitImage.clearBackground(Color.BLACK);
        blitImage.renderNode(output, this, this);

        if (DEBUG) {
            blitImage.debug(output);
        }

        Point2D offset = GeometryUtil.getLocation(blitImage.imageBoundsInDeviceSpace());
        return new MaskedPaint(PaintParser.DEFAULT_COLOR, blitImage.image().getRaster(), offset,
                surfaceSupplier.resourceCleaner(output, useCache));
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
