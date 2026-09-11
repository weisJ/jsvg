/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds.CoversWholeRegion;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeTile extends AbstractFilterPrimitive {
    public static final String TAG = "fetile";
    private static final int MAX_DIRECT_COPIES = 64;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(), filterLayoutContext.filterRegion());
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        boolean repeats = !input.region().isEmpty() && !input.region().contains(region);
        LayoutBounds layout = input.withRegion(region, repeats ? CoversWholeRegion.YES : CoversWholeRegion.NO);
        if (repeats) {
            // Retain the original tile and the upstream samples needed to produce it.
            // TODO: Allocation could avoid the unused gap between a distant input and the visible copies.
            FloatInsets insets = GeometryUtil.max(input.clipBoundsEscapeInsets(),
                    GeometryUtil.overhangInsets(filterLayoutContext.clipBounds(), input.bounds()));
            layout = new LayoutBounds(layout.bounds(), insets, region);
        }
        impl().saveLayoutResult(layout, filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        Filter.FilterInfo info = filterContext.info();
        Rectangle2D tileRegion = filterContext.layout(impl().inputChannelKey()).region();
        Rectangle2D primitiveRegion = filterContext.primitiveRegion(impl());
        if (tileRegion.contains(primitiveRegion)) {
            impl().noop(filterContext);
            return;
        }

        if (tileRegion.isEmpty() || primitiveRegion.isEmpty()) {
            impl().saveResult(new ConstantColorChannel(info.imageWidth, info.imageHeight, 0), filterContext);
            return;
        }

        Channel inputChannel = impl().inputChannel(filterContext);
        if (inputChannel instanceof ConstantColorChannel) {
            impl().saveResult(inputChannel, filterContext);
            return;
        }
        BufferedImage output = ImageUtil.createCompatibleTransparentImage(info.imageWidth, info.imageHeight);
        Image input = inputChannel.toImage(context);
        AffineTransform userToPixel = info.output().transform();
        Graphics2D graphics = output.createGraphics();
        try {
            if (filterContext.renderingHints() != null) {
                graphics.setRenderingHints(filterContext.renderingHints());
            }
            // Axis-aligned tiles (including quarter turns) can share the input raster.
            Rectangle2D pixelRegion = GeometryUtil.containingBoundsAfterTransform(userToPixel, tileRegion);
            Rectangle pixelBounds = pixelRegion.getBounds();
            Rectangle imageBounds = new Rectangle(0, 0, info.imageWidth, info.imageHeight);
            boolean axisAligned = GeometryUtil.isAxisAligned(userToPixel);
            if (axisAligned
                    && pixelRegion.equals(pixelBounds) && input instanceof BufferedImage
                    && imageBounds.contains(pixelBounds)) {
                BufferedImage tile = ((BufferedImage) input).getSubimage(
                        pixelBounds.x, pixelBounds.y, pixelBounds.width, pixelBounds.height);
                graphics.setPaint(new TexturePaint(tile, pixelRegion));
                graphics.fillRect(0, 0, imageBounds.width, imageBounds.height);
            } else {
                Rectangle2D outputRegion = primitiveRegion.createIntersection(info.imageBounds());
                if (!paintCopies(graphics, input, tileRegion, outputRegion, userToPixel, context)) {
                    paintTile(graphics, input, tileRegion, outputRegion, info, userToPixel, context);
                }
            }
        } finally {
            graphics.dispose();
        }

        impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
    }

    private static boolean paintCopies(@NotNull Graphics2D graphics, @NotNull Image input,
            @NotNull Rectangle2D tileRegion, @NotNull Rectangle2D outputRegion,
            @NotNull AffineTransform userToPixel, @NotNull RenderContext context) {
        if (outputRegion.isEmpty()) return true;
        double startX = Math.floor((outputRegion.getX() - tileRegion.getX()) / tileRegion.getWidth());
        double endX = Math.ceil((outputRegion.getMaxX() - tileRegion.getX()) / tileRegion.getWidth());
        double startY = Math.floor((outputRegion.getY() - tileRegion.getY()) / tileRegion.getHeight());
        double endY = Math.ceil((outputRegion.getMaxY() - tileRegion.getY()) / tileRegion.getHeight());
        // Preserve the already transformed raster when only a few copies are needed.
        // Dense and subpixel tiles use TexturePaint instead of an unbounded number of image draws.
        if ((endX - startX) * (endY - startY) > MAX_DIRECT_COPIES) return false;
        Point2D.Double offset = new Point2D.Double();
        AffineTransform translation = new AffineTransform();
        for (int y = 0; y < (int) (endY - startY); y++) {
            for (int x = 0; x < (int) (endX - startX); x++) {
                offset.setLocation((startX + x) * tileRegion.getWidth(), (startY + y) * tileRegion.getHeight());
                userToPixel.deltaTransform(offset, offset);
                translation.setToTranslation(offset.x, offset.y);
                graphics.drawImage(input, translation, context.platformSupport().imageObserver());
            }
        }
        return true;
    }

    private static void paintTile(@NotNull Graphics2D graphics, @NotNull Image input,
            @NotNull Rectangle2D region, @NotNull Rectangle2D outputRegion, @NotNull Filter.FilterInfo info,
            @NotNull AffineTransform userToPixel, @NotNull RenderContext context) {
        // Crop only dimensions larger than the backing image. The other dimensions can repeat in one
        // TexturePaint operation, including subpixel periods which would require unbounded copy loops.
        Rectangle2D imageBounds = info.imageBounds();
        boolean repeatX = region.getWidth() <= imageBounds.getWidth();
        boolean repeatY = region.getHeight() <= imageBounds.getHeight();
        double left = repeatX ? region.getX() : Math.max(region.getX(), imageBounds.getX());
        double top = repeatY ? region.getY() : Math.max(region.getY(), imageBounds.getY());
        double right = repeatX ? region.getMaxX() : Math.min(region.getMaxX(), imageBounds.getMaxX());
        double bottom = repeatY ? region.getMaxY() : Math.min(region.getMaxY(), imageBounds.getMaxY());
        Rectangle2D tileRegion = new Rectangle2D.Double(left, top, right - left, bottom - top);
        if (tileRegion.isEmpty() || outputRegion.isEmpty()) return;

        BufferedImage tile =
                createTileImage(input, tileRegion, info, userToPixel, context, graphics.getRenderingHints());
        if (tile == null) return;

        graphics.transform(userToPixel);
        paintTextureCopies(graphics, tile, region, tileRegion, outputRegion, repeatX, repeatY);
    }

    private static @Nullable BufferedImage createTileImage(@NotNull Image input, @NotNull Rectangle2D tileRegion,
            @NotNull Filter.FilterInfo info, @NotNull AffineTransform userToPixel, @NotNull RenderContext context,
            @NotNull RenderingHints renderingHints) {
        int width = Math.max(1, Math.min(info.imageWidth, (int) Math.ceil(tileRegion.getWidth()
                * GeometryUtil.scaleXOfTransform(userToPixel))));
        int height = Math.max(1, Math.min(info.imageHeight, (int) Math.ceil(tileRegion.getHeight()
                * GeometryUtil.scaleYOfTransform(userToPixel))));
        AffineTransform pixelToUser = GeometryUtil.createInverse(userToPixel);
        if (input instanceof BufferedImage) {
            // Do not interpolate against transparent pixels beyond the tile's raster edges.
            Rectangle crop = GeometryUtil.containingBoundsAfterTransform(userToPixel, tileRegion).getBounds()
                    .intersection(new Rectangle(0, 0, info.imageWidth, info.imageHeight));
            if (crop.isEmpty()) return null;
            input = ((BufferedImage) input).getSubimage(crop.x, crop.y, crop.width, crop.height);
            pixelToUser.translate(crop.x, crop.y);
        }
        BufferedImage tile = ImageUtil.createCompatibleTransparentImage(width, height);
        Graphics2D tileGraphics = tile.createGraphics();
        try {
            tileGraphics.setRenderingHints(renderingHints);
            tileGraphics.scale(width / tileRegion.getWidth(), height / tileRegion.getHeight());
            tileGraphics.translate(-tileRegion.getX(), -tileRegion.getY());
            tileGraphics.drawImage(input, pixelToUser, context.platformSupport().imageObserver());
        } finally {
            tileGraphics.dispose();
        }
        return tile;
    }

    private static void paintTextureCopies(@NotNull Graphics2D graphics, @NotNull BufferedImage tile,
            @NotNull Rectangle2D region, @NotNull Rectangle2D tileRegion, @NotNull Rectangle2D outputRegion,
            boolean repeatX, boolean repeatY) {
        double startX = repeatX ? 0 : Math.floor((outputRegion.getX() - tileRegion.getMaxX()) / region.getWidth()) + 1;
        double endX = repeatX ? 1 : Math.ceil((outputRegion.getMaxX() - tileRegion.getX()) / region.getWidth());
        double startY = repeatY ? 0 : Math.floor((outputRegion.getY() - tileRegion.getMaxY()) / region.getHeight()) + 1;
        double endY = repeatY ? 1 : Math.ceil((outputRegion.getMaxY() - tileRegion.getY()) / region.getHeight());
        Rectangle2D.Double fillRegion = new Rectangle2D.Double();
        for (int y = 0; y < (int) (endY - startY); y++) {
            for (int x = 0; x < (int) (endX - startX); x++) {
                Rectangle2D anchor = new Rectangle2D.Double(tileRegion.getX() + (startX + x) * region.getWidth(),
                        tileRegion.getY() + (startY + y) * region.getHeight(), tileRegion.getWidth(),
                        tileRegion.getHeight());
                graphics.setPaint(new TexturePaint(tile, anchor));
                fillRegion.setRect(outputRegion);
                if (!repeatX) {
                    fillRegion.x = anchor.getX();
                    fillRegion.width = anchor.getWidth();
                }
                if (!repeatY) {
                    fillRegion.y = anchor.getY();
                    fillRegion.height = anchor.getHeight();
                }
                graphics.fill(fillRegion);
            }
        }
    }

}
