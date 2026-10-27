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
package com.github.weisj.jsvg.parser;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.impl.*;
import com.github.weisj.jsvg.parser.resources.ResourceLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.CssHints;

/**
 * The context providing all necessary components for loading an SVG document.
 * The context returned by {@link #createDefault()} can be used across multiple documents and threads.
 */
public interface LoaderContext {
    @Nullable
    DomProcessor preProcessor();

    @NotNull
    CssParser cssParser();

    @NotNull
    PaintParser paintParser();

    @NotNull
    ResourceLoader resourceLoader();

    @NotNull
    ElementLoader elementLoader();

    @NotNull
    ResourcePolicy externalResourcePolicy();

    @NotNull
    DocumentLimits documentLimits();

    @ApiStatus.Experimental
    @NotNull
    CssHints cssHints();

    /**
     * An additional stylesheet applied to every loaded document, or {@code null} if none is set.
     * It is parsed with {@link #cssParser()} and {@link #cssHints()} for each document.
     *
     * @see Builder#externalStyleSheet(String)
     */
    @ApiStatus.Experimental
    @Nullable
    String externalStyleSheet();

    static @NotNull Builder builder() {
        return MutableLoaderContext.createDefault();
    }

    static @NotNull LoaderContext createDefault() {
        return builder().build();
    }

    interface Builder {

        @NotNull
        Builder preProcessor(@Nullable DomProcessor preProcessor);

        @NotNull
        Builder cssParser(@NotNull CssParser cssParser);

        @NotNull
        Builder paintParser(@NotNull PaintParser paintParser);

        @NotNull
        Builder resourceLoader(@NotNull ResourceLoader resourceLoader);

        @NotNull
        Builder elementLoader(@NotNull ElementLoader elementLoader);

        @NotNull
        Builder externalResourcePolicy(@NotNull ResourcePolicy policy);

        @NotNull
        Builder documentLimits(@NotNull DocumentLimits documentLimits);

        @ApiStatus.Experimental
        @NotNull
        Builder cssHints(@NotNull CssHints cssHints);

        /**
         * Sets CSS text that is applied to every loaded document in addition to its own {@code <style>} elements.
         * The rules are placed after all rules of the document, so on equal specificity (and importance) they win
         * over the document's rules. Inline {@code style} attributes still take precedence over non-important rules.
         *
         * @param css the stylesheet source, or {@code null} to remove a previously set stylesheet.
         */
        @ApiStatus.Experimental
        @NotNull
        Builder externalStyleSheet(@Nullable String css);

        @NotNull
        LoaderContext build();
    }
}
