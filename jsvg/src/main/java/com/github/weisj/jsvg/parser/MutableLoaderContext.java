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


final class MutableLoaderContext implements LoaderContext, LoaderContext.Builder {
    private static final ParserProvider DEFAULT_PARSER_PROVIDER = new DefaultParserProvider();
    private static final ResourceLoader DEFAULT_RESOURCE_LOADER = new SynchronousResourceLoader();
    private static final ElementLoader DEFAULT_ELEMENT_LOADER =
            new DefaultElementLoader(DefaultElementLoader.AllowExternalResources.DENY);
    private @NotNull ParserProvider parserProvider = DEFAULT_PARSER_PROVIDER;
    private @NotNull ResourceLoader resourceLoader = DEFAULT_RESOURCE_LOADER;
    private @NotNull ElementLoader elementLoader = DEFAULT_ELEMENT_LOADER;
    private @NotNull ExternalResourcePolicy externalResourcePolicy = ExternalResourcePolicy.DENY;
    private @NotNull DocumentLimits documentLimits = DocumentLimits.DEFAULT;

    static @NotNull MutableLoaderContext createDefault() {
        return new MutableLoaderContext();
    }

    @Override
    public @NotNull ParserProvider parserProvider() {
        return parserProvider;
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
    public @NotNull ExternalResourcePolicy externalResourcePolicy() {
        return externalResourcePolicy;
    }

    @Override
    public @NotNull DocumentLimits documentLimits() {
        return documentLimits;
    }

    @Override
    public @NotNull Builder parserProvider(@NotNull ParserProvider parserProvider) {
        this.parserProvider = parserProvider;
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
    public @NotNull Builder externalResourcePolicy(@NotNull ExternalResourcePolicy policy) {
        this.externalResourcePolicy = policy;
        return this;
    }

    @Override
    public @NotNull Builder documentLimits(@NotNull DocumentLimits documentLimits) {
        this.documentLimits = documentLimits;
        return this;
    }

    @Override
    public @NotNull LoaderContext build() {
        // Check if policy changed. This avoid instantiating the heavier external loader.
        if (this.externalResourcePolicy != ExternalResourcePolicy.DENY) {
            this.elementLoader = new DefaultElementLoader(DefaultElementLoader.AllowExternalResources.ALLOW);
        }
        return this;
    }
}
