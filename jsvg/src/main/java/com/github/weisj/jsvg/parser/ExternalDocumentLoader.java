/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@ApiStatus.Experimental
class ExternalDocumentLoader implements DefaultElementLoader.DocumentLoader {
    private static final Logger LOGGER = Logger.getLogger(ExternalDocumentLoader.class.getName());

    private final @NotNull Map<URI, ParsedDocument> cache = new HashMap<>();
    private final @NotNull ElementLoader.ExternalDocumentPolicy policy;

    ExternalDocumentLoader(@NotNull ElementLoader.ExternalDocumentPolicy policy) {
        this.policy = policy;
    }

    @Override
    public @Nullable ParsedDocument resolveDocument(@NotNull ParsedDocument document, @NotNull String name) {
        if (name.isEmpty()) return document;
        return locateDocument(document, name);
    }

    private @Nullable ParsedDocument locateDocument(@NotNull ParsedDocument document, @NotNull String name) {
        URI root = document.rootURI();
        if (root == null) return null;
        try {
            URI documentUri = policy.resolveDocumentURI(root, name);
            if (documentUri == null) return null;

            URL documentUrl = documentUri.toURL();
            synchronized (cache) {
                ParsedDocument cachedDocument = cache.get(documentUri);
                if (cachedDocument != null) return cachedDocument;

                SVGDocumentBuilder builder = new SVGLoader().loader().parse(
                        SVGLoader.createDocumentInputStream(documentUrl.openStream()),
                        documentUri,
                        document.loaderContext());
                if (builder == null) return null;
                builder.preProcess(documentUri);

                ParsedDocument parsedDocument = builder.parsedDocument();
                cache.put(documentUri, parsedDocument);
                return parsedDocument;
            }
        } catch (Exception e) {
            LOGGER.warning(
                    String.format("Failed to load external document: %s from %s - %s", name, root, e.getMessage()));
            return null;
        }
    }
}
