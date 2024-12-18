/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.animation.value;

import java.awt.geom.Path2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.value.Value;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.util.PathUtil;

public final class AnimatedPath implements Value<@NotNull Path2D> {

    private final @NotNull AnimatedFloatList list;
    private @Nullable Path2D cache;
    private final boolean closed;

    public AnimatedPath(@NotNull AnimatedFloatList list, boolean closed) {
        this.list = list;
        this.closed = closed;
    }

    @Override
    public @NotNull Path2D get(@NotNull MeasureContext context) {
        if (cache == null || list.isDirty(context.timestamp())) {
            cache = PathUtil.setPolyLine(cache, list.get(context), closed);
        }
        return cache;
    }
}
