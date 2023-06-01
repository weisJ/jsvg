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

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;

public final class FilterContext {

    private final @NotNull ChannelStorage<Channel> resultChannels = new ChannelStorage<>();
    private final Filter.FilterInfo info;
    private final @NotNull UnitType primitiveUnits;
    private final @NotNull RenderingHints renderingHints;

    public FilterContext(@NotNull Filter.FilterInfo info, @NotNull UnitType primitiveUnits,
            @NotNull RenderingHints renderingHints) {
        this.info = info;
        this.primitiveUnits = primitiveUnits;
        this.renderingHints = renderingHints;
    }

    public @NotNull Filter.FilterInfo info() {
        return info;
    }

    public @NotNull UnitType primitiveUnits() {
        return primitiveUnits;
    }

    public @NotNull RenderingHints renderingHints() {
        return renderingHints;
    }

    public @NotNull ChannelStorage<Channel> resultChannels() {
        return resultChannels;
    }

    public @NotNull Channel getChannel(@NotNull FilterChannelKey key) {
        return resultChannels.get(key);
    }
}
