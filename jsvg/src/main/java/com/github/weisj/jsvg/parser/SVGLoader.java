/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
import java.util.zip.GZIPInputStream;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;

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
            LOGGER.log(Level.WARNING, "Could not read " + xmlBase, e);
        }
        return null;
    }

    /**
     * @deprecated use {@link #load(InputStream, URI, LoaderContext)} instead
     */
    @Deprecated
    public @Nullable SVGDocument load(@NotNull InputStream inputStream) {
        return load(inputStream, new DefaultParserProvider());
    }

    /**
     * @deprecated use {@link #load(InputStream, URI, LoaderContext)} instead
     */
    @Deprecated
    public @Nullable SVGDocument load(@NotNull InputStream inputStream, @NotNull ParserProvider parserProvider) {
        return load(inputStream, null, LoaderContext.builder()
                .parserProvider(parserProvider)
                .build());
    }

    /**
     * @deprecated use {@link #load(InputStream, URI, LoaderContext)} instead
     */
    @Deprecated
    public @Nullable SVGDocument load(@NotNull InputStream inputStream,
            @NotNull ParserProvider parserProvider,
            @NotNull ResourceLoader resourceLoader) {
        return load(inputStream, null, LoaderContext.builder()
                .parserProvider(parserProvider)
                .resourceLoader(resourceLoader)
                .build());
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
        try {
            return loader.load(createDocumentInputStream(inputStream), xmlBase, loaderContext);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Could not load SVG ", e);
        }
        return null;
    }

    @ApiStatus.Internal
    StaxSVGLoader loader() {
        return loader;
    }

    static @Nullable InputStream createDocumentInputStream(@NotNull InputStream is) {
        try {
            BufferedInputStream bin = new BufferedInputStream(is);
            bin.mark(2);
            int b0 = bin.read();
            int b1 = bin.read();
            bin.reset();

            // Check for gzip magic number
            if ((b1 << 8 | b0) == GZIPInputStream.GZIP_MAGIC) {
                return new GZIPInputStream(bin);
            } else {
                // Plain text
                return bin;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }
}
