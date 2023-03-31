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


import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = { /* <animate>, <set> */ }
)
public final class FeGaussianBlur extends AbstractFilterPrimitive {
    public static final String TAG = "fegaussianblur";

    private float[] stdDeviation;
    private EdgeMode edgeMode;

    private double xCurrent;
    private double yCurrent;
    private Kernel xBlur;
    private Kernel yBlur;
    private final Kernel[] kernels = new Kernel[2];

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        stdDeviation = attributeNode.getFloatList("stdDeviation");
        edgeMode = attributeNode.getEnum("edgeMode", EdgeMode.Duplicate);
    }


    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (stdDeviation.length == 0) return;

        // TODO: Use proper transform here
        AffineTransform at = filterContext.info().graphics().getTransform();
        double xSigma = GeometryUtil.scaleXOfTransform(at) * stdDeviation[0];
        double ySigma = GeometryUtil.scaleYOfTransform(at) * stdDeviation[Math.min(stdDeviation.length - 1, 1)];

        if (xSigma < 0 || ySigma < 0) return;

        ImageProducer input = impl().inputChannel(filterContext).producer();

        int kernelCount = 0;
        if (xSigma > 0) {
            kernels[kernelCount++] = createConvolveKernel(xSigma, true);
        }
        if (ySigma > 0) {
            kernels[kernelCount++] = createConvolveKernel(ySigma, false);
        }

        ImageProducer output = edgeMode.convolve(context, filterContext, input, kernels, kernelCount);
        impl().saveResult(new ImageProducerChannel(output), filterContext);
    }


    private @NotNull Kernel createConvolveKernel(double sigma, boolean horizontal) {
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

        // Otherwise, data is all zeros, which we can't reasonably normalize.
        if (total != 0) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (float) (data[i] / total);
            }
        }

        if (horizontal) {
            xBlur = new Kernel(size, 1, data);
        } else {
            yBlur = new Kernel(1, size, data);
        }

        return horizontal ? xBlur : yBlur;
    }
}
