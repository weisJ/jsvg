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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
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

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds layoutBounds = new LayoutBounds(
                filterLayoutContext.filterPrimitiveRegion(context.measureContext(), this),
                new FloatInsets());
        impl().saveLayoutResult(layoutBounds, filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        Image input = impl().inputChannel(filterContext).toImage(context);
        Filter.FilterInfo info = filterContext.info();
        BufferedImage output = ImageUtil.createCompatibleTransparentImage(info.imageWidth, info.imageHeight);

        Rectangle2D primitiveRegion = filterContext.outputLayout().bounds();
        Rectangle2D tileRegion = filterContext.inputLayout()
                .bounds()
                .createIntersection(primitiveRegion);
        if (tileRegion.isEmpty()) {
            impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
            return;
        }

        Rectangle2D imageBounds = info.imageBounds();
        Rectangle tileArea = userToPixelRect(tileRegion, imageBounds, info.imageWidth, info.imageHeight);
        Rectangle primitiveArea = userToPixelRect(primitiveRegion, imageBounds, info.imageWidth, info.imageHeight);
        if (tileArea.isEmpty() || primitiveArea.isEmpty()) {
            impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
            return;
        }

        Graphics2D graphics = output.createGraphics();
        if (filterContext.renderingHints() != null) graphics.setRenderingHints(filterContext.renderingHints());
        graphics.clip(primitiveArea);
        int startX = tileArea.x + Math.floorDiv(primitiveArea.x - tileArea.x, tileArea.width) * tileArea.width;
        int startY = tileArea.y + Math.floorDiv(primitiveArea.y - tileArea.y, tileArea.height) * tileArea.height;
        for (int y = startY; y < primitiveArea.y + primitiveArea.height; y += tileArea.height) {
            for (int x = startX; x < primitiveArea.x + primitiveArea.width; x += tileArea.width) {
                graphics.drawImage(input,
                        x, y, x + tileArea.width, y + tileArea.height,
                        tileArea.x, tileArea.y, tileArea.x + tileArea.width, tileArea.y + tileArea.height,
                        context.platformSupport().imageObserver());
            }
        }
        graphics.dispose();

        impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
    }

    private static @NotNull Rectangle userToPixelRect(@NotNull Rectangle2D userRect, @NotNull Rectangle2D imageBounds,
            int imageWidth, int imageHeight) {
        double scaleX = imageWidth / imageBounds.getWidth();
        double scaleY = imageHeight / imageBounds.getHeight();
        int x = (int) Math.floor((userRect.getX() - imageBounds.getX()) * scaleX);
        int y = (int) Math.floor((userRect.getY() - imageBounds.getY()) * scaleY);
        int maxX = (int) Math.ceil((userRect.getMaxX() - imageBounds.getX()) * scaleX);
        int maxY = (int) Math.ceil((userRect.getMaxY() - imageBounds.getY()) * scaleY);
        return new Rectangle(x, y, maxX - x, maxY - y);
    }
}
