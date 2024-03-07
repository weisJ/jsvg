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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.github.weisj.jsvg.renderer.ElementBounds;
import com.github.weisj.jsvg.renderer.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories({/* None */})
@PermittedContent(
    categories = {Category.Descriptive, Category.FilterPrimitive},
    anyOf = {Animate.class, Set.class}
)
public final class Filter extends ContainerNode {
    private static final Logger LOGGER = Logger.getLogger(Filter.class.getName());
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

    public @Nullable FilterInfo createFilterInfo(@NotNull Output output, @NotNull RenderContext context,
            @NotNull ElementBounds elementBounds) {
        Rectangle2D.Double filterRegion = filterUnits.computeViewBounds(
                context.measureContext(), elementBounds.boundingBox(), x, y, width, height);
        Rectangle2D graphicsClipBounds = output.clipBounds();

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

        RenderContext imageContext = context.deriveForSurface();

        BlittableImage blitImage = BlittableImage.create(
                ImageUtil::createCompatibleTransparentImage, context, clipHeuristicBounds,
                filterRegion, elementBounds.boundingBox(), UnitType.UserSpaceOnUse, imageContext);

        if (blitImage == null) return null;

        return new FilterInfo(output, blitImage, elementBounds.boundingBox(), filterRegion);
    }

    public void applyFilter(@NotNull Output output, @NotNull RenderContext context, @NotNull FilterInfo filterInfo) {
        ImageProducer producer = filterInfo.blittableImage.image().getSource();

        FilterContext filterContext = new FilterContext(filterInfo, filterPrimitiveUnits, output.renderingHints());

        Channel sourceChannel = new ImageProducerChannel(producer);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceChannel);
        filterContext.resultChannels().addResult(DefaultFilterChannel.LastResult, sourceChannel);
        filterContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha,
                () -> new SourceAlphaChannel(sourceChannel.applyFilter(new AlphaImageFilter()).producer()));

        for (SVGNode child : children()) {
            try {
                FilterPrimitive filterPrimitive = (FilterPrimitive) child;
                filterPrimitive.applyFilter(context, filterContext);
            } catch (IllegalFilterStateException e) {
                // Just carry on applying filters
                LOGGER.log(Level.INFO, "Exception during filter", e);
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
        private final @NotNull Rectangle2D filterRegion;
        private final @NotNull Output imageOutput;
        private final @NotNull BlittableImage blittableImage;

        private ImageProducer producer;

        private FilterInfo(@NotNull Output output, @NotNull BlittableImage blittableImage,
                @NotNull Rectangle2D elementBounds, @NotNull Rectangle2D filterRegion) {
            this.blittableImage = blittableImage;
            this.elementBounds = elementBounds;
            this.filterRegion = filterRegion;

            BufferedImage image = blittableImage.image();

            this.imageWidth = image.getWidth();
            this.imageHeight = image.getHeight();

            Graphics2D g = blittableImage.createGraphics();
            g.setRenderingHints(output.renderingHints());
            this.imageOutput = new Graphics2DOutput(g);
        }

        public @NotNull Rectangle2D imageBounds() {
            return blittableImage.userBoundsInRootSpace();
        }

        public @NotNull Rectangle2D elementBounds() {
            return elementBounds;
        }

        public @NotNull Output output() {
            return imageOutput;
        }

        public @NotNull RenderContext context() {
            return blittableImage.context();
        }

        public @NotNull Rectangle2D tile() {
            Rectangle2D imageBounds = imageBounds();
            return new Rectangle2D.Double(
                    imageBounds.getX() - elementBounds.getX(),
                    imageBounds.getY() - elementBounds.getY(),
                    imageBounds.getWidth(),
                    imageBounds.getHeight());
        }

        public void blitImage(@NotNull Output output, @NotNull RenderContext context) {
            if (DEBUG) {
                blittableImage.debug(output, false);
            }
            output.applyClip(filterRegion);
            blittableImage.prepareForBlitting(output);
            output.drawImage(
                    context.platformSupport().createImage(producer), context.platformSupport().imageObserver());
        }

        public void close() {
            imageOutput.dispose();
        }
    }

    private static final class AlphaImageFilter extends RGBImageFilter {
        private final ColorModel model = ColorModel.getRGBdefault();

        @Override
        public int filterRGB(int x, int y, int rgb) {
            return model.getAlpha(rgb) << 24;
        }
    }

    private static final class SourceAlphaChannel extends ImageProducerChannel {
        public SourceAlphaChannel(@NotNull ImageProducer producer) {
            super(producer);
        }

        @Override
        public boolean isDefaultChannel(DefaultFilterChannel channel) {
            return channel == DefaultFilterChannel.SourceAlpha;
        }
    }
}
