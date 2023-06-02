/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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

import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class FilterLayoutContext {

    private final @NotNull ChannelStorage<LayoutBounds> resultChannels = new ChannelStorage<>();
    private final @NotNull UnitType primitiveUnits;
    private final @NotNull Rectangle2D elementBounds;
    private final @NotNull Rectangle2D clipBounds;

    public FilterLayoutContext(@NotNull UnitType primitiveUnits, @NotNull Rectangle2D elementBounds,
            @NotNull Rectangle2D clipBounds) {
        this.primitiveUnits = primitiveUnits;
        this.elementBounds = elementBounds;
        this.clipBounds = clipBounds;
    }

    public @NotNull UnitType primitiveUnits() {
        return primitiveUnits;
    }

    public @NotNull Rectangle2D elementBounds() {
        return elementBounds;
    }

    public @NotNull Rectangle2D filterPrimitiveRegion(@NotNull MeasureContext context,
            @NotNull FilterPrimitive filterPrimitive) {
        return primitiveUnits.computeViewBounds(context, elementBounds,
                filterPrimitive.x(), filterPrimitive.y(), filterPrimitive.width(), filterPrimitive.height());
    }

    public @NotNull ChannelStorage<LayoutBounds> resultChannels() {
        return resultChannels;
    }

    public @NotNull Rectangle2D clipBounds() {
        return clipBounds;
    }
}
