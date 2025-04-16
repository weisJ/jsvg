/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.impl.context;

import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.view.ViewBox;

public final class RenderContextAccessor {

    public interface Accessor {

        @NotNull
        RenderContext createInitial(@NotNull PlatformSupport awtSupport, @NotNull MeasureContext measureContext);

        @NotNull
        RenderContext deriveForSurface(@NotNull RenderContext context);

        @NotNull
        RenderContext deriveForChildGraphics(@NotNull RenderContext context);

        @NotNull
        RenderContext deriveForNode(
                @NotNull RenderContext context,
                @Nullable Mutator<PaintContext> paintContextMutator,
                @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
                @Nullable FontRenderContext frc,
                @Nullable ContextElementAttributes contextAttributes,
                @NotNull Object node);

        @NotNull
        RenderContext setupInnerViewRenderContext(@NotNull ViewBox viewBox,
                @NotNull RenderContext context, boolean inheritAttributes);

        @NotNull
        StrokeContext strokeContext(@NotNull RenderContext context);

        @NotNull
        FontRenderContext fontRenderContext(@NotNull RenderContext context);

        @NotNull
        FillRule fillRule(@NotNull RenderContext context);

        @NotNull
        PaintOrder paintOrder(@NotNull RenderContext context);

        @NotNull
        SVGFont font(@NotNull RenderContext context);


        void setRootTransform(@NotNull RenderContext context, @NotNull AffineTransform rootTransform);

        void setRootTransform(@NotNull RenderContext context, @NotNull AffineTransform rootTransform,
                @NotNull AffineTransform userSpaceTransform);
    }

    private static Accessor instance;

    private RenderContextAccessor() {}

    public static Accessor instance() {
        if (instance == null) {
            throw new IllegalStateException("RenderContextAccessor not initialized");
        }
        return instance;
    }

    public static void setInstance(Accessor accessor) {
        if (instance != null) {
            throw new IllegalStateException("RenderContextAccessor already initialized");
        }
        instance = accessor;
    }
}
