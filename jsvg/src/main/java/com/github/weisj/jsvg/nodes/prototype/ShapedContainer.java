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
package com.github.weisj.jsvg.nodes.prototype;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.RenderContext;

public interface ShapedContainer<E> extends Container<E>, HasShape, SVGShape {

    @Override
    default @NotNull SVGShape shape() {
        return this;
    }

    @Override
    default @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        Path2D shape = new Path2D.Float();
        for (E child : children()) {
            if (!(child instanceof HasShape)) continue;
            RenderContext childContext = NodeRenderer.setupRenderContext(child, context);
            Shape childShape = ((HasShape) child).shape().shape(childContext, validate);
            shape.append(childShape, false);
        }
        if (this instanceof Transformable) {
            return ((Transformable) this).transformShape(shape, context.measureContext());
        }
        return shape;
    }

    @Override
    default @NotNull Rectangle2D bounds(@NotNull RenderContext context, boolean validate) {
        Rectangle2D bounds = null;
        for (E child : children()) {
            if (!(child instanceof HasShape)) continue;
            RenderContext childContext = NodeRenderer.setupRenderContext(child, context);
            Rectangle2D childBounds = ((HasShape) child).shape().bounds(childContext, validate);
            if (bounds == null) {
                bounds = childBounds;
            } else {
                Rectangle2D.union(bounds, childBounds, bounds);
            }
        }
        if (bounds == null) return new Rectangle2D.Float(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0, 0);
        if (this instanceof Transformable) {
            return ((Transformable) this).transformShape(bounds, context.measureContext()).getBounds2D();
        }
        return bounds;
    }
}
