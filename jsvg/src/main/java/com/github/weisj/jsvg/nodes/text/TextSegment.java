/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
package com.github.weisj.jsvg.nodes.text;


import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.TextOutput;

interface TextSegment {
    default boolean isValid(@NotNull RenderContext currentContext) {
        return true;
    }

    interface RenderableSegment extends TextSegment {
        void prepareSegmentForRendering(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
                @NotNull TextOutput textOutput);

        void renderSegmentWithoutLayout(@NotNull GlyphCursor cursor, @NotNull RenderContext context,
                @NotNull Output output);

        boolean hasFixedLength();

        enum UseTextLengthForCalculation {
            YES,
            NO
        }

        @NotNull
        TextMetrics computeTextMetrics(@NotNull RenderContext context, @NotNull UseTextLengthForCalculation flag);

        void appendTextShape(@NotNull GlyphCursor cursor, @NotNull MutableGlyphRun glyphRun,
                @NotNull RenderContext context);
    }
}
