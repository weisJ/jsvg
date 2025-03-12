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

import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.noise.PerlinTurbulence;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.impl.RenderContext;
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
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        impl().saveLayoutResult(
                new LayoutBounds(
                        filterLayoutContext.filterPrimitiveRegion(context.measureContext(), this),
                        new FloatInsets()),
                filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        Filter.FilterInfo info = filterContext.info();
        Channel turbulenceChannel =
                new TurbulenceChannel(info.imageBounds(), info.imageWidth, info.imageHeight, seed, numOctaves,
                        baseFrequency[0], baseFrequency.length > 1 ? baseFrequency[1] : baseFrequency[0], type);
        impl().saveResult(turbulenceChannel, filterContext);
    }

    public static final class TurbulenceChannel implements Channel, PixelProvider {

        private final PerlinTurbulence perlinTurbulence;
        private final double[] channels = new double[4];
        private final int imageWidth;
        private final int imageHeight;
        private final Type type;
        private final Rectangle2D tileBounds;
        private BufferedImage bufferedImage;

        public TurbulenceChannel(@NotNull Rectangle2D tileBounds, int imageWidth, int imageHeight,
                float seed, int octaves, double xFrequency, double yFrequency, Type type) {
            this.tileBounds = tileBounds;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.type = type;
            this.perlinTurbulence = new PerlinTurbulence((int) seed, octaves, xFrequency, yFrequency);
        }

        private @NotNull BufferedImage ensureImageBackingStore() {
            if (bufferedImage == null) {
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
                ColorModel cm = new DirectColorModel(cs, 32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000,
                        false, DataBuffer.TYPE_INT);
                WritableRaster dest = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
                bufferedImage = new BufferedImage(cm, dest, false, null);

                final int w = dest.getWidth();
                final int h = dest.getHeight();

                final double scaleX = tileBounds.getWidth() / (double) w;
                final double scaleY = tileBounds.getHeight() / (double) h;

                final double startX = tileBounds.getX();
                final double startY = tileBounds.getY();

                boolean fractalNoise = type == Type.fractalNoise;

                final int[] destPixels = ImageUtil.getINT_RGBA_DataBank(dest);
                final int dstAdjust = ImageUtil.getINT_RGBA_DataAdjust(dest);
                int dp = ImageUtil.getINT_RGBA_DataOffset(dest);

                double point_1 = startY;
                for (int i = 0; i < h; i++) {
                    double point_0 = startX;
                    for (int end = dp + w; dp < end; dp++) {
                        perlinTurbulence.turbulence(channels, point_0, point_1, fractalNoise, null, null);
                        destPixels[dp] = cm.getRGB(channelsToRGB(channels));
                        point_0 += scaleX;
                    }
                    point_1 += scaleY;
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
            perlinTurbulence.turbulence(channels, x, y, type == Type.fractalNoise, null, null);
            return channelsToRGB(channels);
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
