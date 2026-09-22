/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.OffscreenImage;
import com.github.weisj.jsvg.util.TransformedBlittableImage;

@ElementCategories({/* None */})
@PermittedContent(
    categories = {Category.Descriptive, Category.FilterPrimitive},
    anyOf = {Animate.class, Set.class}
)
public final class Filter extends ContainerNode {
    private static final Logger LOGGER = LogFactory.createLogger(Filter.class);
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

    public @Nullable FilterLayout createFilterLayout(@Nullable Output output, @NotNull RenderContext context,
            @NotNull ElementBounds elementBounds) {
        Rectangle2D.Double filterRegion = filterUnits.computeViewBounds(
                context.measureContext(), elementBounds.boundingBox(), x, y, width, height);
        AffineTransform transform;
        if (output != null) {
            transform = output.transform();
        } else {
            transform = new AffineTransform(context.rootTransform());
            transform.concatenate(context.userSpaceTransform());
        }

        if (transform.getDeterminant() == 0) return null;

        Rectangle2D graphicsClipBounds = output != null
                ? output.clipBounds()
                : NO_CLIP_BOUNDS.getBounds2D();

        FilterLayoutContext filterLayoutContext =
                new FilterLayoutContext(filterPrimitiveUnits, elementBounds.boundingBox(), graphicsClipBounds,
                        filterRegion, context.measureContext(), transform);

        boolean alignedBuffer = requiresAlignedBuffer(filterLayoutContext);
        if (alignedBuffer) {
            // Preserve both axis scales of the complete transform, including the output/device scale.
            transform = AffineTransform.getScaleInstance(
                    GeometryUtil.scaleXOfTransform(transform), GeometryUtil.scaleYOfTransform(transform));
            // Final blitting can use bicubic interpolation, which needs two buffer pixels outside the clip.
            graphicsClipBounds = GeometryUtil.grow(graphicsClipBounds, new FloatInsets(
                    (float) (2 / transform.getScaleY()), (float) (2 / transform.getScaleX()),
                    (float) (2 / transform.getScaleY()), (float) (2 / transform.getScaleX())));
            filterLayoutContext = new FilterLayoutContext(filterPrimitiveUnits, elementBounds.boundingBox(),
                    graphicsClipBounds, filterRegion, context.measureContext(), transform);
        }

        Rectangle2D effectiveFilterRegion = filterRegion.createIntersection(graphicsClipBounds);

        if (effectiveFilterRegion.isEmpty()) return null;

        // Sampling primitives may need source pixels outside the repaint clip.
        LayoutBounds elementLayoutBounds = LayoutBounds.createInitial(elementBounds.sourceBox(), filterRegion);
        filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, elementLayoutBounds);
        filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha, elementLayoutBounds);
        filterLayoutContext.resultChannels().addAlias(DefaultFilterChannel.LastResult,
                DefaultFilterChannel.SourceGraphic);

        for (SVGNode child : children()) {
            try {
                FilterPrimitive filterPrimitive = (FilterPrimitive) child;
                layoutPrimitive(filterPrimitive, context, filterLayoutContext);
            } catch (IllegalFilterStateException ignored) {
                // Just carry on doing layout
            }
        }

        ChannelStorage<LayoutBounds> layouts = filterLayoutContext.resultChannels();
        LayoutBounds clipHeuristic = layouts.get(DefaultFilterChannel.LastResult);

        FloatInsets insets = clipHeuristic.clipBoundsEscapeInsets();
        Rectangle2D clipHeuristicBounds = clipHeuristic.bounds()
                .createIntersection(GeometryUtil.grow(graphicsClipBounds, insets));
        GeometryUtil.adjustForAliasing(clipHeuristicBounds);

        return new FilterLayout(elementBounds.boundingBox(), filterRegion, clipHeuristicBounds, layouts,
                transform, alignedBuffer);
    }

    private boolean requiresAlignedBuffer(@NotNull FilterLayoutContext context) {
        if (GeometryUtil.isAxisAligned(context.transform())) return false;
        for (SVGNode child : children()) {
            FilterPrimitive primitive = (FilterPrimitive) child;
            if (primitive.isValid() && primitive.requiresAlignedBuffer(context)) return true;
        }
        return false;
    }

    public @NotNull BufferedImage applyFilter(@NotNull Output output, @NotNull RenderContext context,
            @NotNull FilterInfo filterInfo) {
        ImageProducer producer = filterInfo.blittableImage.image().getSource();

        FilterContext filterContext =
                new FilterContext(filterInfo, filterPrimitiveUnits, colorInterpolation, output.renderingHints());

        Channel sourceChannel = new ImageProducerChannel(producer).clip(filterInfo.filterRegion(), filterContext);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceChannel);
        filterContext.resultChannels().addAlias(DefaultFilterChannel.LastResult, DefaultFilterChannel.SourceGraphic);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha,
                () -> new SourceAlphaChannel(sourceChannel.alphaChannel().producer()));

        // TODO: Track if a primitive is actually used and skip applying unused primitives.
        for (SVGNode child : children()) {
            try {
                FilterPrimitive filterPrimitive = (FilterPrimitive) child;
                applyPrimitive(filterPrimitive, context, filterContext);
            } catch (IllegalFilterStateException e) {
                // Just carry on applying filters
                LOGGER.log(Level.INFO, "Exception during filter", e);
            }
        }

        Channel result = Objects.requireNonNull(filterContext.getChannel(DefaultFilterChannel.LastResult));
        return result.toBufferedImageNonAliased(context);
    }

    static void layoutPrimitive(@NotNull FilterPrimitive primitive, @NotNull RenderContext context,
            @NotNull FilterLayoutContext filterLayoutContext) {
        if (!primitive.isValid()) return;
        primitive.layoutFilter(context, filterLayoutContext);
    }

    static void applyPrimitive(@NotNull FilterPrimitive primitive, @NotNull RenderContext context,
            @NotNull FilterContext filterContext) {
        if (!primitive.isValid()) return;
        primitive.applyFilter(context, filterContext);
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof FilterPrimitive && super.acceptChild(id, node);
    }

    public static final class FilterLayout {
        private final @NotNull Rectangle2D elementBounds;
        private final @NotNull Rectangle2D filterRegion;
        private final @NotNull Rectangle2D effectiveFilterArea;
        private final @NotNull ChannelStorage<LayoutBounds> layouts;
        private final @NotNull AffineTransform transform;
        private final boolean alignedBuffer;

        private FilterLayout(@NotNull Rectangle2D elementBounds, @NotNull Rectangle2D filterRegion,
                @NotNull Rectangle2D effectiveFilterArea,
                @NotNull ChannelStorage<LayoutBounds> layouts, @NotNull AffineTransform transform,
                boolean alignedBuffer) {
            this.elementBounds = elementBounds;
            this.filterRegion = filterRegion;
            this.effectiveFilterArea = effectiveFilterArea;
            this.layouts = layouts;
            this.transform = transform;
            this.alignedBuffer = alignedBuffer;
        }

        public @Nullable OffscreenImage createImage(@NotNull BlittableImage.BufferSurfaceSupplier supplier,
                @NotNull RenderContext context, @NotNull RenderContext imageContext, @NotNull Rectangle2D bounds) {
            if (alignedBuffer) {
                return TransformedBlittableImage.create(supplier, context, imageContext,
                        bounds, effectiveFilterArea, transform);
            }
            return BlittableImage.create(supplier, context, effectiveFilterArea, bounds, elementBounds,
                    UnitType.UserSpaceOnUse, imageContext);
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

        @NotNull
        LayoutBounds layout(@NotNull FilterChannelKey key) {
            return layouts.get(key);
        }
    }

    public static final class FilterInfo {
        public final int imageWidth;
        public final int imageHeight;

        private final @NotNull FilterLayout filterLayout;
        private final @NotNull OffscreenImage blittableImage;
        private final @NotNull Output imageOutput;

        public FilterInfo(@NotNull OffscreenImage blittableImage, @NotNull Output imageOutput,
                @NotNull FilterLayout filterLayout) {
            BufferedImage image = blittableImage.image();
            this.imageWidth = image.getWidth();
            this.imageHeight = image.getHeight();
            this.blittableImage = blittableImage;
            this.filterLayout = filterLayout;
            this.imageOutput = imageOutput;
        }

        public @NotNull Rectangle2D imageBounds() {
            return blittableImage.clippedUserBounds();
        }

        public @NotNull Rectangle2D filterRegion() {
            return filterLayout.filterRegion();
        }

        public @NotNull Rectangle2D elementBounds() {
            return filterLayout.elementBounds();
        }

        @NotNull
        LayoutBounds layout(@NotNull FilterChannelKey key) {
            return filterLayout.layout(key);
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
