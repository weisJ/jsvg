/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = { /* <animate>, <set> */ }
)
public final class FeGaussianBlur extends FilterPrimitive {
    public static final String TAG = "fegaussianblur";

    private float[] stdDeviation;

    private double xCurrent;
    private double yCurrent;
    private ConvolveOp xBlur;
    private ConvolveOp yBlur;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        stdDeviation = attributeNode.getFloatList("stdDeviation");
    }

    @Override
    public @NotNull BufferedImageOp[] createImageOps(@NotNull RenderContext context,
            @NotNull Filter.FilterInfo filterInfo) {
        if (stdDeviation.length == 0) return new BufferedImageOp[0];
        AffineTransform at = filterInfo.graphics().getTransform();
        double xSigma = GeometryUtil.scaleXOfTransform(at) * stdDeviation[0];
        double ySigma = GeometryUtil.scaleYOfTransform(at) * stdDeviation[Math.min(stdDeviation.length - 1, 1)];

        if (xSigma < 0 || ySigma < 0) return new BufferedImageOp[0];

        BufferedImageOp xOp = xSigma > 0
                ? createGaussianBlurFilter(xSigma, true)
                : null;
        BufferedImageOp yOp = ySigma > 0
                ? createGaussianBlurFilter(ySigma, false)
                : null;

        if (xOp != null && yOp != null) {
            return new BufferedImageOp[] {xOp, yOp};
        } else if (xOp != null) {
            return new BufferedImageOp[] {xOp};
        } else if (yOp != null) {
            return new BufferedImageOp[] {yOp};
        }
        return new BufferedImageOp[0];
    }

    private @NotNull BufferedImageOp createGaussianBlurFilter(double sigma, boolean horizontal) {
        double radius = 2f * sigma + 1;
        int size = (int) Math.ceil(radius) + 1;
        if (horizontal && xBlur != null && xCurrent == sigma) return xBlur;
        if (!horizontal && yBlur != null && yCurrent == sigma) return yBlur;

        if (horizontal) {
            xCurrent = sigma;
        } else {
            yCurrent = sigma;
        }
        float[] data = new float[size];

        double radius2 = radius * radius;
        double twoSigmaSquare = 2.0f * sigma * sigma;
        double sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        double total = 0.0f;

        double middle = size / 2f;
        for (int i = 0; i < size; i++) {
            double distance = middle - i;
            distance *= distance;

            double value = distance <= radius2
                    ? Math.exp(-distance / twoSigmaSquare) / sigmaRoot
                    : 0f;
            data[i] = (float) value;
            total += data[i];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        if (horizontal) {
            xBlur = new ConvolveOp(new Kernel(size, 1, data), ConvolveOp.EDGE_ZERO_FILL, null);
        } else {
            yBlur = new ConvolveOp(new Kernel(1, size, data), ConvolveOp.EDGE_ZERO_FILL, null);
        }

        return horizontal ? xBlur : yBlur;
    }
}
