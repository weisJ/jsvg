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
package com.github.weisj.jsvg.parser.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.paint.impl.DefaultPaintParser;
import com.github.weisj.jsvg.parser.*;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.impl.SimpleCssParser;
import com.github.weisj.jsvg.parser.resources.ResourceLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.parser.resources.impl.DefaultResourcePolicy;
import com.github.weisj.jsvg.parser.resources.impl.SynchronousResourceLoader;


public final class MutableLoaderContext implements LoaderContext, LoaderContext.Builder {
    private static final ResourceLoader DEFAULT_RESOURCE_LOADER = new SynchronousResourceLoader();
    private static final ElementLoader DEFAULT_ELEMENT_LOADER =
            new DefaultElementLoader(DefaultElementLoader.AllowExternalResources.DENY);
    private static final CssParser DEFAULT_CSS_PARSER = new SimpleCssParser();
    private static final PaintParser DEFAULT_PAINT_PARSER = new DefaultPaintParser();
    private @Nullable DomProcessor preProcessor = null;
    private @NotNull CssParser cssParser = DEFAULT_CSS_PARSER;
    private @NotNull PaintParser paintParser = DEFAULT_PAINT_PARSER;
    private @NotNull ResourceLoader resourceLoader = DEFAULT_RESOURCE_LOADER;
    private @NotNull ElementLoader elementLoader = DEFAULT_ELEMENT_LOADER;
    private @NotNull ResourcePolicy resourcePolicy = ResourcePolicy.DENY_EXTERNAL;
    private @NotNull DocumentLimits documentLimits = DocumentLimits.DEFAULT;

    public static @NotNull MutableLoaderContext createDefault() {
        return new MutableLoaderContext();
    }

    @Override
    public @Nullable DomProcessor preProcessor() {
        return preProcessor;
    }

    @Override
    public @NotNull CssParser cssParser() {
        return cssParser;
    }

    @Override
    public @NotNull PaintParser paintParser() {
        return paintParser;
    }

    @Override
    public @NotNull ResourceLoader resourceLoader() {
        return resourceLoader;
    }

    @Override
    public @NotNull ElementLoader elementLoader() {
        return elementLoader;
    }

    @Override
    public @NotNull ResourcePolicy externalResourcePolicy() {
        return resourcePolicy;
    }

    @Override
    public @NotNull DocumentLimits documentLimits() {
        return documentLimits;
    }

    @Override
    public @NotNull Builder preProcessor(@Nullable DomProcessor preProcessor) {
        this.preProcessor = preProcessor;
        return this;
    }

    @Override
    public @NotNull Builder cssParser(@NotNull CssParser cssParser) {
        this.cssParser = cssParser;
        return this;
    }

    @Override
    public @NotNull Builder paintParser(@NotNull PaintParser paintParser) {
        this.paintParser = paintParser;
        return this;
    }

    @Override
    public @NotNull Builder resourceLoader(@NotNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        return this;
    }

    @Override
    public @NotNull Builder elementLoader(@NotNull ElementLoader elementLoader) {
        this.elementLoader = elementLoader;
        return this;
    }

    @Override
    public @NotNull Builder externalResourcePolicy(@NotNull ResourcePolicy policy) {
        this.resourcePolicy = policy;
        return this;
    }

    @Override
    public @NotNull Builder documentLimits(@NotNull DocumentLimits documentLimits) {
        this.documentLimits = documentLimits;
        return this;
    }

    @Override
    public @NotNull LoaderContext build() {
        // Check if policy changed. This avoids instantiating the heavier external loader.
        if (!(this.resourcePolicy instanceof DefaultResourcePolicy)
                || ((DefaultResourcePolicy) this.resourcePolicy).allowsExternalResources()) {
            this.elementLoader = new DefaultElementLoader(DefaultElementLoader.AllowExternalResources.ALLOW);
        }
        return this;
    }
}
