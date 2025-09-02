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

import javafx.animation.*;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.ui.jfx.FXSVGCanvas;
import com.github.weisj.jsvg.ui.jfx.renderer.FXSVGRenderer;
import com.github.weisj.jsvg.ui.jfx.renderer.FXSVGRendererAWT;
import com.github.weisj.jsvg.ui.jfx.renderer.FXSVGRendererJavaFX;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Implementation of the {@link SkinBase} for {@link FXSVGCanvas}, internal use only
 */
public class FXSVGCanvasSkin extends SkinBase<FXSVGCanvas> {

    private final FXSVGCanvas svgCanvas;
    private final StackPane innerPane;

    private final AnimationTimer timer;
    private FXSVGRenderer activeRenderer;
    private boolean dirty = true;

    private final Timeline timeline = new Timeline();

    public FXSVGCanvasSkin(FXSVGCanvas svgCanvas) {
        super(svgCanvas);
        this.consumeMouseEvents(false);
        this.svgCanvas = svgCanvas;

        innerPane = new StackPane();
        innerPane.getStyleClass().add("inner-stack-pane");
        getChildren().add(innerPane);

        registerChangeListener(svgCanvas.documentProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.renderBackendProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.useSVGViewBoxProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.viewBoxProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.animationProperty(), o -> markDirty());
        registerChangeListener(svgCanvas.elapsedAnimationTimeProperty(), o -> markDirty());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick();
            }
        };
        timer.start();
    }

    public void markDirty() {
        timer.start();
        dirty = true;
    }

    public void tick() {
        if (!dirty) {
            timer.stop();
            return;
        }
        dirty = false;

        SVGDocument svgDocument = svgCanvas.getDocument();
        FXSVGCanvas.RenderBackend backend = svgCanvas.getRenderBackend();
        ViewBox viewBox = activeViewBox();
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

    private @Nullable ViewBox activeViewBox() {
        if (svgCanvas.getViewBox() != null) return svgCanvas.getViewBox();
        if (!svgCanvas.isUsingSVGViewBox()) return null;
        SVGDocument document = svgCanvas.getDocument();
        return document == null ? null : document.viewBox();
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

    private static @NotNull FXSVGRenderer createRenderer(FXSVGCanvas.RenderBackend backend) {
        switch (backend) {
            case JavaFX:
                return new FXSVGRendererJavaFX();
            case AWT:
                return new FXSVGRendererAWT();
            default:
                throw new IllegalArgumentException("Unknown render backend: " + backend);
        }
    }

}
