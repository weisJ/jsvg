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
package com.github.weisj.jsvg.geometry;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;

public class AWTSVGShape<T extends Shape> implements MeasurableShape {
    public static final Rectangle2D EMPTY_SHAPE = new Rectangle();
    protected final @NotNull T shape;
    private Rectangle2D bounds;

    private double pathLength;

    public AWTSVGShape(@NotNull T shape) {
        this(shape, Double.NaN);
    }

    private AWTSVGShape(@NotNull T shape, double pathLength) {
        this.shape = shape;
        this.pathLength = pathLength;
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        return shape;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (bounds == null) bounds = shape.getBounds2D();
        return bounds;
    }

    @Override
    public double pathLength(@NotNull MeasureContext measureContext) {
        if (Double.isNaN(pathLength)) {
            pathLength = computePathLength();
        }
        return pathLength;
    }

    private double computePathLength() {
        // Optimize shapes for which the computation is simple.
        if (shape instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) shape;
            return 2 * (r.getWidth() + r.getHeight());
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D e = (Ellipse2D) shape;
            double w = e.getWidth();
            double h = e.getHeight();
            if (w == h) return Math.PI * w;
            // General ellipse
            return SVGEllipse.ellipseCircumference(w / 2f, h / 2f);
        } else {
            return computeGenericPathLength();
        }
    }

    private double computeGenericPathLength() {
        return GeometryUtil.pathLength(shape);
    }

}
