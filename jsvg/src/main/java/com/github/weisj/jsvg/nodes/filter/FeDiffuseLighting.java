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
import java.awt.color.ColorSpace;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {FeDistantLight.class, FePointLight.class, FeSpotLight.class, Animate.class, Set.class}
)
public final class FeDiffuseLighting extends ContainerNode implements FilterPrimitive {
    public static final String TAG = "fediffuselighting";

    private FilterPrimitiveBase filterPrimitiveBase;

    private float surfaceScale;
    private float diffuseConstant;
    private Color lightingColor;
    private double @Nullable [] kernelUnitLength;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        filterPrimitiveBase = new FilterPrimitiveBase(attributeNode);
        surfaceScale = attributeNode.getFloat("surfaceScale", 1);
        diffuseConstant = attributeNode.getNonNegativeFloat("diffuseConstant", 1);
        lightingColor = attributeNode.getColor("lighting-color", Color.WHITE);

        double[] values = attributeNode.getDoubleList("kernelUnitLength");
        if (values.length > 0 && values[0] > 0) {
            double x = values[0];
            double y = values.length > 1 && values[1] > 0 ? values[1] : x;
            kernelUnitLength = new double[] {x, y};
        } else {
            kernelUnitLength = null;
        }
    }

    private @NotNull FilterPrimitiveBase impl() {
        return filterPrimitiveBase;
    }

    @Override
    public @NotNull com.github.weisj.jsvg.geometry.size.Length x() {
        return impl().x;
    }

    @Override
    public @NotNull com.github.weisj.jsvg.geometry.size.Length y() {
        return impl().y;
    }

    @Override
    public @NotNull com.github.weisj.jsvg.geometry.size.Length width() {
        return impl().width;
    }

    @Override
    public @NotNull com.github.weisj.jsvg.geometry.size.Length height() {
        return impl().height;
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof LightSource && super.acceptChild(id, node);
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
        LightSource lightSource = lightSource();
        if (lightSource == null) {
            Filter.FilterInfo info = filterContext.info();
            BufferedImage img = new BufferedImage(info.imageWidth, info.imageHeight, BufferedImage.TYPE_INT_ARGB);
            impl().saveResult(new ImageProducerChannel(img.getSource()), filterContext);
            return;
        }

        ImageFilter lightingFilter = new BufferedImageFilter(
                new DiffuseLightingOp(lightSource, filterContext.info().tile(), colorInterpolation(filterContext)));
        impl().saveResult(impl().inputChannel(filterContext).applyFilter(lightingFilter), filterContext);
    }

    private @Nullable LightSource lightSource() {
        for (SVGNode node : children()) {
            if (node instanceof LightSource) return (LightSource) node;
        }
        return null;
    }

    @Override
    public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
        return impl().colorInterpolation(filterContext);
    }

    private final class DiffuseLightingOp implements BufferedImageOp {

        private final @NotNull LightSource lightSource;
        private final @NotNull Rectangle2D sourceBounds;
        private final @Nullable ColorInterpolation colorInterpolation;

        private DiffuseLightingOp(@NotNull LightSource lightSource, @NotNull Rectangle2D sourceBounds,
                @Nullable ColorInterpolation colorInterpolation) {
            this.lightSource = lightSource;
            this.sourceBounds = sourceBounds;
            this.colorInterpolation = colorInterpolation;
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
            if (src == null) throw new NullPointerException("src image is null");
            if (src == dest) throw new IllegalArgumentException("src image cannot be the same as the dst image");

            BufferedImage result = dest;
            if (result == null) {
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                ColorModel cm = new DirectColorModel(cs, 32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000,
                        false, DataBuffer.TYPE_INT);
                result = createCompatibleDestImage(src, cm);
            }

            WritableRaster raster = result.getRaster();
            int w = raster.getWidth();
            int h = raster.getHeight();
            double scaleX = sourceBounds.getWidth() / w;
            double scaleY = sourceBounds.getHeight() / h;
            double startX = sourceBounds.getX();
            double startY = sourceBounds.getY();

            double pixelStepX = 1;
            double pixelStepY = 1;
            double userStepX = scaleX;
            double userStepY = scaleY;
            if (kernelUnitLength != null) {
                pixelStepX = kernelUnitLength[0] / scaleX;
                pixelStepY = kernelUnitLength[1] / scaleY;
                userStepX = kernelUnitLength[0];
                userStepY = kernelUnitLength[1];
            }

            int[] lightColor = {lightingColor.getRed(), lightingColor.getGreen(), lightingColor.getBlue(), 255};
            boolean linearRGB = colorInterpolation != ColorInterpolation.S_RGB;
            if (linearRGB) ColorUtil.sRGBtoLinearRGBinPlace(lightColor);

            final int[] destPixels = ImageUtil.getINT_RGBA_DataBank(raster);
            final int dstAdjust = ImageUtil.getINT_RGBA_DataAdjust(raster);
            int dp = ImageUtil.getINT_RGBA_DataOffset(raster);

            double userY = startY;
            for (int y = 0; y < h; y++) {
                double userX = startX;
                for (int x = 0, end = dp + w; dp < end; dp++, x++) {
                    double z = heightAt(src, x, y);
                    Normal normal = normalAt(src, x, y, pixelStepX, pixelStepY, userStepX, userStepY);
                    LightSource.Light light = lightSource.lightAt(userX, userY, z);
                    double diffuse = diffuseConstant * light.intensity *
                            Math.max(0, normal.x * light.x + normal.y * light.y + normal.z * light.z);

                    int r = ColorUtil.toRgbRange(lightColor[0] * diffuse);
                    int g = ColorUtil.toRgbRange(lightColor[1] * diffuse);
                    int b = ColorUtil.toRgbRange(lightColor[2] * diffuse);
                    if (linearRGB) {
                        r = ColorUtil.linearRGBtoSRGBBand(r);
                        g = ColorUtil.linearRGBtoSRGBBand(g);
                        b = ColorUtil.linearRGBtoSRGBBand(b);
                    }
                    destPixels[dp] = (0xFF << 24) | (r << 16) | (g << 8) | b;
                    userX += scaleX;
                }
                userY += scaleY;
                dp += dstAdjust;
            }
            return result;
        }

        private double heightAt(@NotNull BufferedImage src, double x, double y) {
            return surfaceScale * alphaAt(src, x, y) / 255.0;
        }

        private double alphaAt(@NotNull BufferedImage src, double x, double y) {
            double clampedX = Math.max(0, Math.min(src.getWidth() - 1, x));
            double clampedY = Math.max(0, Math.min(src.getHeight() - 1, y));
            int x0 = (int) Math.floor(clampedX);
            int y0 = (int) Math.floor(clampedY);
            int x1 = Math.min(src.getWidth() - 1, x0 + 1);
            int y1 = Math.min(src.getHeight() - 1, y0 + 1);
            double tx = clampedX - x0;
            double ty = clampedY - y0;

            double a00 = (src.getRGB(x0, y0) >>> 24) & 0xFF;
            double a10 = (src.getRGB(x1, y0) >>> 24) & 0xFF;
            double a01 = (src.getRGB(x0, y1) >>> 24) & 0xFF;
            double a11 = (src.getRGB(x1, y1) >>> 24) & 0xFF;
            double a0 = a00 + (a10 - a00) * tx;
            double a1 = a01 + (a11 - a01) * tx;
            return a0 + (a1 - a0) * ty;
        }

        private @NotNull Normal normalAt(@NotNull BufferedImage src, int x, int y, double pixelStepX,
                double pixelStepY, double userStepX, double userStepY) {
            double dx = (heightAt(src, x + pixelStepX, y) - heightAt(src, x - pixelStepX, y)) / (2 * userStepX);
            double dy = (heightAt(src, x, y + pixelStepY) - heightAt(src, x, y - pixelStepY)) / (2 * userStepY);

            double nx = -dx;
            double ny = -dy;
            double nz = 1;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            return new Normal(nx / length, ny / length, nz / length);
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }
    }

    private static final class Normal {
        final double x;
        final double y;
        final double z;

        private Normal(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
