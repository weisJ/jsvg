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
package com.github.weisj.jsvg.nodes;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.geometry.MeasurableShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.renderer.PaintContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.ShapeRenderer;

public abstract class ShapeNode extends RenderableSVGNode implements HasShape {
    private PaintContext paintContext;
    private Length pathLength;
    private MeasurableShape shape;

    @Override
    public final void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        pathLength = attributeNode.getLength("pathLength", Length.UNSPECIFIED);
        paintContext = PaintContext.parse(attributeNode);
        shape = buildShape(attributeNode);
    }

    protected abstract @NotNull MeasurableShape buildShape(@NotNull AttributeNode attributeNode);

    @Override
    public @NotNull SVGShape shape() {
        return shape;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return super.isVisible(context);
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        MeasureContext measureContext = context.measureContext();
        Shape paintShape = shape.shape(context);
        Rectangle2D bounds = shape.usesOptimizedBoundsCalculation()
                ? shape.bounds(context, false)
                : paintShape.getBounds2D();
        float pathLengthFactor = 1f;
        if (pathLength.isSpecified()) {
            double effectiveLength = pathLength.resolveLength(measureContext);
            double actualLength = shape.pathLength(measureContext);
            pathLengthFactor = (float) (actualLength / effectiveLength);
        }
        ShapeRenderer.renderShape(context, paintContext, g, paintShape, bounds,
                shape.canBeFilled(), true, pathLengthFactor);
    }
}
