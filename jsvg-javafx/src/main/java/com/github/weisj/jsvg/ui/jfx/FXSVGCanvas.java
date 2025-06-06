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
package com.github.weisj.jsvg.ui.jfx;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.util.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.renderer.animation.Animation;
import com.github.weisj.jsvg.ui.jfx.skin.FXSVGCanvasSkin;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * A JavaFX node for displaying a {@link SVGDocument}}
 */
public class FXSVGCanvas extends Control {

    public enum RenderBackend {
        /**
         * Renders directly to a {@link GraphicsContext} and benefits from some hardware acceleration.
         * The majority of SVG's will render correctly, but some specific features e.g. Filters, Masks may not
         */
        JavaFX,
        /**
         * Renders using JSVG AWT implementation, which may be slower to update but may display the SVG more accurately
         */
        AWT
    }

    private static final String STYLE_CLASS = "fx-svg-canvas";
    private static final String STYLE_CLASS_TRANSPARENT_PATTERN = "show-transparent-pattern";
    private static final RenderBackend DEFAULT_RENDER_BACKEND = RenderBackend.JavaFX;
    private static final ViewBox DEFAULT_VIEW_BOX = null;
    private static final AnimationPeriod DEFAULT_ANIMATION = new AnimationPeriod(0, 0, false);
    private static final boolean DEFAULT_USE_SVG_VIEW_BOX = false;
    private static final boolean DEFAULT_ANIMATED = true;
    private static final boolean DEFAULT_SHOW_TRANSPARENT_PATTERN = false;

    private final Timeline timeline = new Timeline();

    public FXSVGCanvas() {
        getStyleClass().add(STYLE_CLASS);
        if (isShowingTransparentBackground()) getStyleClass().add(STYLE_CLASS_TRANSPARENT_PATTERN);

        currentAnimation.addListener((observable, oldValue, newValue) -> {
            setupAnimation(newValue);
        });

        currentAnimation.bind(Bindings.createObjectBinding(() -> {
            if (!animated.get()) return DEFAULT_ANIMATION;
            if (animation.get() != null) return animation.get();
            SVGDocument document = getDocument();
            return document == null ? DEFAULT_ANIMATION : document.animation();
        }, document, animation, animated));

        currentViewBox.bind(Bindings.createObjectBinding(() -> {
            if (viewBox.get() != null) return viewBox.get();
            if (!useSVGViewBox.get()) return DEFAULT_VIEW_BOX;
            SVGDocument document = getDocument();
            return document == null ? DEFAULT_VIEW_BOX : document.viewBox();
        }, viewBox, document, useSVGViewBox));

        showTransparentPatternProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue) getStyleClass().remove(STYLE_CLASS_TRANSPARENT_PATTERN);
            if (newValue) getStyleClass().add(STYLE_CLASS_TRANSPARENT_PATTERN);
        });

    }

    private void setupAnimation(Animation animation) {
        timeline.getKeyFrames().clear();
        timeline.getKeyFrames()
                .add(new KeyFrame(Duration.millis(animation.startTime()), new KeyValue(animationElapsedTime, 0)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(animation.endTime()),
                new KeyValue(animationElapsedTime, animation.endTime())));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.playFromStart();
    }

    ////////////////////////////////////////////////

    /**
     * Requests that the SVG Document be repainted
     * In most cases this is not necessary, as the canvas will automatically be repainted when any properties (e.g. document, view-box, backend) are changed
     */
    public void repaint() {
        SkinBase<?> skin = (SkinBase<?>) getSkin();
        if (skin != null) {
            FXSVGCanvasSkin fxSkin = (FXSVGCanvasSkin) skin;
            fxSkin.markDirty();
        }
    }

    public void playAnimation() {
        timeline.play();
    }

    public void pauseAnimation() {
        timeline.pause();
    }

    public void restartAnimation() {
        timeline.playFromStart();
    }

    public void stopAnimation() {
        timeline.stop();
    }

    ////////////////////////////////////////////////

    private final ObjectProperty<@NotNull RenderBackend> renderBackend =
            new SimpleObjectProperty<>(DEFAULT_RENDER_BACKEND);

    public @NotNull RenderBackend getRenderBackend() {
        return renderBackend.get();
    }

    public @NotNull ObjectProperty<@NotNull RenderBackend> renderBackendProperty() {
        return renderBackend;
    }

    public void setRenderBackend(@NotNull RenderBackend renderer) {
        this.renderBackend.set(renderer);
    }

    ////////////////////////////////////////////////

    private final ObjectProperty<@Nullable SVGDocument> document = new SimpleObjectProperty<>();

    public @Nullable SVGDocument getDocument() {
        return document.get();
    }

    public @NotNull ObjectProperty<@Nullable SVGDocument> documentProperty() {
        return document;
    }

    public void setDocument(@Nullable SVGDocument document) {
        this.document.set(document);
    }

    ////////////////////////////////////////////////

    private final BooleanProperty useSVGViewBox = new SimpleBooleanProperty(DEFAULT_USE_SVG_VIEW_BOX);

    public boolean isUsingSVGViewBox() {
        return useSVGViewBox.get();
    }

    public BooleanProperty useSVGViewBoxProperty() {
        return useSVGViewBox;
    }

    public void setUseSVGViewBox(boolean useSVGViewBox) {
        this.useSVGViewBox.set(useSVGViewBox);
    }

    ////////////////////////////////////////////////

    private final ObjectProperty<@Nullable ViewBox> currentViewBox = new SimpleObjectProperty<>();

    public @Nullable ViewBox getCurrentViewBox() {
        return currentViewBox.get();
    }

    public @Nullable ReadOnlyObjectProperty<@Nullable ViewBox> currentViewBoxProperty() {
        return currentViewBox;
    }

    ////////////////////////////////////////////////

    private final ObjectProperty<@Nullable ViewBox> viewBox = new SimpleObjectProperty<>(DEFAULT_VIEW_BOX);

    public @Nullable ViewBox getViewBox() {
        return viewBox.get();
    }

    public @NotNull ObjectProperty<@Nullable ViewBox> viewBoxProperty() {
        return viewBox;
    }

    public void setViewBox(@Nullable ViewBox viewBox) {
        this.viewBox.set(viewBox);
    }

    ////////////////////////////////////////////////

    private final BooleanProperty animated = new SimpleBooleanProperty(DEFAULT_ANIMATED);

    public boolean isAnimated() {
        return animated.get();
    }

    public @NotNull BooleanProperty animatedProperty() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated.set(animated);
    }

    ////////////////////////////////////////////////

    private final LongProperty animationElapsedTime = new SimpleLongProperty();

    public long getAnimationElapsedTime() {
        return animationElapsedTime.get();
    }

    public @NotNull ReadOnlyLongProperty animationElapsedTimeProperty() {
        return animationElapsedTime;
    }

    ////////////////////////////////////////////////

    private final ObjectProperty<@NotNull Animation> currentAnimation = new SimpleObjectProperty<>(DEFAULT_ANIMATION);

    public @NotNull Animation getCurrentAnimation() {
        return currentAnimation.get();
    }

    public @NotNull ReadOnlyObjectProperty<@NotNull Animation> currentAnimationProperty() {
        return currentAnimation;
    }

    ////////////////////////////////////////////////

    private final ObjectProperty<@Nullable Animation> animation = new SimpleObjectProperty<>();

    public @Nullable Animation getAnimation() {
        return animation.get();
    }

    public @NotNull ObjectProperty<@Nullable Animation> animationProperty() {
        return animation;
    }

    public void setAnimation(@Nullable Animation animation) {
        this.animation.set(animation);
    }

    ////////////////////////////////////////////////

    private final BooleanProperty showTransparentPattern = new SimpleBooleanProperty(DEFAULT_SHOW_TRANSPARENT_PATTERN);

    public boolean isShowingTransparentBackground() {
        return showTransparentPattern.get();
    }

    public BooleanProperty showTransparentPatternProperty() {
        return showTransparentPattern;
    }

    public void setShowTransparentPattern(boolean showTransparentPattern) {
        this.showTransparentPattern.set(showTransparentPattern);
    }

    ////////////////////////////////////////////////

    @Override
    public String getUserAgentStylesheet() {
        return FXSVGCanvas.class.getResource("fx-svg-canvas.css").toExternalForm();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new FXSVGCanvasSkin(this);
    }
}
