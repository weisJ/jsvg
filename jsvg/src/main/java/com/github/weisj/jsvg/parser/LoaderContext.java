/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.impl.*;
import com.github.weisj.jsvg.parser.resources.ResourceLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;

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


        @NotNull
        LoaderContext build();
    }
}
