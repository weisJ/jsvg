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
package com.github.weisj.jsvg.attributes.paint;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.RenderContext;

public interface SVGPaint {
    AwtSVGPaint DEFAULT_PAINT = new AwtSVGPaint(PaintParser.DEFAULT_COLOR);
    SVGPaint NONE = new SVGPaint() {
        @Override
        public void fillShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) { /* NONE shouldn't fill anything */ }

        @Override
        public void drawShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) { /* NONE shouldn't draw anything */ }

        @Override
        public String toString() {
            return "SVGPaint.None";
        }
    };
    SVGPaint CURRENT_COLOR = new SVGPaint() {
        @Override
        public void fillShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) {
            throw new IllegalStateException("Sentinel color CURRENT_COLOR shouldn't be used for painting directly");
        }

        @Override
        public void drawShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) {
            throw new IllegalStateException("Sentinel color CURRENT_COLOR shouldn't be used for painting directly");
        }

        @Override
        public String toString() {
            return "SVGPaint.CurrentColor";
        }
    };
    SVGPaint CONTEXT_FILL = new SVGPaint() {
        @Override
        public void fillShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) {
            throw new IllegalStateException("Sentinel color CONTEXT_FILL shouldn't be used for painting directly");
        }

        @Override
        public void drawShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) {
            throw new IllegalStateException("Sentinel color CONTEXT_FILL shouldn't be used for painting directly");
        }

        @Override
        public String toString() {
            return "SVGPaint.ContextFill";
        }
    };
    SVGPaint CONTEXT_STROKE = new SVGPaint() {
        @Override
        public void fillShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) {
            throw new IllegalStateException("Sentinel color CONTEXT_STROKE shouldn't be used for painting directly");
        }

        @Override
        public void drawShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
                @Nullable Rectangle2D bounds) {
            throw new IllegalStateException("Sentinel color CONTEXT_STROKE shouldn't be used for painting directly");
        }

        @Override
        public String toString() {
            return "SVGPaint.ContextStroke";
        }
    };

    void fillShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds);

    void drawShape(@NotNull Graphics2D g, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds);

    default boolean isVisible() {
        return this != NONE;
    }
}
