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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.noise.PerlinTurbulence;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;
import com.github.weisj.jsvg.util.ImageUtil;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeTurbulence extends AbstractFilterPrimitive {
    public static final String TAG = "feturbulence";

    public enum Type {
        fractalNoise,
        Turbulence
    }

    private float seed;
    private float[] baseFrequency;
    private int numOctaves;

    private Type type;
    private boolean stitchTiles;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        seed = attributeNode.getFloat("seed", 0);

        baseFrequency = attributeNode.getFloatList("baseFrequency");
        if (baseFrequency.length == 0) baseFrequency = new float[] {0};

        numOctaves = attributeNode.getInt("numOctaves", 1);
        // beyond 8 octaves there is no significant contribution
        // to the output pixel (contribution is halved for each
        // octave so after 8 we are contributing less than half a
        // code value _at_best_).
        numOctaves = Math.min(numOctaves, 8);

        type = attributeNode.getEnum("type", Type.fractalNoise);
        stitchTiles = "stitch".equals(attributeNode.getValue("stitchTiles"));
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(), filterLayoutContext.filterRegion());
        impl().saveLayoutResult(LayoutBounds.createInitial(region, region), filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        Filter.FilterInfo info = filterContext.info();
        AffineTransform primitiveTransform = filterContext.primitiveUnits()
                .applyToTransform(info.output().transform(), info.elementBounds());
        Rectangle2D region = filterContext.primitiveRegion(impl());
        if (stitchTiles && filterContext.primitiveUnits() == UnitType.ObjectBoundingBox) {
            Rectangle2D bounds = info.elementBounds();
            region = new Rectangle2D.Double(
                    (region.getX() - bounds.getX()) / bounds.getWidth(),
                    (region.getY() - bounds.getY()) / bounds.getHeight(),
                    region.getWidth() / bounds.getWidth(),
                    region.getHeight() / bounds.getHeight());
        }
        double xFrequency = baseFrequency[0];
        double yFrequency = baseFrequency[Math.min(baseFrequency.length - 1, 1)];
        Rectangle2D.Double tileRegion = stitchTiles && !region.isEmpty()
                ? GeometryUtil.toDoubleRectangle(region)
                : null;
        Channel turbulenceChannel = new TurbulenceChannel(
                GeometryUtil.createInverse(primitiveTransform),
                tileRegion,
                info.imageWidth, info.imageHeight,
                type, colorInterpolation(filterContext),
                new PerlinTurbulence((int) seed, numOctaves, xFrequency, yFrequency));
        impl().saveResult(turbulenceChannel, filterContext);
    }

    public static final class TurbulenceChannel implements Channel, PixelProvider {

        private final PerlinTurbulence perlinTurbulence;
        private final double[] channels = new double[4];
        private final int imageWidth;
        private final int imageHeight;
        private final Type type;
        private final boolean linearRGB;
        private final @NotNull AffineTransform imageToPrimitive;
        private final @Nullable Rectangle2D.Double tileRegion;
        private final PerlinTurbulence.@Nullable StitchInfo stitchInfo;
        private BufferedImage bufferedImage;

        private final double[] coordinateBuffer = new double[2];

        public TurbulenceChannel(@NotNull AffineTransform imageToPrimitive, @Nullable Rectangle2D.Double tileRegion,
                int imageWidth, int imageHeight, Type type, @NotNull ColorInterpolation colorInterpolation,
                @NotNull PerlinTurbulence perlinTurbulence) {
            this.imageToPrimitive = imageToPrimitive;
            this.tileRegion = tileRegion;
            this.stitchInfo = tileRegion != null ? new PerlinTurbulence.StitchInfo() : null;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.type = type;
            this.linearRGB = colorInterpolation == ColorInterpolation.LinearRGB;
            this.perlinTurbulence = perlinTurbulence;
        }

        private @NotNull BufferedImage ensureImageBackingStore() {
            if (bufferedImage == null) {
                bufferedImage = ImageUtil.createCompatibleTransparentImage(imageWidth, imageHeight);
                WritableRaster dest = bufferedImage.getRaster();

                final int w = dest.getWidth();
                final int h = dest.getHeight();

                final int[] destPixels = ImageUtil.getINT_RGBA_DataBank(dest);
                final int dstAdjust = ImageUtil.getINT_RGBA_DataAdjust(dest);
                int dp = ImageUtil.getINT_RGBA_DataOffset(dest);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++, dp++) {
                        destPixels[dp] = pixelAt(x, y);
                    }
                    dp += dstAdjust;
                }
            }
            return bufferedImage;
        }

        @Override
        public @NotNull ImageProducer producer() {
            return ensureImageBackingStore().getSource();
        }

        @Override
        public @NotNull BufferedImage toBufferedImageNonAliased(@NotNull RenderContext context) {
            BufferedImage img = ensureImageBackingStore();
            ColorModel cm = img.getColorModel();
            WritableRaster raster = img.copyData(null);
            return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        }

        @Override
        public @NotNull Channel applyFilter(@NotNull ImageFilter filter) {
            return new ImageProducerChannel(new FilteredImageSource(producer(), filter));
        }

        @Override
        public @NotNull PixelProvider pixels(@NotNull RenderContext context) {
            return this;
        }

        @Override
        public int pixelAt(double x, double y) {
            coordinateBuffer[0] = x;
            coordinateBuffer[1] = y;
            imageToPrimitive.transform(coordinateBuffer, 0, coordinateBuffer, 0, 1);
            double primitiveX = coordinateBuffer[0];
            double primitiveY = coordinateBuffer[1];
            perlinTurbulence.turbulence(channels, primitiveX, primitiveY,
                    type == Type.fractalNoise, stitchInfo, tileRegion);
            int argb = channelsToRGB(channels);
            // Channel consumers receive straight sRGB, including direct pixel sampling.
            return linearRGB ? ColorUtil.linearRGBtoSRGB(argb) : argb;
        }

        private static int channelsToRGB(double[] channels) {
            int j;
            int i = (int) channels[0];
            if ((i & 0xFFFFFF00) == 0) {
                j = i << 16;
            } else {
                j = ((i & 0x80000000) != 0) ? 0 : 0xFF0000;
            }

            i = (int) channels[1];
            if ((i & 0xFFFFFF00) == 0) {
                j |= i << 8;
            } else {
                j |= ((i & 0x80000000) != 0) ? 0 : 0xFF00;
            }

            i = (int) channels[2];
            if ((i & 0xFFFFFF00) == 0) {
                j |= i;
            } else {
                j |= ((i & 0x80000000) != 0) ? 0 : 0xFF;
            }

            i = (int) channels[3];
            if ((i & 0xFFFFFF00) == 0) {
                j |= i << 24;
            } else {
                j |= ((i & 0x80000000) != 0) ? 0 : 0xFF000000;
            }

            return j;
        }
    }
}
