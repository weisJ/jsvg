/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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

/** Hints controlling how CSS media queries are evaluated. May move into {@link PlatformSupport} (or similar). */
@ApiStatus.Experimental
public class CssHints {
    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.SCREEN;
    public static final ColorScheme DEFAULT_COLOR_SCHEME = ColorScheme.LIGHT;

    /** The media type the document is rendered to. Used to evaluate {@code @media} at-rules. */
    public enum MediaType {
        /** Intended to be sent to a printer, PDF export. */
        PRINT,
        /** A computer screen. */
        SCREEN
    }

    /**
     * The color scheme the document is rendered to. Used to evaluate {@code @media prefers-color-scheme} media feature.
     */
    public enum ColorScheme {
        LIGHT,
        DARK
    }

    private final @NotNull CssHints.MediaType mediaType;
    private final @NotNull ColorScheme colorScheme;

    public static final @NotNull CssHints DEFAULT = new CssHints.Builder().build();

    private CssHints(@NotNull MediaType mediaType, @NotNull ColorScheme colorScheme) {
        this.mediaType = mediaType;
        this.colorScheme = colorScheme;
    }

    /** The media type the document is rendered to. */
    public @NotNull CssHints.MediaType mediaType() {
        return mediaType;
    }

    public @NotNull ColorScheme colorScheme() {
        return colorScheme;
    }

    public static class Builder {
        private @NotNull MediaType mediaType = DEFAULT_MEDIA_TYPE;
        private @NotNull ColorScheme colorScheme = DEFAULT_COLOR_SCHEME;

        public Builder() {}

        public @NotNull Builder mediaType(@NotNull CssHints.MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public @NotNull Builder colorScheme(@NotNull CssHints.ColorScheme colorScheme) {
            this.colorScheme = colorScheme;
            return this;
        }

        public @NotNull CssHints build() {
            return new CssHints(mediaType, colorScheme);
        }
    }
}
