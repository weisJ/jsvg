/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;

public interface HasShape {

    enum Box {
        BoundingBox,
        StrokeBox
    }

    default @NotNull Shape elementShape(@NotNull RenderContext context) {
        Shape shape = untransformedElementShape(context);
        if (this instanceof Transformable) {
            return ((Transformable) this).transformShape(shape, context);
        }
        return shape;
    }


    @NotNull
    Shape untransformedElementShape(@NotNull RenderContext context);

    default @NotNull Rectangle2D elementBounds(@NotNull RenderContext context, Box box) {
        Rectangle2D shape = untransformedElementBounds(context, box);
        if (!GeometryUtil.isValidRect(shape)) return shape;
        if (this instanceof Transformable) {
            return ((Transformable) this).transformShape(shape, context).getBounds2D();
        }
        return shape;
    }

    @NotNull
    Rectangle2D untransformedElementBounds(@NotNull RenderContext context, Box box);
}
