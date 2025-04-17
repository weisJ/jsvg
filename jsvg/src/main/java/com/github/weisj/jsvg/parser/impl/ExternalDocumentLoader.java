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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.RawDocument;


@ApiStatus.Experimental
class ExternalDocumentLoader implements DefaultElementLoader.DocumentLoader {
    private static final Logger LOGGER = Logger.getLogger(ExternalDocumentLoader.class.getName());

    private final @NotNull Map<URI, CachedDocument> cache = new HashMap<>();

    @Override
    public @Nullable RawDocument resolveDocument(@NotNull RawDocument document, @NotNull String name) {
        if (name.isEmpty()) return document;
        return locateDocument(document, name);
    }

    private @Nullable RawDocument locateDocument(@NotNull RawDocument document, @NotNull String name) {
        URI documentUri = document
                .loaderContext()
                .externalResourcePolicy()
                .resolveResourceURI(document.rootURI(), name);
        if (documentUri == null) return null;

        try {
            URL documentUrl = documentUri.toURL();
            synchronized (cache) {
                CachedDocument cachedDocument = cache.get(documentUri);
                if (cachedDocument != null) {
                    ParsedDocument cached = cachedDocument.document;
                    if (cached == null) {
                        throw new IllegalStateException("Reference cycle containing external document: " + documentUri);
                    }
                    return cached;
                }
            }
            CachedDocument cachedDocument = new CachedDocument();
            synchronized (cache) {
                cache.put(documentUri, cachedDocument);
            }

            try (InputStream is = StreamUtil.createDocumentInputStream(documentUrl.openStream())) {
                SVGDocumentBuilder builder = new StaxSVGLoader().parse(
                        is, documentUri, document.loaderContext());

                if (builder == null) return null;
                builder.preProcess(documentUri);

                ParsedDocument parsedDocument = builder.parsedDocument();
                synchronized (cache) {
                    cachedDocument.document = parsedDocument;
                }
                return parsedDocument;
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Failed to load external document: %s from %s - %s",
                    name, documentUri, e.getMessage()));
            return null;
        }
    }

    private static final class CachedDocument {
        private @Nullable ParsedDocument document;
    }
}
