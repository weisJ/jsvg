/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import com.github.weisj.jsvg.renderer.output.Output;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.MeasureContext;

public final class FilterLayoutContext {

    private final @NotNull ChannelStorage<LayoutBounds> resultChannels = new ChannelStorage<>();
    private final @NotNull UnitType primitiveUnits;
    private final @NotNull Rectangle2D elementBounds;
    private final @NotNull Rectangle2D clipBounds;
    private final @NotNull Rectangle2D filterRegion;
    private final @NotNull MeasureContext measureContext;
    private final @NotNull AffineTransform transform;

    public FilterLayoutContext(@NotNull UnitType primitiveUnits, @NotNull Rectangle2D elementBounds,
            @NotNull Rectangle2D clipBounds, @NotNull Rectangle2D filterRegion,
            @NotNull MeasureContext measureContext, @NotNull AffineTransform transform) {
        this.primitiveUnits = primitiveUnits;
        this.elementBounds = elementBounds;
        this.clipBounds = clipBounds;
        this.filterRegion = filterRegion;
        this.measureContext = measureContext;
        this.transform = transform;
    }

    public @NotNull UnitType primitiveUnits() {
        return primitiveUnits;
    }

    public @NotNull Rectangle2D elementBounds() {
        return elementBounds;
    }

    public @NotNull Rectangle2D filterRegion() {
        return filterRegion;
    }

    public @NotNull AffineTransform transform() {
        return transform;
    }

    @NotNull
    public Rectangle2D filterPrimitiveRegion(@NotNull FilterPrimitiveBase primitive, @NotNull Rectangle2D defaults) {
        return resolveRegion(measureContext, primitive.x, primitive.y, primitive.width, primitive.height, defaults);
    }

    private @NotNull Rectangle2D resolveRegion(@NotNull MeasureContext context,
            @NotNull Length x, @NotNull Length y, @NotNull Length width, @NotNull Length height,
            @NotNull Rectangle2D defaults) {
        if (x.isUnspecified() && y.isUnspecified() && width.isUnspecified() && height.isUnspecified()) {
            if (defaults.isEmpty() || filterRegion.contains(defaults)) return defaults;
            return defaults.createIntersection(filterRegion);
        }
        // computeViewBounds adds the bounding-box origin, which the default coordinates already include.
        double originX = primitiveUnits == UnitType.ObjectBoundingBox ? elementBounds.getX() : 0;
        double originY = primitiveUnits == UnitType.ObjectBoundingBox ? elementBounds.getY() : 0;
        Rectangle2D region = primitiveUnits.computeViewBounds(context, elementBounds,
                x.orElseIfUnspecified((float) (defaults.getX() - originX)),
                y.orElseIfUnspecified((float) (defaults.getY() - originY)),
                width.orElseIfUnspecified((float) defaults.getWidth()),
                height.orElseIfUnspecified((float) defaults.getHeight()));
        // Every primitive exposes the effective subregion after clipping to the filter region.
        // computeViewBounds created this rectangle, so it can be clipped in place.
        Rectangle2D.intersect(region, filterRegion, region);
        return region;
    }

    public @NotNull ChannelStorage<LayoutBounds> resultChannels() {
        return resultChannels;
    }

    public @NotNull Rectangle2D clipBounds() {
        return clipBounds;
    }
}
