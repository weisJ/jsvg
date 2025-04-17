/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.impl.*;

/**
 * Class for loading svg files as an {@link SVGDocument}.
 * Note that this class isn't guaranteed to be thread safe and hence shouldn't be used across multiple threads.
 */
public final class SVGLoader {

    static final Logger LOGGER = Logger.getLogger(SVGLoader.class.getName());
    private static final @NotNull NodeSupplier NODE_SUPPLIER = new NodeSupplier();
    private final StaxSVGLoader loader = new StaxSVGLoader(NODE_SUPPLIER);

    public @Nullable SVGDocument load(@NotNull URL xmlBase) {
        return load(xmlBase, new DefaultParserProvider());
    }


    public @Nullable SVGDocument load(@NotNull URL xmlBase, @NotNull ParserProvider parserProvider) {
        return load(xmlBase, LoaderContext.builder()
                .parserProvider(parserProvider)
                .build());
    }

    public @Nullable SVGDocument load(@NotNull URL xmlBase, @NotNull LoaderContext loaderContext) {
        try {
            URI uri = xmlBase.toURI();
            return load(xmlBase.openStream(), uri, loaderContext);
        } catch (URISyntaxException | IOException e) {
            LOGGER.log(Level.WARNING, String.format("Could not read %s", xmlBase), e);
        }
        return null;
    }

    /**
     * Load an SVG document from the given input stream.
     *
     * @param inputStream the input stream to read the SVG document from
     * @param xmlBase The uri of the document. This is used to resolve external documents (if enabled).
     * @param loaderContext The loader context to use for loading the document.
     * @return The loaded SVG document or null if an error occurred.
     */
    public @Nullable SVGDocument load(@NotNull InputStream inputStream, @Nullable URI xmlBase,
            @NotNull LoaderContext loaderContext) {
        try (InputStream is = StreamUtil.createDocumentInputStream(inputStream)) {
            return loader.load(is, xmlBase, loaderContext);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load SVG ", e);
        }
        return null;
    }
}
