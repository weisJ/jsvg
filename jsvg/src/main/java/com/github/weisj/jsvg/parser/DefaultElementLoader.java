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

public class DefaultElementLoader implements ElementLoader {

    @Override
    public <T> @Nullable T loadElement(@NotNull Class<T> type, @Nullable String value,
            @NotNull ParsedDocument document,
            @NotNull AttributeParser attributeParser) {
        String url = attributeParser.parseUrl(value);
        if (url == null) return null;
        if (url.contains("#")) {
            String[] parts = url.split("#", 2);
            ParsedDocument parsedDocument = resolveDocument(document, parts[0]);
            if (parsedDocument == null) return null;
            return getElementById(parsedDocument, type, parts[1]);
        }
        return getElementById(document, type, url);
    }

    private @Nullable ParsedDocument resolveDocument(@NotNull ParsedDocument document, @NotNull String name) {
        if (name.isEmpty()) return document;
        // Document references are not supported
        return null;
    }

    private <T> @Nullable T getElementById(@NotNull ParsedDocument document, @NotNull Class<T> type,
            @Nullable String id) {
        if (id == null) return null;
        // Todo: Look up in spec how elements should be resolved if multiple elements have the same id.
        Object node = document.getNamedElement(id);
        if (node instanceof ParsedElement) {
            node = ((ParsedElement) node).nodeEnsuringBuildStatus();
        }
        return type.isInstance(node) ? type.cast(node) : null;
    }
}
