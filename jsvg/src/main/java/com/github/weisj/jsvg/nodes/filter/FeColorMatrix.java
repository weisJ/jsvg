/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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

import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = { /* <animate>, <set> */ }
)
public final class FeColorMatrix extends AbstractFilterPrimitive {
    public static final String TAG = "fecolormatrix";
    private static final String KEY_VALUES = "values";

    private ImageFilter filter;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        String type = attributeNode.getValue("type");
        if (type == null) type = "matrix";
        filter = null;
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "matrix":
                double[] colorTransform = attributeNode.getDoubleList(KEY_VALUES);
                if (colorTransform.length == 20) {
                    boolean isIdentity = Arrays.equals(colorTransform,
                            new double[] {
                                    1, 0, 0, 0, 0,
                                    0, 1, 0, 0, 0,
                                    0, 0, 1, 0, 0,
                                    0, 0, 0, 1, 0});
                    if (!isIdentity) filter = new MatrixRGBFilter(colorTransform);
                }
                break;
            case "saturate":
                float s = attributeNode.getFloat(KEY_VALUES, 1);
                if (s != 1) {
                    filter = new LinearRGBFilter(
                            0.213 + 0.787 * s, 0.715 * (1 - s), 0.072 * (1 - s),
                            0.213 * (1 - s), 0.715 + 0.285 * s, 0.072 * (1 - s),
                            0.213 * (1 - s), 0.715 * (1 - s), 0.072 + 0.928 * s);
                }
                break;
            case "huerotate":
                float hueRotate = attributeNode.getFloat(KEY_VALUES, 0);
                if (hueRotate != 1) {
                    double radians = Math.toRadians(hueRotate);
                    double sin = Math.sin(radians);
                    double cos = Math.cos(radians);
                    //@formatter:off
                    filter = new LinearRGBFilter(
                        0.213 + cos * 0.787 - sin * 0.2127, 0.715 - 0.715 * cos - 0.715 * sin,0.072 - 0.072 * cos + 0.982 * sin,
                        0.213 - cos * 0.213 + sin * 0.143, 0.715 + 0.285 * cos + 0.140 * sin,0.072 - 0.072 * cos - 0.283 * sin,
                        0.213 - cos * 0.213 - sin * 0.787, 0.715 - 0.715 * cos + 0.715 * sin,0.072 + 0.982 * cos + 0.072 * sin);
                    //@formatter:on
                }
                break;
            case "luminancetoalpha":
                filter = new LuminanceToAlphaFilter();
                break;
            default:
                break;
        }
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (filter == null) return;
        impl().saveResult(impl().inputChannel(filterContext).applyFilter(filter), filterContext);
    }

    private static int toRgbRange(double value) {
        return (int) Math.max(Math.min(Math.round(value), 255), 0);
    }

    private static final class MatrixRGBFilter extends RGBImageFilter {

        private final double r1, r2, r3, r4, r5;
        private final double g1, g2, g3, g4, g5;
        private final double b1, b2, b3, b4, b5;
        private final double a1, a2, a3, a4, a5;

        private MatrixRGBFilter(double[] values) {
            r1 = values[0];
            r2 = values[1];
            r3 = values[2];
            r4 = values[3];
            r5 = values[4];

            g1 = values[5];
            g2 = values[6];
            g3 = values[7];
            g4 = values[8];
            g5 = values[9];

            b1 = values[10];
            b2 = values[11];
            b3 = values[12];
            b4 = values[13];
            b5 = values[14];

            a1 = values[15];
            a2 = values[16];
            a3 = values[17];
            a4 = values[18];
            a5 = values[19];
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int a = (rgb >> 24) & 0xFF;
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            int nr = toRgbRange(r1 * r + r2 * g + r3 * b + r4 * a + r5);
            int ng = toRgbRange(g1 * r + g2 * g + g3 * b + g4 * a + g5);
            int nb = toRgbRange(b1 * r + b2 * g + b3 * b + b4 * a + b5);
            int na = toRgbRange(a1 * r + a2 * g + a3 * b + a4 * a + a5);

            return ((na & 0xFF) << 24) |
                    ((nr & 0xFF) << 16) |
                    ((ng & 0xFF) << 8) |
                    (nb & 0xFF);
        }
    }

    private static final class LinearRGBFilter extends RGBImageFilter {
        private final double r1, r2, r3;
        private final double g1, g2, g3;
        private final double b1, b2, b3;

        private LinearRGBFilter(
                double r1, double r2, double r3,
                double g1, double g2, double g3,
                double b1, double b2, double b3) {
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.g1 = g1;
            this.g2 = g2;
            this.g3 = g3;
            this.b1 = b1;
            this.b2 = b2;
            this.b3 = b3;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int a = (rgb >> 24) & 0xFF;
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            int nr = toRgbRange(r1 * r + r2 * g + r3 * b);
            int ng = toRgbRange(g1 * r + g2 * g + g3 * b);
            int nb = toRgbRange(b1 * r + b2 * g + b3 * b);

            return (a << 24) |
                    ((nr & 0xFF) << 16) |
                    ((ng & 0xFF) << 8) |
                    (nb & 0xFF);
        }
    }

    private static class LuminanceToAlphaFilter extends RGBImageFilter {

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            int na = toRgbRange(0.2125 * r + 0.7164 * g + 0.0712 * b);
            return (na & 0xFF) << 24;
        }
    }
}
