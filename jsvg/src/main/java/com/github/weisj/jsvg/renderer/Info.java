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
package com.github.weisj.jsvg.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.Renderable;

class Info implements AutoCloseable {
    protected final @NotNull RenderContext context;
    protected final @NotNull Output output;
    private final @NotNull Renderable renderable;

    Info(@NotNull Renderable renderable, @NotNull RenderContext context, @NotNull Output output) {
        this.renderable = renderable;
        this.context = context;
        this.output = output;
    }

    public @NotNull Renderable renderable() {
        return renderable;
    }

    public @NotNull Output output() {
        return output;
    }

    public @NotNull RenderContext context() {
        return context;
    }

    @Override
    public void close() {
        output.dispose();
    }

    static final class InfoWithFilter extends Info {
        private final @NotNull Filter filter;
        private final @NotNull Filter.FilterInfo filterInfo;

        static @Nullable InfoWithFilter create(@NotNull Renderable renderable, @NotNull RenderContext context,
                @NotNull Output output, @NotNull Filter filter, @NotNull ElementBounds elementBounds) {
            Filter.FilterInfo info = filter.createFilterInfo(output, context, elementBounds);
            if (info == null) return null;
            return new InfoWithFilter(renderable, context, output, filter, info);
        }

        private InfoWithFilter(@NotNull Renderable renderable, @NotNull RenderContext context, @NotNull Output output,
                @NotNull Filter filter, @NotNull Filter.FilterInfo filterInfo) {
            super(renderable, context, output);
            this.filter = filter;
            this.filterInfo = filterInfo;
        }

        @Override
        public @NotNull Output output() {
            return filterInfo.output();
        }

        @Override
        public @NotNull RenderContext context() {
            return filterInfo.context();
        }

        @Override
        public void close() {
            Output previousOutput = this.output;
            filter.applyFilter(previousOutput, context, filterInfo);
            filterInfo.blitImage(previousOutput, context);
            filterInfo.close();
            super.close();
        }
    }
}
