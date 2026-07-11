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
        BufferedImage input = impl().inputChannel(filterContext).toBufferedImageNonAliased(context);
        Filter.FilterInfo info = filterContext.info();
        BufferedImage output = new BufferedImage(info.imageWidth, info.imageHeight, BufferedImage.TYPE_INT_ARGB);

        Rectangle2D tileRegion = impl().layoutInput(filterContext.layoutContext())
                .resolve(LayoutBounds.ComputeFlags.INITIAL)
                .bounds();
        if (tileRegion.isEmpty()) {
            impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
            return;
        }

        Rectangle2D primitiveRegion =
                filterContext.layoutContext().filterPrimitiveRegion(context.measureContext(), this);
        Rectangle2D imageBounds = info.imageBounds();
        double scaleX = imageBounds.getWidth() / output.getWidth();
        double scaleY = imageBounds.getHeight() / output.getHeight();

        for (int y = 0; y < output.getHeight(); y++) {
            double userY = imageBounds.getY() + y * scaleY;
            if (userY < primitiveRegion.getY() || userY >= primitiveRegion.getMaxY()) continue;
            double tiledY = tileRegion.getY() + positiveModulo(userY - tileRegion.getY(), tileRegion.getHeight());
            int sourceY = userToPixel(tiledY, imageBounds.getY(), scaleY, input.getHeight());

            for (int x = 0; x < output.getWidth(); x++) {
                double userX = imageBounds.getX() + x * scaleX;
                if (userX < primitiveRegion.getX() || userX >= primitiveRegion.getMaxX()) continue;
                double tiledX = tileRegion.getX()
                        + positiveModulo(userX - tileRegion.getX(), tileRegion.getWidth());
                int sourceX = userToPixel(tiledX, imageBounds.getX(), scaleX, input.getWidth());
                output.setRGB(x, y, input.getRGB(sourceX, sourceY));
            }
        }

        impl().saveResult(new ImageProducerChannel(output.getSource()), filterContext);
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }

    private static int userToPixel(double value, double origin, double scale, int size) {
        int pixel = (int) ((value - origin) / scale);
        return Math.min(Math.max(pixel, 0), size - 1);
    }
}
