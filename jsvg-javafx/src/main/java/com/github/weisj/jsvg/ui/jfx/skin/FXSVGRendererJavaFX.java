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
package com.github.weisj.jsvg.ui.jfx.skin;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.jfx.impl.FXOutput;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.ui.jfx.FXSVGCanvas;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

class FXSVGRendererJavaFX extends FXSVGRenderer {
    private final Canvas canvas;
    private final GraphicsContext graphics;

    public FXSVGRendererJavaFX() {
        canvas = new Canvas();
        graphics = canvas.getGraphicsContext2D();
    }

    @Override
    public FXSVGCanvas.@NotNull RenderBackend getBackend() {
        return FXSVGCanvas.RenderBackend.JavaFX;
    }

    @Override
    public void render(@NotNull SVGDocument svgDocument, @Nullable ViewBox viewBox,
            @Nullable AnimationState animationState) {
        FloatSize svgSize = svgDocument.size();
        double width = svgSize.getWidth();
        double height = svgSize.getHeight();

        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(svgSize.getWidth());
            canvas.setHeight(svgSize.getHeight());
        }

        graphics.save();
        try {
            graphics.setTransform(1, 0, 0, 1, 0, 0);
            graphics.setGlobalAlpha(1D);
            graphics.setGlobalBlendMode(BlendMode.SRC_OVER);
            graphics.clearRect(0, 0, width, height);

            Output output = FXOutput.createForGraphicsContext(graphics);
            svgDocument.renderWithPlatform(NullPlatformSupport.INSTANCE, output, viewBox, animationState);
            output.dispose();
        } finally {
            graphics.restore();
        }
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public @NotNull Node getFXNode() {
        return canvas;
    }

}
