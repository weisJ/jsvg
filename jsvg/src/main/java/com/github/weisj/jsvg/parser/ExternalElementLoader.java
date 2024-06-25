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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.AttributeParser;

@ApiStatus.Experimental
public class ExternalElementLoader implements ElementLoader {

    private final @NotNull DefaultElementLoader loader = new DefaultElementLoader();
    private final @NotNull ExternalDocumentFinder externalDocumentFinder = new ExternalDocumentFinder();

    public ExternalElementLoader() {
        loader.setDocumentFinder(externalDocumentFinder);
    }

    @Override
    public <T> @Nullable T loadElement(@NotNull Class<T> type, @Nullable String value, @NotNull ParsedDocument document,
            @NotNull AttributeParser attributeParser) {
        return loader.loadElement(type, value, document, attributeParser);
    }

    public @NotNull ExternalElementLoader withResourceRoots(@NotNull Class<?>... clazzes) {
        for (Class<?> c : clazzes) {
            externalDocumentFinder.resourceRoots.add(new ClassResourceRoot(c));
        }
        return this;
    }

    public @NotNull ExternalElementLoader withResourceRoots(@NotNull ClassLoader... classLoaders) {
        for (ClassLoader cl : classLoaders) {
            externalDocumentFinder.resourceRoots.add(new ClassLoaderResourceRoot(cl));
        }
        return this;
    }

    public @NotNull ExternalElementLoader withResourceRoots(@NotNull Path... roots) {
        for (Path root : roots) {
            externalDocumentFinder.resourceRoots.add(new FileSystemResourceRoot(root));
        }
        return this;
    }

    private static class ExternalDocumentFinder implements DefaultElementLoader.DocumentFinder {
        private final @NotNull List<@NotNull ResourceRoot> resourceRoots = new ArrayList<>();
        private final @NotNull Map<String, ParsedDocument> cache = new HashMap<>();

        @Override
        public @Nullable ParsedDocument resolveDocument(@NotNull ParsedDocument document, @NotNull String name) {
            if (name.isEmpty()) return document;
            return cache.computeIfAbsent(name, (p) -> locateDocument(document.loaderContext(), p));
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
    }

    abstract static class ResourceRoot {
        abstract @Nullable URL getResource(@NotNull String path);
    }

    final static class ClassResourceRoot extends ResourceRoot {
        private final @NotNull Class<?> clazz;

        ClassResourceRoot(@NotNull Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        @Nullable
        URL getResource(@NotNull String path) {
            return clazz.getResource(path);
        }
    }

    final static class ClassLoaderResourceRoot extends ResourceRoot {
        private final @NotNull ClassLoader classLoader;

        ClassLoaderResourceRoot(@NotNull ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        @Nullable
        URL getResource(@NotNull String path) {
            return classLoader.getResource(path);
        }
    }

    final static class FileSystemResourceRoot extends ResourceRoot {
        private final Path root;

        FileSystemResourceRoot(@NotNull Path root) {
            this.root = root;
        }

        @Override
        @Nullable
        URL getResource(@NotNull String path) {
            try {
                return root.resolve(path).toUri().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }
}
