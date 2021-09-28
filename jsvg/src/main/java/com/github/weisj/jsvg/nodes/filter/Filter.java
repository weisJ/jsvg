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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories({/* None */})
@PermittedContent(
    categories = {Category.Descriptive, Category.FilterPrimitive},
    anyOf = { /* <animate>, <set> */ }
)
public final class Filter extends ContainerNode {
    private static final boolean DEBUG = false;
    public static final String TAG = "filter";

    private static final Length DEFAULT_FILTER_COORDINATE = Unit.PERCENTAGE.valueOf(-10);
    private static final Length DEFAULT_FILTER_SIZE = Unit.PERCENTAGE.valueOf(120);

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private UnitType filterUnits;
    private UnitType filterPrimitiveUnits;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public boolean hasEffect() {
        return !children().isEmpty();
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", DEFAULT_FILTER_COORDINATE);
        y = attributeNode.getLength("y", DEFAULT_FILTER_COORDINATE);
        width = attributeNode.getLength("width", DEFAULT_FILTER_SIZE);
        height = attributeNode.getLength("height", DEFAULT_FILTER_SIZE);

        filterUnits = attributeNode.getEnum("filterUnits", UnitType.ObjectBoundingBox);
        filterPrimitiveUnits = attributeNode.getEnum("primitiveUnits", UnitType.UserSpaceOnUse);
    }

    public @NotNull FilterInfo createFilterInfo(@NotNull Graphics2D g, @NotNull RenderContext context,
            @NotNull Rectangle2D elementBounds) {

        MeasureContext measure = context.measureContext();
        Rectangle2D.Double imageBounds = filterUnits.computeViewBounds(measure, elementBounds, x, y, width, height);

        if (filterUnits == UnitType.ObjectBoundingBox) {
            imageBounds.x += elementBounds.getX();
            imageBounds.y += elementBounds.getY();
        }

        BufferedImage image = ImageUtil.createCompatibleTransparentImage(g,
                imageBounds.getWidth(), imageBounds.getHeight());

        Rectangle2D filterRegion = new Rectangle2D.Double(
                -imageBounds.getX(), -imageBounds.getY(),
                elementBounds.getWidth(), elementBounds.getHeight());

        return new FilterInfo(g, image, imageBounds, filterRegion, elementBounds);
    }

    public void applyFilter(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull FilterInfo filterInfo) {
        ImageProducer producer = filterInfo.image.getSource();

        FilterContext filterContext = new FilterContext(filterInfo);

        Channel sourceChannel = new ImageProducerChannel(producer);
        filterContext.addResult(DefaultFilterChannel.SourceGraphic, sourceChannel);
        filterContext.addResult(DefaultFilterChannel.LastResult, sourceChannel);

        for (SVGNode child : children()) {
            FilterPrimitive filterPrimitive = (FilterPrimitive) child;
            Rectangle2D.Double filterPrimitiveRegion = filterPrimitiveUnits.computeViewBounds(
                    context.measureContext(), filterInfo.elementBounds,
                    filterPrimitive.x, filterPrimitive.y, filterPrimitive.width, filterPrimitive.height);

            filterInfo.imageGraphics.dispose();

            if (filterPrimitiveUnits == UnitType.ObjectBoundingBox) {
                filterPrimitiveRegion.x += filterInfo.elementBounds.getX();
                filterPrimitiveRegion.y += filterInfo.elementBounds.getY();
            }

            Rectangle2D.intersect(filterPrimitiveRegion, filterInfo.imageBounds, filterPrimitiveRegion);

            filterPrimitive.applyFilter(g, context, filterContext);

            // Todo: Respect filterPrimitiveRegion
        }

        filterInfo.producer = Objects.requireNonNull(
                filterContext.getChannel(DefaultFilterChannel.LastResult)).producer();
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof FilterPrimitive && super.acceptChild(id, node);
    }

    public static class FilterInfo {
        public final @NotNull Rectangle2D imageBounds;
        public final int imageWidth;
        public final int imageHeight;

        private final @NotNull Rectangle2D elementBounds;
        private final @NotNull Graphics2D imageGraphics;
        private final @NotNull BufferedImage image;

        private ImageProducer producer;

        private FilterInfo(@NotNull Graphics2D g, @NotNull BufferedImage image, @NotNull Rectangle2D imageBounds,
                @NotNull Rectangle2D filterRegion, @NotNull Rectangle2D elementBounds) {
            this.image = image;
            this.imageBounds = imageBounds;
            this.elementBounds = elementBounds;

            this.imageWidth = image.getWidth();
            this.imageHeight = image.getHeight();

            this.imageGraphics = image.createGraphics();
            this.imageGraphics.setRenderingHints(g.getRenderingHints());
            this.imageGraphics.scale(
                    image.getWidth() / imageBounds.getWidth(),
                    image.getHeight() / imageBounds.getHeight());
            this.imageGraphics.translate(filterRegion.getX(), filterRegion.getY());
        }

        public @NotNull Graphics2D graphics() {
            return imageGraphics;
        }

        public @NotNull Rectangle2D tile() {
            return new Rectangle2D.Double(
                    imageBounds.getX() - elementBounds.getX(),
                    imageBounds.getY() - elementBounds.getY(),
                    imageBounds.getWidth(),
                    imageBounds.getHeight());
        }

        public void blitImage(@NotNull Graphics2D g, @NotNull RenderContext context) {
            if (DEBUG) {
                GraphicsUtil.safelySetPaint(g, Color.RED);
                g.draw(imageBounds);
            }
            g.translate(imageBounds.getX(), imageBounds.getY());
            g.scale(imageBounds.getWidth() / image.getWidth(), imageBounds.getHeight() / image.getHeight());

            Image image = context.createImage(producer);
            g.drawImage(image, 0, 0, context.targetComponent());
        }
    }
}
