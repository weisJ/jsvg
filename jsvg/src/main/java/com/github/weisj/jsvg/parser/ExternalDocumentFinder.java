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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@ApiStatus.Experimental
class ExternalDocumentFinder implements DefaultElementLoader.DocumentFinder {

    private final @NotNull List<ResourceRoot> resourceRoots = new ArrayList<>();
    private final @NotNull Map<String, ParsedDocument> cache = new HashMap<>();

    @Override
    public @Nullable ParsedDocument resolveDocument(@NotNull ParsedDocument document, @NotNull String name) {
        if (name.isEmpty()) return document;
        return cache.computeIfAbsent(name, (p) -> locateDocument(document.loaderContext(), p));
    }

    public void addResourceRoot(@NotNull URI uri) {
        resourceRoots.add(new URIResourceRoot(uri));
    }

    private @Nullable ParsedDocument locateDocument(@NotNull LoaderContext context, @NotNull String name) {
        try {
            Optional<URL> url = resourceRoots.stream()
                    .map(root -> root.getResource(name))
                    .filter(Objects::nonNull)
                    .findFirst();
            if (!url.isPresent()) return null;
            SVGDocumentBuilder builder = new SVGLoader().loader()
                    .parse(SVGLoader.createDocumentInputStream(url.get().openStream()), context);
            if (builder == null) return null;
            return builder.parsedDocument();
        } catch (IOException | XMLStreamException e) {
            return null;
        }
    }

    private interface ResourceRoot {
        @Nullable
        URL getResource(@NotNull String path);
    }

    private static class URIResourceRoot implements ResourceRoot {
        private final @NotNull URI uri;

        public URIResourceRoot(@NotNull URI uri) {
            this.uri = uri;
        }

        @Override
        public @Nullable URL getResource(@NotNull String path) {
            try {
                return uri.resolve(path).toURL();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
