/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.HasFilter;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.HasShape.Box;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.view.ViewBox;

public class ElementBounds {
    private final @NotNull SVGNode node;
    private final @NotNull RenderContext context;
    private final @Nullable Box suppliedBox;
    private final @Nullable Rectangle2D suppliedBounds;

    public ElementBounds(@NotNull SVGNode node, @NotNull RenderContext context) {
        this(node, context, null, null);
    }

    private ElementBounds(@NotNull SVGNode node, @NotNull RenderContext context,
            @Nullable Box suppliedBox, @Nullable Rectangle2D suppliedBounds) {
        this.node = node;
        this.context = context;
        this.suppliedBox = suppliedBox;
        this.suppliedBounds = suppliedBounds;
    }

    public static @NotNull ElementBounds fromUntransformedBounds(@NotNull SVGNode node, @NotNull RenderContext context,
            @NotNull Rectangle2D bounds, @NotNull Box box) {
        return new ElementBounds(node, context, box, bounds);
    }

    public @NotNull Rectangle2D boundingBox() {
        return bounds(Box.BoundingBox);
    }

    public @NotNull Rectangle2D fillBox() {
        return boundingBox();
    }

    public @NotNull Rectangle2D strokeBox() {
        return bounds(Box.StrokeBox);
    }

    public @NotNull Rectangle2D sourceBox() {
        return bounds(Box.SourceBox);
    }

    public @NotNull Rectangle2D outputBox() {
        return bounds(Box.OutputBox);
    }

    public @NotNull Rectangle2D bounds(@NotNull Box box) {
        if (box == suppliedBox && suppliedBounds != null) return suppliedBounds;
        if (box == Box.OutputBox) {
            Rectangle2D source = sourceBox();
            Filter filter = node instanceof HasFilter ? ((HasFilter) node).filter() : null;
            if (filter == null) return source;
            Filter.FilterLayout layout = filter.createFilterLayout(null, context, this);
            return layout != null ? GeometryUtil.union(source, layout.effectiveFilterArea()) : source;
        }
        if (node instanceof HasShape) return ((HasShape) node).computeUntransformedBounds(context, box);
        MeasureContext measureContext = context.measureContext();
        return new ViewBox(measureContext.viewWidth(), measureContext.viewHeight());
    }

    /** Bounds in the parent coordinate system. */
    public @NotNull Rectangle2D transformedBounds(@NotNull Box box) {
        return node instanceof HasShape
                ? ((HasShape) node).computeTransformedBounds(context, box)
                : bounds(box);
    }
}
