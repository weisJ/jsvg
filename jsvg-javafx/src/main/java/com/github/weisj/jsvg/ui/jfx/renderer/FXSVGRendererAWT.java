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
package com.github.weisj.jsvg.ui.jfx.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.jfx.impl.bridge.FXRenderingHintsUtil;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.ui.jfx.FXSVGCanvas;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

public final class FXSVGRendererAWT implements FXSVGRenderer {

    private ImageView fxImageView;

    private BufferedImage awtImage;
    private WritableImage fxImage;
    private int currentRTWidth = -1;
    private int currentRTHeight = -1;

    public FXSVGRendererAWT() {
        fxImageView = new ImageView();
    }

    private void setupRenderTargets(int width, int height) {
        awtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fxImage = new WritableImage(width, height);
        currentRTWidth = width;
        currentRTHeight = height;
        fxImageView.setImage(fxImage);
    }

    private void disposeRenderTargets() {
        awtImage = null;
        fxImage = null;
        currentRTWidth = -1;
        currentRTHeight = -1;
    }

    private void flush() {
        fxImage = SwingFXUtils.toFXImage(awtImage, fxImage);
    }

    @Override
    public FXSVGCanvas.@NotNull RenderBackend getBackend() {
        return FXSVGCanvas.RenderBackend.AWT;
    }

    @Override
    public void render(@NotNull SVGDocument svgDocument, @Nullable ViewBox viewBox,
            @Nullable AnimationState animationState) {
        FloatSize size = svgDocument.size();
        int width = (int) size.width;
        int height = (int) size.height;

        if (currentRTWidth != width || currentRTHeight != height) {
            disposeRenderTargets();
            setupRenderTargets(width, height);
        }
        Graphics2D g = awtImage.createGraphics();
        Output output = Output.createForGraphics(g);
        FXRenderingHintsUtil.setupDefaultJFXRenderingHints(output);
        g.setBackground(new Color(0, 0, 0, 0));
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.clearRect(0, 0, width, height);
            svgDocument.renderWithPlatform(NullPlatformSupport.INSTANCE, output, viewBox, animationState);
        } finally {
            g.dispose();
        }
        flush();
    }

    @Override
    public void dispose() {
        disposeRenderTargets();
        fxImageView = null;
    }

    @Override
    public @NotNull Node getFXNode() {
        return fxImageView;
    }

}
