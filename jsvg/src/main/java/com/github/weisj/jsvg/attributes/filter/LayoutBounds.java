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

    public enum CoversWholeSourceBounds {
        YES,
        NO
    }

    /**
     * A conservative tight bound around pixels which can contribute to the result, including input
     * samples needed by a clipped repaint.
     * This is used to optimize backing buffer allocation by tracking the area where pixels are non-transparent
     * and can contribute to the output.
     */
    private final @NotNull Rectangle2D tightSourceBounds;
    private final @NotNull FloatInsets clipBoundsEscapeInsets;
    /**
     * The logical extent of the filter image. Pixels inside this rectangle but outside
     * {@link #tightSourceBounds} are transparent and still belong to the image.
     * Filter primitives use these bounds for clipping and edge sampling.
     * For SourceGraphic and SourceAlpha this is the filter region.
     * For a primitive result it is that primitive's subregion.
     */
    private final @NotNull Rectangle2D sourceBounds;

    public static @NotNull LayoutBounds createInitial(@NotNull Rectangle2D tightSourceBounds,
            @NotNull Rectangle2D sourceBounds) {
        return new LayoutBounds(tightSourceBounds, new FloatInsets(), sourceBounds);
    }

    public LayoutBounds(@NotNull Rectangle2D tightSourceBounds, @NotNull FloatInsets clipBoundsEscapeInsets,
            @NotNull Rectangle2D sourceBounds) {
        this.tightSourceBounds = tightSourceBounds;
        this.clipBoundsEscapeInsets = clipBoundsEscapeInsets;
        this.sourceBounds = sourceBounds;
    }

    public @NotNull Rectangle2D tightSourceBounds() {
        return tightSourceBounds;
    }

    public @NotNull FloatInsets clipBoundsEscapeInsets() {
        return clipBoundsEscapeInsets;
    }

    public @NotNull Rectangle2D sourceBounds() {
        return sourceBounds;
    }

    public @NotNull LayoutBounds withTightSourceBounds(@NotNull Rectangle2D tightSourceBounds) {
        if (this.tightSourceBounds.equals(tightSourceBounds)) return this;
        return new LayoutBounds(tightSourceBounds, clipBoundsEscapeInsets, sourceBounds);
    }

    public @NotNull LayoutBounds withSourceBounds(@NotNull Rectangle2D sourceBounds) {
        return withSourceBounds(sourceBounds, CoversWholeSourceBounds.NO);
    }

    public @NotNull LayoutBounds withSourceBounds(@NotNull Rectangle2D sourceBounds,
            @NotNull CoversWholeSourceBounds coversWholeSourceBounds) {
        if (coversWholeSourceBounds == CoversWholeSourceBounds.YES
                && !sourceBounds.isEmpty()
                && !tightSourceBounds.contains(sourceBounds)) {
            return new LayoutBounds(
                    GeometryUtil.union(tightSourceBounds, sourceBounds), clipBoundsEscapeInsets, sourceBounds);
        }
        if (this.sourceBounds.equals(sourceBounds)) return this;
        return new LayoutBounds(tightSourceBounds, clipBoundsEscapeInsets, sourceBounds);
    }

    public @NotNull LayoutBounds union(@NotNull LayoutBounds other) {
        return new LayoutBounds(
                GeometryUtil.union(tightSourceBounds, other.tightSourceBounds),
                GeometryUtil.max(clipBoundsEscapeInsets, other.clipBoundsEscapeInsets),
                GeometryUtil.union(sourceBounds, other.sourceBounds));
    }

    public @NotNull LayoutBounds grow(float horizontal, float vertical, @NotNull FilterLayoutContext context) {
        return grow(new FloatInsets(vertical, horizontal, vertical, horizontal), context);
    }

    public @NotNull LayoutBounds grow(@NotNull FloatInsets growInsets, @NotNull FilterLayoutContext context) {
        if (tightSourceBounds.isEmpty()) return this;
        Rectangle2D clipBounds = context.clipBounds();
        Rectangle2D newBounds = GeometryUtil.grow(tightSourceBounds, growInsets);
        FloatInsets requiredInsets = GeometryUtil.sum(clipBoundsEscapeInsets, growInsets);
        FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), requiredInsets);
        return new LayoutBounds(newBounds, GeometryUtil.max(clipBoundsEscapeInsets, ins), sourceBounds);
    }

    /**
     * Keeps the affected output bounds unchanged while ensuring that {@code requiredRect} is present
     * in the filter backing image.
     */
    public @NotNull LayoutBounds ensuringRectIsAvailable(@NotNull Rectangle2D requiredRect,
            @NotNull FilterLayoutContext context) {
        FloatInsets requiredInsets = GeometryUtil.overhangInsets(context.clipBounds(), requiredRect);
        return new LayoutBounds(
                tightSourceBounds, GeometryUtil.max(clipBoundsEscapeInsets, requiredInsets), sourceBounds);
    }

    public @NotNull LayoutBounds translate(float dx, float dy, @NotNull FilterLayoutContext context) {
        if (tightSourceBounds.isEmpty()) return this;
        Rectangle2D clipBounds = context.clipBounds();
        Rectangle2D newBounds = GeometryUtil.grow(tightSourceBounds, new FloatInsets(
                Math.max(-dy, 0),
                Math.max(-dx, 0),
                Math.max(dy, 0),
                Math.max(dx, 0)));
        // Visible output can require input outside the clip in the opposite direction.
        FloatInsets requiredInsets = GeometryUtil.sum(clipBoundsEscapeInsets, new FloatInsets(
                Math.max(dy, 0),
                Math.max(dx, 0),
                Math.max(-dy, 0),
                Math.max(-dx, 0)));
        FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), requiredInsets);
        return new LayoutBounds(newBounds, GeometryUtil.max(clipBoundsEscapeInsets, ins), sourceBounds);
    }

    @Override
    public String toString() {
        return "LayoutBounds{" +
                "tightSourceBounds=" + GeometryUtil.compactRepresentation(tightSourceBounds) +
                ", clipBoundsEscapeInsets=" + clipBoundsEscapeInsets +
                ", sourceBounds=" + GeometryUtil.compactRepresentation(sourceBounds) +
                '}';
    }
}
