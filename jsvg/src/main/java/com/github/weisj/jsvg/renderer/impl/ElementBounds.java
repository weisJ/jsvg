/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.impl;

import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.HasFilter;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.view.ViewBox;

public class ElementBounds {

    private final @NotNull SVGNode node;
    private final RenderContext context;

    private Rectangle2D boundingBox;
    private Rectangle2D strokeBox;
    private Rectangle2D geometryBox;

    public ElementBounds(@NotNull SVGNode node, RenderContext context) {
        this.node = node;
        this.context = context;
    }

    public static @NotNull ElementBounds fromUntransformedBounds(@NotNull SVGNode node, @NotNull RenderContext context,
            @NotNull Rectangle2D bounds, HasShape.Box box) {
        ElementBounds elementBounds = new ElementBounds(node, context);
        switch (box) {
            case BoundingBox:
                elementBounds.boundingBox = bounds;
                break;
            case StrokeBox:
                elementBounds.strokeBox = bounds;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + box);
        }
        return elementBounds;
    }

    public @NotNull Rectangle2D boundingBox() {
        if (boundingBox == null) {
            boundingBox = elementBounds(node, context, HasShape.Box.BoundingBox);
        }
        return boundingBox;
    }

    public @NotNull Rectangle2D geometryBox() {
        if (geometryBox == null) {
            geometryBox = strokeBox();
            if (node instanceof HasFilter) {
                geometryBox = filterBounds((HasFilter) node, context, geometryBox);
            }
        }
        return geometryBox;
    }

    public @NotNull Rectangle2D strokeBox() {
        if (strokeBox == null) {
            strokeBox = elementBounds(node, context, HasShape.Box.StrokeBox);
        }
        return strokeBox;
    }

    public @NotNull Rectangle2D fillBox() {
        return boundingBox();
    }

    private static @NotNull Rectangle2D elementBounds(@NotNull SVGNode node, @NotNull RenderContext context,
            HasShape.Box box) {
        Rectangle2D elementBounds;
        if (node instanceof HasShape) {
            elementBounds = ((HasShape) node).untransformedElementBounds(context, box);
        } else {
            MeasureContext measureContext = context.measureContext();
            elementBounds = new ViewBox(measureContext.viewWidth(), measureContext.viewHeight());
        }
        return elementBounds;
    }

    private @NotNull Rectangle2D filterBounds(@NotNull HasFilter node, @NotNull RenderContext context,
            @NotNull Rectangle2D elementBounds) {
        Filter filter = node.filter();
        if (filter == null) return elementBounds;
        Filter.FilterBounds filterBounds = filter.createFilterBounds(null, context, this);
        if (filterBounds == null) return elementBounds;
        return elementBounds.createUnion(filterBounds.effectiveFilterArea());
    }

}
