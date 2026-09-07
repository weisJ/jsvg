/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.renderer.RenderContext;

abstract class ChainedFilterPrimitive extends AbstractFilterPrimitive implements FilterPrimitive {

    protected final FilterChannelKey outerLastResult = new OuterLastResult();

    protected abstract @NotNull FilterPrimitive @NotNull [] primitives();

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds input = impl().layoutInput(filterLayoutContext);
        filterLayoutContext.resultChannels().addResult(outerLastResult, input);
        for (FilterPrimitive primitive : primitives()) {
            primitive.layoutFilter(context, filterLayoutContext);
        }
        // The chain implements one primitive, whose default subregion comes from its original input.
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(), input.region());
        LayoutBounds result = filterLayoutContext.resultChannels().get(DefaultFilterChannel.LastResult);
        impl().saveLayoutResult(result.withRegion(region), filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        filterContext.resultChannels().addResult(outerLastResult, impl().inputChannel(filterContext));
        for (FilterPrimitive primitive : primitives()) {
            primitive.applyFilter(context, filterContext);
        }
        impl().saveResult(filterContext.getChannel(DefaultFilterChannel.LastResult), filterContext);
    }

    private static final class OuterLastResult implements FilterChannelKey {
        private final String key = "outer-last-result-" + hashCode();

        @Override
        public @NotNull Object key() {
            return key;
        }
    }
}
