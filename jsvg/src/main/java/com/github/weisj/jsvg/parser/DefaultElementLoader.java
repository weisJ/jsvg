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


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.AttributeParser;

class DefaultElementLoader implements ElementLoader {

    private static final DocumentLoader DEFAULT_DOCUMENT_LOADER = new DefaultDocumentLoader();
    private final DocumentLoader documentLoader;

    enum AllowExternalResources {
        DENY,
        ALLOW
    }

    DefaultElementLoader(AllowExternalResources allowExternalResources) {
        documentLoader = createDocumentLoader(allowExternalResources);
    }

    private static @NotNull DocumentLoader createDocumentLoader(AllowExternalResources allowExternalResources) {
        if (allowExternalResources == AllowExternalResources.DENY) return DEFAULT_DOCUMENT_LOADER;
        return new ExternalDocumentLoader();
    }

    @Override
    public <T> @Nullable T loadElement(@NotNull Class<T> type, @Nullable String value,
            @NotNull ParsedDocument document, @NotNull AttributeParser attributeParser) {
        String url = attributeParser.parseUrl(value);
        if (url == null) return null;
        if (url.contains("#")) {
            String[] parts = url.split("#", 2);
            ParsedDocument parsedDocument = documentLoader.resolveDocument(document, parts[0]);
            if (parsedDocument == null) return null;
            return parsedDocument.getElementById(type, parts[1]);
        }
        return document.getElementById(type, url);
    }

    interface DocumentLoader {
        @Nullable
        ParsedDocument resolveDocument(@NotNull ParsedDocument document, @NotNull String name);
    }

    private static class DefaultDocumentLoader implements DocumentLoader {
        @Override
        public @Nullable ParsedDocument resolveDocument(@NotNull ParsedDocument document, @NotNull String name) {
            if (name.isEmpty()) return document;
            return null;
        }
    }
}
