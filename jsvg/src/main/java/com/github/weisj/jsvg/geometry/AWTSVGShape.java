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
package com.github.weisj.jsvg.geometry;

import java.awt.*;
import java.awt.geom.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.ConstantValue;
import com.github.weisj.jsvg.attributes.value.Value;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.impl.RenderContext;

public class AWTSVGShape<T extends Shape> implements MeasurableShape {
    public static final Rectangle2D EMPTY_SHAPE = new Rectangle();
    protected final @NotNull Value<@NotNull T> shapeValue;
    private Rectangle2D boundsCache;
    private T shapeCache;

    private double pathLength;

    public AWTSVGShape(@NotNull T shape) {
        this(new ConstantValue<>(shape));
    }

    public AWTSVGShape(@NotNull Value<@NotNull T> shapeValue) {
        this(shapeValue, Double.NaN);
    }

    private AWTSVGShape(@NotNull Value<@NotNull T> shapeValue, double pathLength) {
        this.shapeValue = shapeValue;
        this.pathLength = pathLength;
    }

    @Override
    public @NotNull T shape(@NotNull RenderContext context, boolean validate) {
        if (shapeCache == null || validate) {
            shapeCache = shapeValue.get(context.measureContext());
        }
        return shapeCache;
    }

    @Override
    public @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        if (boundsCache == null || validate) {
            Shape shape = shape(context, validate);
            boundsCache = shape.getBounds2D();
        }
        return boundsCache;
    }

    @Override
    public double pathLength(@NotNull RenderContext context) {
        if (Double.isNaN(pathLength)) {
            pathLength = computePathLength(context);
        }
        return pathLength;
    }

    private double computePathLength(@NotNull RenderContext context) {
        // Optimize shapes for which the computation is simple.
        Shape shape = shape(context, false);
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
            return GeometryUtil.pathLength(shape);
        }
    }

    @Override
    public boolean isClosed(@NotNull RenderContext context) {
        Shape shape = shape(context, false);
        if (shape instanceof Rectangle2D) {
            return true;
        } else if (shape instanceof Ellipse2D) {
            return true;
        } else {
            return GeometryUtil.isSingleClosedPath(shape);
        }
    }
}
