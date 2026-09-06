/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.LayoutBounds;

final class ResolvedLayouts {
    private final @NotNull Map<FilterPrimitiveBase, LayoutBounds> layouts;
    private final @NotNull LayoutBounds lastResult;
    private final @NotNull LayoutBounds source;

    ResolvedLayouts(@NotNull Map<FilterPrimitiveBase, LayoutBounds> layouts,
            @NotNull LayoutBounds lastResult, @NotNull LayoutBounds source) {
        this.layouts = layouts;
        this.lastResult = lastResult;
        this.source = source;
    }

    @NotNull
    LayoutBounds.Data get(@NotNull FilterPrimitiveBase primitive) {
        LayoutBounds layout = layouts.get(primitive);
        if (layout == null) {
            throw new IllegalFilterStateException("Primitive layout not found.");
        }
        return layout.resolve(LayoutBounds.ComputeFlags.INITIAL);
    }

    @NotNull
    LayoutBounds.Data lastResult() {
        return lastResult.resolve(LayoutBounds.ComputeFlags.INITIAL);
    }

    @NotNull
    LayoutBounds.Data source() {
        return source.resolve(LayoutBounds.ComputeFlags.INITIAL);
    }
}
