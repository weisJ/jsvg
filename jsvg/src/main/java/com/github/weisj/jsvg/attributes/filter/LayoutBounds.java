/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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
package com.github.weisj.jsvg.attributes.filter;

import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.filter.FilterLayoutContext;

public final class LayoutBounds {

    private final @NotNull Rectangle2D bounds;
    private final @NotNull FloatInsets clipBoundsEscapeInsets;

    public LayoutBounds(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets) {
        this.bounds = bounds;
        this.clipBoundsEscapeInsets = clipBoundsEscapeInsets;
    }

    public @NotNull Rectangle2D bounds() {
        return bounds;
    }

    public @NotNull FloatInsets clipBoundsEscapeInsets() {
        return clipBoundsEscapeInsets;
    }

    public @NotNull LayoutBounds union(@NotNull LayoutBounds other) {
        return new LayoutBounds(bounds.createUnion(other.bounds),
                GeometryUtil.max(clipBoundsEscapeInsets(), other.clipBoundsEscapeInsets()));
    }

    public @NotNull LayoutBounds grow(float horizontal, float vertical, @NotNull FilterLayoutContext context) {
        FloatInsets insets = clipBoundsEscapeInsets();
        Rectangle2D clipBounds = context.clipBounds();
        FloatInsets growInsets = new FloatInsets(vertical, horizontal, vertical, horizontal);
        Rectangle2D newBounds = GeometryUtil.grow(bounds, growInsets);
        FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), growInsets);
        return new LayoutBounds(newBounds, GeometryUtil.max(insets, ins));
    }

    public @NotNull LayoutBounds translate(float dx, float dy, @NotNull FilterLayoutContext context) {
        FloatInsets insets = clipBoundsEscapeInsets();
        Rectangle2D clipBounds = context.clipBounds();
        FloatInsets offsetInsets = new FloatInsets(
                Math.max(-dy, 0),
                Math.max(-dx, 0),
                Math.max(dy, 0),
                Math.max(dx, 0));
        Rectangle2D newBounds = GeometryUtil.grow(bounds, offsetInsets);
        // The new layout rect is the union of the original rect and the shifted rect.
        FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, bounds), offsetInsets);
        return new LayoutBounds(newBounds, GeometryUtil.max(insets, ins));
    }

    @Override
    public String toString() {
        return "LayoutBounds{" +
                "bounds=" + GeometryUtil.compactRepresentation(bounds) +
                ", clipInsets=" + clipBoundsEscapeInsets +
                '}';
    }
}
