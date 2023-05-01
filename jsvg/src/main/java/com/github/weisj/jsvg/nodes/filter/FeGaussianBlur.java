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


import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.EdgeMode;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeGaussianBlur extends AbstractFilterPrimitive {
    public static final String TAG = "fegaussianblur";
    private static final double SQRT_2_PI = Math.sqrt(2 * Math.PI);
    private static final double THREE_QUARTER_SQRT_2_PI = SQRT_2_PI * 3f / 4f;
    private static final float KERNEL_PRECISION = 0.001f;

    // TODO: Use 2 here and implement fast box blurring
    private static final double BOX_BLUR_APPROXIMATION_THRESHOLD = Double.POSITIVE_INFINITY;

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
    public @NotNull Rectangle2D boundsNeededForOutput(@NotNull Rectangle2D region,
            @NotNull RenderContext context) {
        AffineTransform transform = new AffineTransform();
        transform.concatenate(context.rootTransform());
        transform.concatenate(context.userSpaceTransform());
        double[] sigma = computeAbsoluteStdDeviation(transform);
        // Add one for safety
        int horizontal = diameterForRadius(kernelRadiusForStandardDeviation(sigma[0])) + 1;
        int vertical = diameterForRadius(kernelRadiusForStandardDeviation(sigma[1])) + 1;
        return new Rectangle2D.Double(
                region.getX() - horizontal,
                region.getY() - vertical,
                region.getWidth() + 2 * horizontal,
                region.getHeight() + 2 * vertical);
    }

    private double[] computeAbsoluteStdDeviation(@NotNull AffineTransform at) {
        double xSigma = GeometryUtil.scaleXOfTransform(at) * stdDeviation[0];
        double ySigma = GeometryUtil.scaleYOfTransform(at) * stdDeviation[Math.min(stdDeviation.length - 1, 1)];
        return new double[] {xSigma, ySigma};
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        if (stdDeviation.length == 0) return;

        // TODO: Use proper transform here
        double[] sigma = computeAbsoluteStdDeviation(filterContext.info().graphics().getTransform());
        double xSigma = sigma[0];
        double ySigma = sigma[1];

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
        int radius = kernelRadiusForStandardDeviation(sigma);
        int diameter = diameterForRadius(radius);

        if (horizontal && xBlur != null && xCurrent == sigma) return xBlur;
        if (!horizontal && yBlur != null && yCurrent == sigma) return yBlur;

        if (horizontal) {
            xCurrent = sigma;
        } else {
            yCurrent = sigma;
        }
        float[] data = computeKernelData(diameter, sigma);

        if (horizontal) {
            xBlur = new Kernel(diameter, 1, data);
        } else {
            yBlur = new Kernel(1, diameter, data);
        }

        return horizontal ? xBlur : yBlur;
    }

    private static float normalConvolve(float x, double standardDeviation) {
        return (float) (Math.pow(Math.E, -x * x / (2 * standardDeviation * standardDeviation))
                / (standardDeviation * SQRT_2_PI));
    }

    private static float[] computeKernelData(int diameter, double standardDeviation) {
        final float[] data = new float[diameter];

        int mid = diameter / 2;
        float total = 0;
        for (int i = 0; i < diameter; i++) {
            data[i] = normalConvolve((float) i - mid, standardDeviation);
            total += data[i];
        }

        // Otherwise, data is all zeros, which we can't reasonably normalize.
        if (total > 0) {
            for (int i = 0; i < diameter; i++) {
                data[i] /= total;
            }
        }

        return data;
    }

    public static int diameterForRadius(int radius) {
        return 2 * radius + 1;
    }

    public static int kernelRadiusForStandardDeviation(double standardDeviation) {
        if (standardDeviation <= BOX_BLUR_APPROXIMATION_THRESHOLD) {
            float areaSum = (float) (0.5 / (standardDeviation * SQRT_2_PI));
            int i = 0;
            while (areaSum < 0.5 - KERNEL_PRECISION) {
                areaSum += normalConvolve(i, standardDeviation);
                i++;
            }
            return i;
        } else {
            int diameter = (int) Math.floor(THREE_QUARTER_SQRT_2_PI * standardDeviation + 0.5f);
            if (diameter % 2 == 0) {
                return diameter - 1 + diameter / 2;
            } else {
                return diameter - 2 + diameter / 2;
            }
        }
    }
}
