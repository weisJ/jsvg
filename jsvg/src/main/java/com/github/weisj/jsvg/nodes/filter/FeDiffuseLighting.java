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
import java.awt.image.*;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds.CoversWholeRegion;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.NormalizedAlphaSampler;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    categories = {Category.LightSource},
    anyOf = {Animate.class, Set.class}
)
public final class FeDiffuseLighting extends AbstractFilterPrimitive implements Container<LightSource> {
    public static final String TAG = "fediffuselighting";

    private static final Logger LOGGER = LogFactory.createLogger(FeDiffuseLighting.class);

    private float surfaceScale;
    private float diffuseConstant;
    private Color lightingColor;
    private double @Nullable [] kernelUnitLength;
    private @Nullable LightSource lightSource;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        surfaceScale = attributeNode.getFloat("surfaceScale", 1);
        diffuseConstant = attributeNode.getNonNegativeFloat("diffuseConstant", 1);
        lightingColor = attributeNode.getColor("lighting-color", Color.WHITE);

        double[] values = attributeNode.getDoubleList("kernelUnitLength");
        if ((values.length == 1 || values.length == 2) && values[0] > 0
                && (values.length == 1 || values[1] > 0)) {
            double x = values[0];
            double y = values.length == 2 ? values[1] : x;
            kernelUnitLength = new double[] {x, y};
        } else {
            kernelUnitLength = null;
        }
    }

    @Override
    public void addChild(@Nullable String id, @NotNull SVGNode node) {
        if (node instanceof LightSource) {
            if (lightSource != null) {
                LOGGER.log(Logger.Level.WARNING,
                        "Element <fediffuselighting> should only have one light source. Using the first one.");
            } else {
                lightSource = (LightSource) node;
            }
        }
    }

    @Override
    public @NotNull List<? extends @NotNull LightSource> children() {
        if (lightSource == null) return Collections.emptyList();
        return Collections.singletonList(lightSource);
    }

    @Override
    public boolean requiresAlignedBuffer(@NotNull FilterLayoutContext context) {
        return lightSource != null;
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(), input.region());
        AffineTransform primitiveToImage = filterLayoutContext.primitiveUnits().applyToTransform(
                new AffineTransform(filterLayoutContext.transform()), filterLayoutContext.elementBounds());
        Point2D.Double step = samplingStep(primitiveToImage);
        filterLayoutContext.inverseTransform().deltaTransform(step, step);
        // Normals need neighboring height samples beyond the repaint clip.
        LayoutBounds bounds = input.grow((float) Math.abs(step.x), (float) Math.abs(step.y), filterLayoutContext);
        impl().saveLayoutResult(bounds.withRegion(region, CoversWholeRegion.YES), filterLayoutContext);
    }

    private @NotNull Point2D.Double samplingStep(@NotNull AffineTransform transform) {
        assert GeometryUtil.isAxisAligned(transform);
        if (kernelUnitLength == null) return new Point2D.Double(1, 1);
        Point2D.Double step = new Point2D.Double(kernelUnitLength[0], kernelUnitLength[1]);
        transform.deltaTransform(step, step);
        step.setLocation(Math.abs(step.x), Math.abs(step.y));
        return step;
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (lightSource == null) {
            Filter.FilterInfo info = filterContext.info();
            BufferedImage img = ImageUtil.createCompatibleTransparentImage(info.imageWidth, info.imageHeight);
            impl().saveResult(new ImageProducerChannel(img.getSource()), filterContext);
            return;
        }

        AffineTransform primitiveToImage = filterContext.info().output().transform();
        Rectangle2D inputRegion = GeometryUtil.transformBounds(
                primitiveToImage, filterContext.layout(impl().inputChannelKey()).region()).getBounds2D();
        filterContext.primitiveUnits().applyToTransform(primitiveToImage, filterContext.info().elementBounds());
        ImageFilter lightingFilter = new BufferedImageFilter(
                new DiffuseLightingOp(lightSource, primitiveToImage, inputRegion, colorInterpolation(filterContext)));
        impl().saveResult(impl().inputChannel(filterContext).applyFilter(lightingFilter), filterContext);
    }

    private final class DiffuseLightingOp implements BufferedImageOp {

        private final @NotNull LightSource lightSource;
        private final @NotNull AffineTransform imageToPrimitive;
        private final @NotNull Point2D.Double sampleStep;
        private final boolean swappedAxes;
        private final double normalScaleX;
        private final double normalScaleY;
        private final @NotNull Rectangle2D inputRegion;
        private final @Nullable ColorInterpolation colorInterpolation;

        private DiffuseLightingOp(@NotNull LightSource lightSource, @NotNull AffineTransform primitiveToImage,
                @NotNull Rectangle2D inputRegion, @Nullable ColorInterpolation colorInterpolation) {
            this.lightSource = lightSource;
            this.inputRegion = inputRegion;
            imageToPrimitive = GeometryUtil.createInverse(primitiveToImage);
            sampleStep = samplingStep(primitiveToImage);
            swappedAxes = primitiveToImage.getScaleX() == 0;
            double scaleX = swappedAxes ? primitiveToImage.getShearX() : primitiveToImage.getScaleX();
            double scaleY = swappedAxes ? primitiveToImage.getShearY() : primitiveToImage.getScaleY();
            // Convert each image-axis Sobel response to a signed primitive-coordinate normal component.
            normalScaleX = -surfaceScale * scaleX / sampleStep.x;
            normalScaleY = -surfaceScale * scaleY / sampleStep.y;
            this.colorInterpolation = colorInterpolation;
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
            return ImageUtil.createCompatibleDestImage(src, dstCM);
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
                ColorModel cm = ColorModel.getRGBdefault();
                result = createCompatibleDestImage(src, cm);
            }

            WritableRaster raster = result.getRaster();
            int w = raster.getWidth();
            int h = raster.getHeight();
            int[] lightColor = {lightingColor.getRed(), lightingColor.getGreen(), lightingColor.getBlue(), 255};
            boolean linearRGB = colorInterpolation != ColorInterpolation.S_RGB;
            if (linearRGB) ColorUtil.sRGBtoLinearRGBinPlace(lightColor);

            final int[] destPixels = ImageUtil.getINT_RGBA_DataBank(raster);
            final int dstAdjust = ImageUtil.getINT_RGBA_DataAdjust(raster);
            int dp = ImageUtil.getINT_RGBA_DataOffset(raster);

            NormalizedAlphaSampler samples = new NormalizedAlphaSampler(src);
            Point2D.Double primitivePoint = new Point2D.Double();
            for (int y = 0; y < h; y++) {
                for (int x = 0, end = dp + w; dp < end; dp++, x++) {
                    // Z(x,y) = surfaceScale * I(x,y), where I is the input alpha.
                    double z = surfaceScale * samples.at(x, y);
                    // Light positions, heights and normals share the primitive coordinate system.
                    primitivePoint.setLocation(x + 0.5, y + 0.5);
                    imageToPrimitive.transform(primitivePoint, primitivePoint);
                    LightSource.Light light = lightSource.lightAt(primitivePoint.x, primitivePoint.y, z);
                    // D = kd * (N dot L) * light color; output alpha is always one.
                    double diffuse = diffuseConstant * light.intensity *
                            diffuseAt(samples, x, y, light);

                    int r = ColorUtil.toRgbRange(lightColor[0] * diffuse);
                    int g = ColorUtil.toRgbRange(lightColor[1] * diffuse);
                    int b = ColorUtil.toRgbRange(lightColor[2] * diffuse);
                    if (linearRGB) {
                        r = ColorUtil.linearRGBtoSRGBBand(r);
                        g = ColorUtil.linearRGBtoSRGBBand(g);
                        b = ColorUtil.linearRGBtoSRGBBand(b);
                    }
                    destPixels[dp] = (0xFF << 24) | (r << 16) | (g << 8) | b;
                }
                dp += dstAdjust;
            }
            return result;
        }

        private double diffuseAt(@NotNull NormalizedAlphaSampler samples, int x, int y,
                @NotNull LightSource.Light light) {
            if (!inputRegion.contains(x + 0.5, y + 0.5)) return Math.max(0, light.z);

            // Select edges of the input surface, not edges of the clipped backing image.
            boolean hasLeft = x + 0.5 - sampleStep.x >= inputRegion.getMinX();
            boolean hasRight = x + 0.5 + sampleStep.x < inputRegion.getMaxX();
            boolean hasTop = y + 0.5 - sampleStep.y >= inputRegion.getMinY();
            boolean hasBottom = y + 0.5 + sampleStep.y < inputRegion.getMaxY();
            SobelKernel kernel = SobelKernel.at(hasLeft, hasRight, hasTop, hasBottom);

            // If neither neighbor exists on an axis, the surface is constant along that axis.
            double stepX = hasLeft || hasRight ? sampleStep.x : 0;
            double stepY = hasTop || hasBottom ? sampleStep.y : 0;
            double gradientX = 0;
            double gradientY = 0;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int index = 3 * row + column;
                    int kx = kernel.kx[index];
                    int ky = kernel.ky[index];
                    if (kx == 0 && ky == 0) continue;

                    double alpha = samples.at(x + (column - 1) * stepX, y + (row - 1) * stepY);
                    gradientX += kx * alpha;
                    gradientY += ky * alpha;
                }
            }

            double nx = normalScaleX * kernel.factorX * gradientX;
            double ny = normalScaleY * kernel.factorY * gradientY;
            if (swappedAxes) {
                double swap = nx;
                nx = ny;
                ny = swap;
            }
            // N = normalize(nx, ny, 1). Normalize the dot product directly.
            return Math.max(0, (nx * light.x + ny * light.y + light.z) / Math.sqrt(nx * nx + ny * ny + 1));
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }
    }

    /**
     * The specification's Kx, Ky and factors for each position in the input surface.
     * The factors below omit 1/dx and 1/dy, which are included in normalScaleX and normalScaleY.
     *
     * @see <a href="https://drafts.csswg.org/filter-effects/#feDiffuseLightingElement">Surface normals</a>
     */
    @SuppressWarnings("ImmutableEnumChecker") // Coefficient arrays are private, never exposed or modified.
    private enum SobelKernel {
        // @formatter:off
        TOP_LEFT(2.0 / 3, 2.0 / 3,
            new int[] {
                 0,  0,  0,
                 0, -2,  2,
                 0, -1,  1},
            new int[] {
                 0,  0,  0,
                 0, -2, -1,
                 0,  2,  1}),
        TOP(1.0 / 3, 1.0 / 2,
            new int[] {
                 0,  0,  0,
                -2,  0,  2,
                -1,  0,  1},
            new int[] {
                 0,  0,  0,
                -1, -2, -1,
                 1,  2,  1}),
        TOP_RIGHT(2.0 / 3, 2.0 / 3,
            new int[] {
                 0,  0,  0,
                -2,  2,  0,
                -1,  1,  0},
            new int[] {
                 0,  0,  0,
                -1, -2,  0,
                 1,  2,  0}),
        LEFT(1.0 / 2, 1.0 / 3,
            new int[] {
                 0, -1,  1,
                 0, -2,  2,
                 0, -1,  1},
            new int[] {
                 0, -2, -1,
                 0,  0,  0,
                 0,  2,  1}),
        INTERIOR(1.0 / 4, 1.0 / 4,
            new int[] {
                -1,  0,  1,
                -2,  0,  2,
                -1,  0,  1},
            new int[] {
                -1, -2, -1,
                 0,  0,  0,
                 1,  2,  1}),
        RIGHT(1.0 / 2, 1.0 / 3,
            new int[] {
                -1,  1,  0,
                -2,  2,  0,
                -1,  1,  0},
            new int[] {
                -1, -2,  0,
                 0,  0,  0,
                 1,  2,  0}),
        BOTTOM_LEFT(2.0 / 3, 2.0 / 3,
            new int[] {
                 0, -1,  1,
                 0, -2,  2,
                 0,  0,  0},
            new int[] {
                 0, -2, -1,
                 0,  2,  1,
                 0,  0,  0}),
        BOTTOM(1.0 / 3, 1.0 / 2,
            new int[] {
                -1,  0,  1,
                -2,  0,  2,
                 0,  0,  0},
            new int[] {
                -1, -2, -1,
                 1,  2,  1,
                 0,  0,  0}),
        BOTTOM_RIGHT(2.0 / 3, 2.0 / 3,
            new int[] {
                -1,  1,  0,
                -2,  2,  0,
                 0,  0,  0},
            new int[] {
                -1, -2,  0,
                 1,  2,  0,
                 0,  0,  0});
        // @formatter:on

        private final double factorX;
        private final double factorY;
        private final int @NotNull [] kx;
        private final int @NotNull [] ky;

        SobelKernel(double factorX, double factorY, int @NotNull [] kx, int @NotNull [] ky) {
            this.factorX = factorX;
            this.factorY = factorY;
            this.kx = kx;
            this.ky = ky;
        }

        private static @NotNull SobelKernel at(boolean hasLeft, boolean hasRight,
                boolean hasTop, boolean hasBottom) {
            if (!hasTop && hasBottom) {
                return atColumn(hasLeft, hasRight, TOP_LEFT, TOP, TOP_RIGHT);
            }
            if (hasTop && !hasBottom) {
                return atColumn(hasLeft, hasRight, BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT);
            }
            return atColumn(hasLeft, hasRight, LEFT, INTERIOR, RIGHT);
        }

        private static @NotNull SobelKernel atColumn(boolean hasLeft, boolean hasRight,
                @NotNull SobelKernel left, @NotNull SobelKernel interior, @NotNull SobelKernel right) {
            if (!hasLeft && hasRight) return left;
            if (hasLeft && !hasRight) return right;
            return interior;
        }
    }
}
