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

import java.awt.*;
import java.awt.image.BufferedImage;
import javafx.animation.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.animation.Animation;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.jfx.impl.FXOutput;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.ui.jfx.FXSVGCanvas;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Implementation of the {@link SkinBase} for {@link FXSVGCanvas}, internal use only
 */
public class FXSVGCanvasSkin extends SkinBase<FXSVGCanvas> {

    private final FXSVGCanvas svgCanvas;
    private final StackPane innerPane;

    private final AnimationTimer timer;
    private Renderer activeRenderer;
    private boolean dirty;

    public FXSVGCanvasSkin(FXSVGCanvas svgCanvas) {
        super(svgCanvas);
        this.consumeMouseEvents(false);
        this.svgCanvas = svgCanvas;

        innerPane = new StackPane();
        innerPane.getStyleClass().add("inner-stack-pane");
        getChildren().add(innerPane);

        registerChangeListener(svgCanvas.documentProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.renderBackendProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.currentViewBoxProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.currentAnimationProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.elapsedAnimationTimeProperty(), o -> markDirty());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick();
            }
        };

        markDirty();
        timer.start();
    }

    public void markDirty() {
        timer.start();
        dirty = true;
    }

    public void tick() {
        if (!dirty) {
            return;
        }
        dirty = false;

        SVGDocument svgDocument = svgCanvas.getDocument();
        FXSVGCanvas.RenderBackend backend = svgCanvas.getRenderBackend();
        Animation animationPeriod = svgCanvas.getCurrentAnimation();
        ViewBox viewBox = svgCanvas.getCurrentViewBox();
        AnimationState state = new AnimationState(0, svgCanvas.getElapsedAnimationTime());

        if (activeRenderer != null && activeRenderer.getBackend() != backend) {
            innerPane.getChildren().remove(activeRenderer.getFXNode());
            activeRenderer.dispose();
            activeRenderer = null;
        }

        if (activeRenderer == null) {
            activeRenderer = createRenderer(backend);
            innerPane.getChildren().add(activeRenderer.getFXNode());
        }

        if (svgDocument != null) {
            activeRenderer.render(svgDocument, viewBox, state);
            activeRenderer.getFXNode().setVisible(true);
        } else {
            activeRenderer.getFXNode().setVisible(false);
        }
    }


    @Override
    public void dispose() {
        super.dispose();
        if (activeRenderer != null) {
            activeRenderer.dispose();
            activeRenderer = null;
        }
        timer.stop();
    }

    private static Renderer createRenderer(FXSVGCanvas.RenderBackend backend) {
        switch (backend) {
            case JavaFX:
                return new JFXRenderer();
            case AWT:
                return new AWTRenderer();
            default:
                throw new IllegalArgumentException("Unknown render backend: " + backend);
        }
    }

    private abstract static class Renderer {

        public abstract @NotNull FXSVGCanvas.RenderBackend getBackend();

        public abstract void render(@NotNull SVGDocument svgDocument, @Nullable ViewBox viewBox,
                @Nullable AnimationState animationState);

        public abstract void dispose();

        public abstract @NotNull Node getFXNode();

    }

    private static class JFXRenderer extends Renderer {
        private final Canvas canvas;
        private final GraphicsContext graphics;

        public JFXRenderer() {
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

    private static class AWTRenderer extends Renderer {

        private ImageView fxImageView;

        private BufferedImage awtImage;
        private WritableImage fxImage;
        private int currentRTWidth = -1;
        private int currentRTHeight = -1;

        public AWTRenderer() {
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
            FXOutput.setupDefaultJFXRenderingHints(output);
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
}
