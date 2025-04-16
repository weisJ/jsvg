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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
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
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.util.BlittableImage;

@ElementCategories({/* None */})
@PermittedContent(
    categories = {Category.Descriptive, Category.FilterPrimitive},
    anyOf = {Animate.class, Set.class}
)
public final class Filter extends ContainerNode {
    private static final Logger LOGGER = Logger.getLogger(Filter.class.getName());
    public static final String TAG = "filter";

    private static final Length DEFAULT_FILTER_COORDINATE_X = Unit.PERCENTAGE_WIDTH.valueOf(-10);
    private static final Length DEFAULT_FILTER_COORDINATE_Y = Unit.PERCENTAGE_HEIGHT.valueOf(-10);
    private static final Length DEFAULT_FILTER_WIDTH = Unit.PERCENTAGE_WIDTH.valueOf(120);
    private static final Length DEFAULT_FILTER_HEIGHT = Unit.PERCENTAGE_HEIGHT.valueOf(120);
    private static final Rectangle2D.Double NO_CLIP_BOUNDS = new Rectangle2D.Double(
            -(Double.MAX_VALUE / 3), -(Double.MAX_VALUE / 3),
            2 * (Double.MAX_VALUE / 3), 2 * (Double.MAX_VALUE / 3));

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private UnitType filterUnits;
    private UnitType filterPrimitiveUnits;
    private ColorInterpolation colorInterpolation;

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
        colorInterpolation = attributeNode.getEnum("color-interpolation-filters", ColorInterpolation.LinearRGB);

        x = attributeNode.getLength("x", PercentageDimension.WIDTH, DEFAULT_FILTER_COORDINATE_X)
                .coercePercentageToCorrectUnit(filterUnits, PercentageDimension.WIDTH);
        y = attributeNode.getLength("y", PercentageDimension.HEIGHT, DEFAULT_FILTER_COORDINATE_Y)
                .coercePercentageToCorrectUnit(filterUnits, PercentageDimension.HEIGHT);
        width = attributeNode.getLength("width", PercentageDimension.WIDTH, DEFAULT_FILTER_WIDTH)
                .coercePercentageToCorrectUnit(filterUnits, PercentageDimension.WIDTH);
        height = attributeNode.getLength("height", PercentageDimension.HEIGHT, DEFAULT_FILTER_HEIGHT)
                .coercePercentageToCorrectUnit(filterUnits, PercentageDimension.HEIGHT);
    }

    public @Nullable FilterBounds createFilterBounds(@Nullable Output output, @NotNull RenderContext context,
            @NotNull ElementBounds elementBounds) {
        Rectangle2D.Double filterRegion = filterUnits.computeViewBounds(
                context.measureContext(), elementBounds.boundingBox(), x, y, width, height);
        Rectangle2D graphicsClipBounds = output != null
                ? output.clipBounds()
                : NO_CLIP_BOUNDS.getBounds2D();

        FilterLayoutContext filterLayoutContext =
                new FilterLayoutContext(filterPrimitiveUnits, elementBounds.boundingBox(), graphicsClipBounds);

        Rectangle2D clippedElementBounds = elementBounds.geometryBox().createIntersection(graphicsClipBounds);
        Rectangle2D effectiveFilterRegion = filterRegion.createIntersection(graphicsClipBounds);

        if (effectiveFilterRegion.isEmpty()) return null;

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
            try {
                FilterPrimitive filterPrimitive = (FilterPrimitive) child;
                filterPrimitive.layoutFilter(context, filterLayoutContext);
            } catch (IllegalFilterStateException ignored) {
                // Just carry on doing layout
            }
        }

        LayoutBounds.Data clipHeuristic = filterLayoutContext.resultChannels()
                .get(DefaultFilterChannel.LastResult)
                .resolve(LayoutBounds.ComputeFlags.INITIAL);

        FloatInsets insets = clipHeuristic.clipBoundsEscapeInsets();
        Rectangle2D clipHeuristicBounds = clipHeuristic.bounds()
                .createIntersection(GeometryUtil.grow(graphicsClipBounds, insets));
        GeometryUtil.adjustForAliasing(clipHeuristicBounds);

        return new FilterBounds(elementBounds.boundingBox(), filterRegion, clipHeuristicBounds);
    }

    public @NotNull BufferedImage applyFilter(@NotNull Output output, @NotNull RenderContext context,
            @NotNull FilterInfo filterInfo) {
        ImageProducer producer = filterInfo.blittableImage.image().getSource();

        FilterContext filterContext =
                new FilterContext(filterInfo, filterPrimitiveUnits, colorInterpolation, output.renderingHints());

        Channel sourceChannel = new ImageProducerChannel(producer);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceChannel);
        filterContext.resultChannels().addResult(DefaultFilterChannel.LastResult, sourceChannel);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha,
                () -> new SourceAlphaChannel(sourceChannel.alphaChannel().producer()));

        for (SVGNode child : children()) {
            try {
                FilterPrimitive filterPrimitive = (FilterPrimitive) child;
                filterPrimitive.applyFilter(context, filterContext);
            } catch (IllegalFilterStateException e) {
                // Just carry on applying filters
                LOGGER.log(Level.FINE, "Exception during filter", e);
            }
            // Todo: Respect filterPrimitiveRegion
        }

        Channel result = Objects.requireNonNull(filterContext.getChannel(DefaultFilterChannel.LastResult));
        return result.toBufferedImageNonAliased(context);
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof FilterPrimitive && super.acceptChild(id, node);
    }

    public static final class FilterBounds {
        private final @NotNull Rectangle2D elementBounds;
        private final @NotNull Rectangle2D filterRegion;
        private final @NotNull Rectangle2D effectiveFilterArea;

        private FilterBounds(@NotNull Rectangle2D elementBounds, @NotNull Rectangle2D filterRegion,
                @NotNull Rectangle2D effectiveFilterArea) {
            this.elementBounds = elementBounds;
            this.filterRegion = filterRegion;
            this.effectiveFilterArea = effectiveFilterArea;
        }

        public @NotNull Rectangle2D elementBounds() {
            return elementBounds;
        }

        public @NotNull Rectangle2D filterRegion() {
            return filterRegion;
        }

        public @NotNull Rectangle2D effectiveFilterArea() {
            return effectiveFilterArea;
        }
    }

    public static final class FilterInfo {
        public final int imageWidth;
        public final int imageHeight;

        private final @NotNull FilterBounds filterBounds;
        private final @NotNull BlittableImage blittableImage;
        private final @NotNull Output imageOutput;

        public FilterInfo(@NotNull BlittableImage blittableImage, @NotNull Output imageOutput,
                @NotNull FilterBounds filterBounds) {
            BufferedImage image = blittableImage.image();
            this.imageWidth = image.getWidth();
            this.imageHeight = image.getHeight();
            this.blittableImage = blittableImage;
            this.filterBounds = filterBounds;
            this.imageOutput = imageOutput;
        }

        public @NotNull Rectangle2D imageBounds() {
            return blittableImage.userBoundsInRootSpace();
        }

        public @NotNull Rectangle2D filterRegion() {
            return filterBounds.filterRegion();
        }

        public @NotNull Rectangle2D elementBounds() {
            return filterBounds.elementBounds();
        }

        public @NotNull Output output() {
            return imageOutput;
        }

        public @NotNull Rectangle2D tile() {
            Rectangle2D elementBounds = elementBounds();
            Rectangle2D imageBounds = imageBounds();
            return new Rectangle2D.Double(
                    imageBounds.getX() - elementBounds.getX(),
                    imageBounds.getY() - elementBounds.getY(),
                    imageBounds.getWidth(),
                    imageBounds.getHeight());
        }
    }

    private static final class SourceAlphaChannel extends ImageProducerChannel {
        public SourceAlphaChannel(@NotNull ImageProducer producer) {
            super(producer);
        }

    }
}
