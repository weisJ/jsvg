/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.View;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Immutable configuration for a single SVG render operation.
 * <p>
 * The {@link #view() view} selects the source region and aspect-ratio mapping from the SVG document, while the
 * {@link #viewport() viewport} defines the destination rectangle into which that view is rendered.
 */
public final class RenderConfig {
    private final @NotNull PlatformSupport platformSupport;
    private final @Nullable FontLoader fontLoader;
    private final @Nullable Float fontSize;
    private final @Nullable String defaultFontFamily;
    private final @Nullable ViewBox viewport;
    private final @Nullable View view;
    private final @NotNull AnimationState animationState;

    private RenderConfig(@NotNull PlatformSupport platformSupport,
            @Nullable FontLoader fontLoader,
            @Nullable Float fontSize,
            @Nullable String defaultFontFamily,
            @Nullable ViewBox viewport,
            @Nullable View view,
            @NotNull AnimationState animationState) {
        this.platformSupport = platformSupport;
        this.fontLoader = fontLoader;
        this.fontSize = fontSize;
        this.defaultFontFamily = defaultFontFamily;
        this.viewport = viewport;
        this.view = view;
        this.animationState = animationState;
    }

    /** Creates a builder initialized with JSVG's default rendering configuration. */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /** Returns the platform integration used for images and repaint support. */
    public @NotNull PlatformSupport platformSupport() {
        return platformSupport;
    }

    /** Returns the custom font loader, or {@code null} when only platform fonts should be used. */
    @ApiStatus.Experimental
    public @Nullable FontLoader fontLoader() {
        return fontLoader;
    }

    /** Returns the configured default font size, or JSVG's current default when none was configured. */
    public float fontSize() {
        return fontSize != null ? fontSize : SVGFont.defaultFontSize();
    }

    /** Returns the configured default font family, or JSVG's current default when none was configured. */
    public @NotNull String defaultFontFamily() {
        return defaultFontFamily != null ? defaultFontFamily : SVGFont.defaultFontFamily();
    }

    /**
     * Returns the destination viewport into which the selected document {@link #view() view} is rendered, or
     * {@code null} to use the document's intrinsic size.
     */
    public @Nullable ViewBox viewport() {
        return viewport;
    }

    /** Returns the source document view selected for rendering, or {@code null} for the root view. */
    public @Nullable View view() {
        return view;
    }

    /** Returns the animation state used to resolve animated values. */
    public @NotNull AnimationState animationState() {
        return animationState;
    }

    /** Builder for {@link RenderConfig}. */
    public static final class Builder {
        private @NotNull PlatformSupport platformSupport = NullPlatformSupport.INSTANCE;
        private @Nullable FontLoader fontLoader;
        private @Nullable Float fontSize;
        private @Nullable String defaultFontFamily;
        private @Nullable ViewBox viewport;
        private @Nullable View view;
        private @NotNull AnimationState animationState = AnimationState.NO_ANIMATION;

        private Builder() {}

        /** Sets the platform integration used for images and repaint support. */
        public @NotNull Builder platformSupport(@NotNull PlatformSupport platformSupport) {
            this.platformSupport = platformSupport;
            return this;
        }

        /** Sets the custom font loader, or {@code null} to use only platform fonts. */
        @ApiStatus.Experimental
        public @NotNull Builder fontLoader(@Nullable FontLoader fontLoader) {
            this.fontLoader = fontLoader;
            return this;
        }

        /** Sets the default font size, or {@code null} to use JSVG's current default. */
        public @NotNull Builder fontSize(@Nullable Float fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        /** Sets the default font family, or {@code null} to use JSVG's current default. */
        public @NotNull Builder defaultFontFamily(@Nullable String defaultFontFamily) {
            this.defaultFontFamily = defaultFontFamily;
            return this;
        }

        /**
         * Sets the destination viewport into which the selected document {@link #view(View) view} is rendered, or
         * {@code null} to use the document's intrinsic size.
         */
        public @NotNull Builder viewport(@Nullable ViewBox viewport) {
            this.viewport = viewport;
            return this;
        }

        /** Selects the source document view, or {@code null} to use the root view. */
        public @NotNull Builder view(@Nullable View view) {
            this.view = view;
            return this;
        }

        /** Sets the animation state; {@code null} selects {@link AnimationState#NO_ANIMATION}. */
        public @NotNull Builder animationState(@Nullable AnimationState animationState) {
            this.animationState = animationState != null ? animationState : AnimationState.NO_ANIMATION;
            return this;
        }

        /** Creates an immutable render configuration. */
        public @NotNull RenderConfig build() {
            return new RenderConfig(
                    platformSupport, fontLoader, fontSize, defaultFontFamily, viewport, view, animationState);
        }
    }
}
