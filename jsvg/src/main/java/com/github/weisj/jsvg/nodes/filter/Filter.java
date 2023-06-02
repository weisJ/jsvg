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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories({/* None */})
@PermittedContent(
    categories = {Category.Descriptive, Category.FilterPrimitive},
    anyOf = {Animate.class, Set.class}
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

    private boolean isValid;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public boolean hasEffect() {
        return isValid && !children().isEmpty();
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        isValid = true;
        for (SVGNode child : children()) {
            FilterPrimitive filterPrimitive = (FilterPrimitive) child;
            if (!filterPrimitive.isValid()) {
                isValid = false;
                break;
            }
        }

        filterUnits = attributeNode.getEnum("filterUnits", UnitType.ObjectBoundingBox);
        filterPrimitiveUnits = attributeNode.getEnum("primitiveUnits", UnitType.UserSpaceOnUse);

        x = attributeNode.getLength("x", DEFAULT_FILTER_COORDINATE);
        y = attributeNode.getLength("y", DEFAULT_FILTER_COORDINATE);
        width = attributeNode.getLength("width", DEFAULT_FILTER_SIZE);
        height = attributeNode.getLength("height", DEFAULT_FILTER_SIZE);

        // Note: Apparently these coordinates are always interpreted as percentages regardless of the
        // specified unit (except for explicit percentages).
        // Unfortunately this results in rather large buffer images in general due to misuse.
        if (filterUnits == UnitType.ObjectBoundingBox) {
            x = coerceToPercentage(x);
            y = coerceToPercentage(y);
            width = coerceToPercentage(width);
            height = coerceToPercentage(height);
        }
    }

    private @NotNull Length coerceToPercentage(@NotNull Length length) {
        if (length.unit() == Unit.PERCENTAGE) return length;
        return new Length(Unit.PERCENTAGE, length.raw() * 100);
    }

    public @NotNull FilterInfo createFilterInfo(@NotNull Graphics2D g, @NotNull RenderContext context,
            @NotNull Rectangle2D elementBounds) {
        Rectangle2D.Double filterRegion = filterUnits.computeViewBounds(
                context.measureContext(), elementBounds, x, y, width, height);
        Rectangle2D graphicsClipBounds = g.getClipBounds();

        FilterLayoutContext filterLayoutContext =
                new FilterLayoutContext(filterPrimitiveUnits, elementBounds, graphicsClipBounds);

        Rectangle2D clippedElementBounds = elementBounds.createIntersection(graphicsClipBounds);

        Rectangle2D effectiveFilterRegion = filterRegion.createIntersection(graphicsClipBounds);
        LayoutBounds elementLayoutBounds = new LayoutBounds(effectiveFilterRegion, new FloatInsets());
        LayoutBounds clippedElementLayoutBounds = new LayoutBounds(clippedElementBounds, new FloatInsets());
        LayoutBounds sourceDependentBounds = elementLayoutBounds.transform(
                (data, flags) -> flags.operatesOnWholeFilterRegion
                        ? data
                        : clippedElementLayoutBounds.resolve(flags));

        filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.LastResult, elementLayoutBounds);
        filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceDependentBounds);
        filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha, sourceDependentBounds);

        for (SVGNode child : children()) {
            FilterPrimitive filterPrimitive = (FilterPrimitive) child;
            filterPrimitive.layoutFilter(context, filterLayoutContext);
        }

        LayoutBounds.Data clipHeuristic = filterLayoutContext.resultChannels()
                .get(DefaultFilterChannel.LastResult)
                .resolve(LayoutBounds.ComputeFlags.INITIAL);

        FloatInsets insets = clipHeuristic.clipBoundsEscapeInsets();
        Rectangle2D clipHeuristicBounds = clipHeuristic.bounds().createIntersection(
                GeometryUtil.grow(graphicsClipBounds, insets));

        BlittableImage blitImage = BlittableImage.create(
                ImageUtil::createCompatibleTransparentImage, context, clipHeuristicBounds,
                filterRegion, elementBounds, UnitType.UserSpaceOnUse);

        return new FilterInfo(g, blitImage, elementBounds);
    }

    public void applyFilter(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull FilterInfo filterInfo) {
        ImageProducer producer = filterInfo.blittableImage.image().getSource();

        FilterContext filterContext = new FilterContext(filterInfo, filterPrimitiveUnits, g.getRenderingHints());

        Channel sourceChannel = new ImageProducerChannel(producer);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceChannel);
        filterContext.resultChannels().addResult(DefaultFilterChannel.LastResult, sourceChannel);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha,
                () -> sourceChannel.applyFilter(new AlphaImageFilter()));

        for (SVGNode child : children()) {
            try {
                FilterPrimitive filterPrimitive = (FilterPrimitive) child;
                filterPrimitive.applyFilter(context, filterContext);
            } catch (IllegalFilterStateException ignored) {
                // Just carry on applying filters
            }
            // Todo: Respect filterPrimitiveRegion
        }

        filterInfo.producer = Objects.requireNonNull(
                filterContext.getChannel(DefaultFilterChannel.LastResult)).producer();
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof FilterPrimitive && super.acceptChild(id, node);
    }

    public static final class FilterInfo {
        public final int imageWidth;
        public final int imageHeight;

        private final @NotNull Rectangle2D elementBounds;
        private final @NotNull Graphics2D imageGraphics;
        private final @NotNull BlittableImage blittableImage;

        private ImageProducer producer;

        private FilterInfo(@NotNull Graphics2D g, @NotNull BlittableImage blittableImage,
                @NotNull Rectangle2D elementBounds) {
            this.blittableImage = blittableImage;
            this.elementBounds = elementBounds;

            BufferedImage image = blittableImage.image();

            this.imageWidth = image.getWidth();
            this.imageHeight = image.getHeight();

            this.imageGraphics = blittableImage.createGraphics();
            this.imageGraphics.setRenderingHints(g.getRenderingHints());
        }

        public @NotNull Rectangle2D imageBounds() {
            return blittableImage.boundsInUserSpace();
        }

        public @NotNull Rectangle2D elementBounds() {
            return elementBounds;
        }

        public @NotNull Graphics2D graphics() {
            return imageGraphics;
        }

        public @NotNull Rectangle2D tile() {
            Rectangle2D imageBounds = imageBounds();
            return new Rectangle2D.Double(
                    imageBounds.getX() - elementBounds.getX(),
                    imageBounds.getY() - elementBounds.getY(),
                    imageBounds.getWidth(),
                    imageBounds.getHeight());
        }

        public void blitImage(@NotNull Graphics2D g, @NotNull RenderContext context) {
            Rectangle2D imageBounds = imageBounds();

            if (DEBUG) {
                GraphicsUtil.safelySetPaint(g, Color.RED);
                g.draw(imageBounds);
            }

            blittableImage.prepareForBlitting(g, context);
            g.drawImage(context.createImage(producer), 0, 0, context.targetComponent());
        }

        public void close() {
            imageGraphics.dispose();
        }
    }

    private static final class AlphaImageFilter extends RGBImageFilter {
        private final ColorModel model = ColorModel.getRGBdefault();

        @Override
        public int filterRGB(int x, int y, int rgb) {
            return model.getAlpha(rgb) << 24;
        }
    }
}
