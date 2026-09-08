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

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.ColorChannel;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeDisplacementMap extends AbstractFilterPrimitive {
    public static final String TAG = "fedisplacementmap";

    private ColorChannel xChannelSelector;
    private ColorChannel yChannelSelector;

    private float scale;

    private FilterChannelKey inputChannel2;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        xChannelSelector = attributeNode.getEnum("xChannelSelector", ColorChannel.A);
        yChannelSelector = attributeNode.getEnum("yChannelSelector", ColorChannel.A);

        scale = attributeNode.getFloat("scale", 0);

        inputChannel2 = attributeNode.getFilterChannelKey("in2", DefaultFilterChannel.LastResult);
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        // The displacement formula maps channel values from [0, 1] to [-0.5, 0.5],
        // so the maximum lookup offset is half of the configured scale in either direction.
        float growX = Math.abs(scale) / 2f;
        float growY = growX;
        if (filterLayoutContext.primitiveUnits() == UnitType.ObjectBoundingBox) {
            Rectangle2D elementBounds = filterLayoutContext.elementBounds();
            growX *= (float) elementBounds.getWidth();
            growY *= (float) elementBounds.getHeight();
        }
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        LayoutBounds displacementInput = filterLayoutContext.resultChannels().get(inputChannel2);
        LayoutBounds layoutBounds = scale == 0 ? input : input.grow(growX, growY, filterLayoutContext);
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(),
                GeometryUtil.union(input.region(), displacementInput.region()));
        impl().saveLayoutResult(layoutBounds.withRegion(region), filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (scale == 0) {
            impl().noop(filterContext);
            return;
        }
        Channel input = impl().inputChannel(filterContext);
        Channel displacementInput = filterContext.getChannel(inputChannel2);

        double displacementScaleX = scale;
        double displacementScaleY = scale;
        if (filterContext.primitiveUnits() == UnitType.ObjectBoundingBox) {
            Rectangle2D elementBounds = filterContext.info().elementBounds();
            displacementScaleX *= elementBounds.getWidth();
            displacementScaleY *= elementBounds.getHeight();
        }

        AffineTransform displacementTransform = filterContext.info().output().transform();
        displacementTransform.scale(displacementScaleX, displacementScaleY);
        ImageFilter displacementFilter = new BufferedImageFilter(
                new DisplacementOp(displacementInput.pixels(context), displacementTransform));
        impl().saveResult(input.applyFilter(displacementFilter), filterContext);
    }

    private final class DisplacementOp implements BufferedImageOp {

        private final @NotNull PixelProvider displacementChannel;
        private final @NotNull AffineTransform displacementTransform;

        public DisplacementOp(@NotNull PixelProvider displacementChannel,
                @NotNull AffineTransform displacementTransform) {
            this.displacementChannel = displacementChannel;
            this.displacementTransform = displacementTransform;
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
            if (dstCM == null) dstCM = src.getColorModel();
            return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()),
                    dstCM.isAlphaPremultiplied(), null);
        }

        @Override
        public Rectangle2D getBounds2D(@NotNull BufferedImage src) {
            return new Rectangle(0, 0, src.getWidth(), src.getHeight());
        }

        @Override
        public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
            return (Point2D) srcPt.clone();
        }

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dest) {
            if (src == null) {
                throw new NullPointerException("src image is null");
            }
            if (src == dest) {
                throw new IllegalArgumentException("src image cannot be the " +
                        "same as the dst image");
            }

            BufferedImage result = dest;

            if (result == null) {
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                ColorModel cm = new DirectColorModel(cs, 32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000,
                        false, DataBuffer.TYPE_INT);
                result = createCompatibleDestImage(src, cm);
            }

            WritableRaster raster = result.getRaster();

            final int w = raster.getWidth();
            final int h = raster.getHeight();

            final double scaleX = displacementTransform.getScaleX();
            final double scaleY = displacementTransform.getScaleY();
            final double shearX = displacementTransform.getShearX();
            final double shearY = displacementTransform.getShearY();

            Raster sourceRaster = src.getRaster();
            Rectangle sourceRasterBounds = sourceRaster.getBounds();
            ColorModel sourceColorModel = src.getColorModel();
            Object sourcePixel = null;

            final int[] destPixels = ImageUtil.getINT_RGBA_DataBank(raster);
            final int dstAdjust = ImageUtil.getINT_RGBA_DataAdjust(raster);
            int dp = ImageUtil.getINT_RGBA_DataOffset(raster);

            int x;
            int y = 0;
            for (int i = 0; i < h; i++) {
                x = 0;
                for (int end = dp + w; dp < end; dp++) {
                    int displacementRGB = displacementChannel.pixelAt(x, y);
                    double xDisplacement = xChannelSelector.value(displacementRGB) / 255.0 - 0.5f;
                    double yDisplacement = yChannelSelector.value(displacementRGB) / 255.0 - 0.5f;
                    int xDest = (int) (x + scaleX * xDisplacement + shearX * yDisplacement);
                    int yDest = (int) (y + shearY * xDisplacement + scaleY * yDisplacement);
                    if (sourceRasterBounds.contains(xDest, yDest)) {
                        sourcePixel = sourceRaster.getDataElements(xDest, yDest, sourcePixel);
                        destPixels[dp] = sourceColorModel.getRGB(sourcePixel);
                    } else {
                        destPixels[dp] = 0;
                    }
                    x++;
                }
                dp += dstAdjust;
                y++;
            }

            return result;
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }
    }
}
