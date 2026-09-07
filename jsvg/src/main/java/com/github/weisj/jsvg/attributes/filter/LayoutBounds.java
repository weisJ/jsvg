/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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

    public enum CoversWholeRegion {
        YES,
        NO
    }

    private final @NotNull Rectangle2D bounds;
    private final @NotNull FloatInsets clipBoundsEscapeInsets;
    private final @NotNull Rectangle2D region;

    public static @NotNull LayoutBounds createInitial(@NotNull Rectangle2D bounds, @NotNull Rectangle2D region) {
        return new LayoutBounds(bounds, new FloatInsets(), region);
    }

    public LayoutBounds(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets,
            @NotNull Rectangle2D region) {
        this.bounds = bounds;
        this.clipBoundsEscapeInsets = clipBoundsEscapeInsets;
        this.region = region;
    }

    public @NotNull Rectangle2D bounds() {
        return bounds;
    }

    public @NotNull FloatInsets clipBoundsEscapeInsets() {
        return clipBoundsEscapeInsets;
    }

    public @NotNull Rectangle2D region() {
        return region;
    }

    public @NotNull LayoutBounds withRegion(@NotNull Rectangle2D region) {
        return withRegion(region, CoversWholeRegion.NO);
    }

    public @NotNull LayoutBounds withRegion(@NotNull Rectangle2D region,
            @NotNull CoversWholeRegion coversWholeRegion) {
        if (coversWholeRegion == CoversWholeRegion.YES && !region.isEmpty() && !bounds.contains(region)) {
            return new LayoutBounds(GeometryUtil.union(bounds, region), clipBoundsEscapeInsets, region);
        }
        if (this.region.equals(region)) return this;
        return new LayoutBounds(bounds, clipBoundsEscapeInsets, region);
    }

    public @NotNull LayoutBounds union(@NotNull LayoutBounds other) {
        return new LayoutBounds(
                GeometryUtil.union(bounds, other.bounds),
                GeometryUtil.max(clipBoundsEscapeInsets, other.clipBoundsEscapeInsets),
                GeometryUtil.union(region, other.region));
    }

    public @NotNull LayoutBounds grow(float horizontal, float vertical, @NotNull FilterLayoutContext context) {
        Rectangle2D clipBounds = context.clipBounds();
        FloatInsets growInsets = new FloatInsets(vertical, horizontal, vertical, horizontal);
        Rectangle2D newBounds = GeometryUtil.grow(bounds, growInsets);
        FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), growInsets);
        return new LayoutBounds(newBounds, GeometryUtil.max(clipBoundsEscapeInsets, ins), region);
    }

    public @NotNull LayoutBounds translate(float dx, float dy, @NotNull FilterLayoutContext context) {
        Rectangle2D clipBounds = context.clipBounds();
        Rectangle2D newBounds = GeometryUtil.grow(bounds, new FloatInsets(
                Math.max(-dy, 0),
                Math.max(-dx, 0),
                Math.max(dy, 0),
                Math.max(dx, 0)));
        // Visible output can require input outside the clip in the opposite direction.
        FloatInsets ins = GeometryUtil.max(GeometryUtil.overhangInsets(clipBounds, bounds), new FloatInsets(
                Math.max(dy, 0),
                Math.max(dx, 0),
                Math.max(-dy, 0),
                Math.max(-dx, 0)));
        return new LayoutBounds(newBounds, GeometryUtil.max(clipBoundsEscapeInsets, ins), region);
    }

    @Override
    public String toString() {
        return "LayoutBounds{" +
                "bounds=" + GeometryUtil.compactRepresentation(bounds) +
                ", clipBoundsEscapeInsets=" + clipBoundsEscapeInsets +
                ", region=" + GeometryUtil.compactRepresentation(region) +
                '}';
    }
}
