/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jannis Weis
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

import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.Rect;
import com.github.weisj.jsvg.paint.impl.DefaultPaintParser;
import com.github.weisj.jsvg.parser.LoaderContext;

public final class ParserTestUtil {

    private static final AttributeParser ATTRIBUTE_PARSER = new AttributeParser(new DefaultPaintParser());
    private static final LoadHelper LOAD_HELPER = new LoadHelper(ATTRIBUTE_PARSER, LoaderContext.builder().build());

    private ParserTestUtil() {}

    public static @NotNull AttributeNode createDummyAttributeNode(@NotNull Map<String, String> attrs) {
        ParsedDocument document = new ParsedDocument(null, LoaderContext.createDefault());
        AttributeNode attributeNode = new AttributeNode("dummy", attrs, Collections.emptyList(), LOAD_HELPER);
        attributeNode.setElement(new ParsedElement(null, document, null, attributeNode, new Rect()));
        return attributeNode;
    }
}
