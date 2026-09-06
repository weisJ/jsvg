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
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.filter.FilterLayoutContext;

public final class LayoutBounds {

    public static final class Data {
        private final @NotNull Rectangle2D bounds;
        private final @NotNull FloatInsets clipBoundsEscapeInsets;

        private Data(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets) {
            this.bounds = bounds;
            this.clipBoundsEscapeInsets = clipBoundsEscapeInsets;
        }

        public @NotNull FloatInsets clipBoundsEscapeInsets() {
            return clipBoundsEscapeInsets;
        }

        public @NotNull Rectangle2D bounds() {
            return bounds;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "bounds=" + GeometryUtil.compactRepresentation(bounds) +
                    ", clipBoundsEscapeInsets=" + clipBoundsEscapeInsets +
                    '}';
        }
    }

    public static class ComputeFlags {

        private static final int CACHE_SIZE = 2;
        public static final @NotNull ComputeFlags INITIAL = new ComputeFlags(false);
        public final boolean operatesOnWholeFilterRegion;

        public ComputeFlags(boolean operatesOnWholeFilterRegion) {
            this.operatesOnWholeFilterRegion = operatesOnWholeFilterRegion;
        }

        private int cacheIndex() {
            return operatesOnWholeFilterRegion ? 1 : 0;
        }

        public @NotNull ComputeFlags or(@NotNull ComputeFlags other) {
            if (operatesOnWholeFilterRegion) return this;
            if (other.operatesOnWholeFilterRegion) return other;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ComputeFlags that = (ComputeFlags) o;
            return operatesOnWholeFilterRegion == that.operatesOnWholeFilterRegion;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(operatesOnWholeFilterRegion);
        }
    }

    private final @NotNull Rectangle2D region;
    private final @NotNull Function<ComputeFlags, @NotNull Data> computer;
    private final @NotNull ComputeFlags additionalFlags;
    private final @Nullable Data @NotNull [] cache = new Data[ComputeFlags.CACHE_SIZE];

    public LayoutBounds(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets,
            @NotNull Rectangle2D region) {
        Data data = new Data(bounds, clipBoundsEscapeInsets);
        this.region = region;
        this.computer = flags -> data;
        this.additionalFlags = ComputeFlags.INITIAL;
    }

    private LayoutBounds(@NotNull Rectangle2D region, @NotNull Function<ComputeFlags, @NotNull Data> computer,
            @NotNull ComputeFlags flags) {
        this.region = region;
        this.computer = computer;
        this.additionalFlags = flags;
    }

    public @NotNull Rectangle2D region() {
        return region;
    }

    public @NotNull LayoutBounds withRegion(@NotNull Rectangle2D region) {
        if (this.region.equals(region)) return this;
        return new LayoutBounds(region, this::resolve, additionalFlags);
    }

    public @NotNull LayoutBounds transform(
            @NotNull BiFunction<@NotNull Data, ComputeFlags, @NotNull Data> newTransformer) {
        return new LayoutBounds(region, flags -> newTransformer.apply(resolve(flags), flags), additionalFlags);
    }

    public @NotNull LayoutBounds withFlags(@NotNull ComputeFlags flags) {
        ComputeFlags combined = additionalFlags.or(flags);
        if (combined.equals(additionalFlags)) return this;
        return new LayoutBounds(region, this::resolve, combined);
    }

    public @NotNull Data resolve(@NotNull ComputeFlags flags) {
        ComputeFlags effectiveFlags = flags.or(additionalFlags);
        int index = effectiveFlags.cacheIndex();
        Data data = cache[index];
        if (data != null) return data;

        data = computer.apply(effectiveFlags);
        cache[index] = data;
        return data;
    }

    public boolean isResolved() {
        for (Data data : cache) {
            if (data != null) return true;
        }
        return false;
    }

    public @NotNull LayoutBounds union(@NotNull LayoutBounds other) {
        Rectangle2D unionRegion = unionRegion(other);
        return new LayoutBounds(unionRegion, flags -> {
            Data data = resolve(flags);
            Data otherData = other.resolve(flags);
            return new Data(
                    data.bounds.createUnion(otherData.bounds),
                    GeometryUtil.max(data.clipBoundsEscapeInsets, otherData.clipBoundsEscapeInsets));
        }, additionalFlags);
    }

    public @NotNull Rectangle2D unionRegion(@NotNull LayoutBounds other) {
        if (region.isEmpty()) return other.region;
        if (other.region.isEmpty() || region.equals(other.region)) return region;
        return region.createUnion(other.region);
    }

    public @NotNull LayoutBounds grow(float horizontal, float vertical, @NotNull FilterLayoutContext context) {
        Rectangle2D clipBounds = context.clipBounds();
        return transform((data, flags) -> {
            FloatInsets insets = data.clipBoundsEscapeInsets;
            FloatInsets growInsets = new FloatInsets(vertical, horizontal, vertical, horizontal);
            Rectangle2D newBounds = GeometryUtil.grow(data.bounds, growInsets);
            FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), growInsets);
            return new Data(newBounds, GeometryUtil.max(insets, ins));
        });
    }

    public @NotNull LayoutBounds translate(float dx, float dy, @NotNull FilterLayoutContext context) {
        Rectangle2D clipBounds = context.clipBounds();
        return transform((data, flags) -> {
            FloatInsets insets = data.clipBoundsEscapeInsets;
            FloatInsets offsetInsets = new FloatInsets(
                    Math.max(dy, 0),
                    Math.max(dx, 0),
                    Math.max(-dy, 0),
                    Math.max(-dx, 0));
            Rectangle2D newBounds = GeometryUtil.grow(data.bounds, offsetInsets);
            // The new layout rect is the union of the original rect and the shifted rect.
            FloatInsets ins = GeometryUtil.max(GeometryUtil.overhangInsets(clipBounds, data.bounds), offsetInsets);
            return new Data(newBounds, GeometryUtil.max(insets, ins));
        });
    }

    @Override
    public String toString() {
        return "LayoutBounds{" +
                "region=" + GeometryUtil.compactRepresentation(region) +
                ", resolved=" + isResolved() +
                '}';
    }
}
