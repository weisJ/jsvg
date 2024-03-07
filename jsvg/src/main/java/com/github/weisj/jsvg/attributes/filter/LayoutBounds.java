/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;

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

        public static final @NotNull ComputeFlags INITIAL = new ComputeFlags(false);
        public final boolean operatesOnWholeFilterRegion;

        public ComputeFlags(boolean operatesOnWholeFilterRegion) {
            this.operatesOnWholeFilterRegion = operatesOnWholeFilterRegion;
        }

        public @NotNull ComputeFlags or(@NotNull ComputeFlags other) {
            return new ComputeFlags(operatesOnWholeFilterRegion || other.operatesOnWholeFilterRegion);
        }
    }

    private final @NotNull Data data;
    private final @NotNull BiFunction<@NotNull Data, ComputeFlags, @NotNull Data> transformer;
    private final ComputeFlags additionalFlags;

    public LayoutBounds(@NotNull Rectangle2D bounds, @NotNull FloatInsets clipBoundsEscapeInsets) {
        data = new Data(bounds, clipBoundsEscapeInsets);
        transformer = (d, f) -> d;
        additionalFlags = new ComputeFlags(false);
    }

    private LayoutBounds(@NotNull Data data,
            @NotNull BiFunction<@NotNull Data, ComputeFlags, @NotNull Data> transformer,
            @NotNull ComputeFlags flags) {
        this.data = data;
        this.transformer = transformer;
        this.additionalFlags = flags;
    }

    public @NotNull LayoutBounds transform(
            @NotNull BiFunction<@NotNull Data, ComputeFlags, @NotNull Data> newTransformer) {
        return new LayoutBounds(data, (data, flags) -> {
            Data newData = transformer.apply(data, flags);
            return newTransformer.apply(newData, flags);
        }, additionalFlags);
    }

    public @NotNull LayoutBounds withFlags(@NotNull ComputeFlags flags) {
        return new LayoutBounds(data, transformer, additionalFlags.or(flags));
    }

    public @NotNull Data resolve(@NotNull ComputeFlags flags) {
        return transformer.apply(data, flags.or(additionalFlags));
    }

    public @NotNull LayoutBounds union(@NotNull LayoutBounds other) {
        return transform((data, flags) -> {
            Data otherData = other.resolve(flags);
            return new Data(
                    data.bounds.createUnion(otherData.bounds),
                    GeometryUtil.max(data.clipBoundsEscapeInsets, otherData.clipBoundsEscapeInsets));
        });
    }

    public @NotNull LayoutBounds grow(float horizontal, float vertical, @NotNull FilterLayoutContext context) {
        return transform((data, flags) -> {
            FloatInsets insets = data.clipBoundsEscapeInsets;
            Rectangle2D clipBounds = context.clipBounds();
            FloatInsets growInsets = new FloatInsets(vertical, horizontal, vertical, horizontal);
            Rectangle2D newBounds = GeometryUtil.grow(data.bounds, growInsets);
            FloatInsets ins = GeometryUtil.min(GeometryUtil.overhangInsets(clipBounds, newBounds), growInsets);
            return new Data(newBounds, GeometryUtil.max(insets, ins));
        });
    }

    public @NotNull LayoutBounds translate(float dx, float dy, @NotNull FilterLayoutContext context) {
        return transform((data, flags) -> {
            FloatInsets insets = data.clipBoundsEscapeInsets;
            FloatInsets offsetInsets = new FloatInsets(
                    Math.max(dy, 0),
                    Math.max(dx, 0),
                    Math.max(-dy, 0),
                    Math.max(-dx, 0));
            Rectangle2D newBounds = GeometryUtil.grow(data.bounds, offsetInsets);
            Rectangle2D clipBounds = context.clipBounds();
            // The new layout rect is the union of the original rect and the shifted rect.
            FloatInsets ins = GeometryUtil.max(GeometryUtil.overhangInsets(clipBounds, data.bounds), offsetInsets);
            return new Data(newBounds, GeometryUtil.max(insets, ins));
        });
    }

    @Override
    public String toString() {
        return "LayoutBounds{" +
                "data=" + data +
                ", transformer=" + transformer +
                '}';
    }
}
