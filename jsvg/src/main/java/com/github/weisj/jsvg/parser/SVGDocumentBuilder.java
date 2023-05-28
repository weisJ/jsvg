/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.AttributeParser;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.Style;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.StyleSheet;

public final class SVGDocumentBuilder {

    private final Map<String, Object> namedElements = new HashMap<>();
    private final List<Style> styleElements = new ArrayList<>();
    private final List<StyleSheet> styleSheets = new ArrayList<>();
    private final Deque<ParsedElement> currentNodeStack = new ArrayDeque<>();

    private final @NotNull ParserProvider parserProvider;
    private final @NotNull LoadHelper loadHelper;
    private final @NotNull NodeSupplier nodeSupplier;

    private ParsedElement rootNode;

    public SVGDocumentBuilder(
            @NotNull ParserProvider parserProvider,
            @NotNull ResourceLoader resourceLoader,
            @NotNull NodeSupplier nodeSupplier) {
        this.parserProvider = parserProvider;
        this.loadHelper = new LoadHelper(new AttributeParser(parserProvider.createPaintParser()), resourceLoader);
        this.nodeSupplier = nodeSupplier;
    }

    public void startDocument() {
        if (rootNode != null) throw new IllegalStateException("Document already started");
    }

    public void endDocument() {
        if (rootNode == null) throw new IllegalStateException("Document is empty");
    }

    public boolean startElement(@NotNull String tagName, @NotNull Map<String, String> attributes) {
        ParsedElement parentElement = !currentNodeStack.isEmpty()
                ? currentNodeStack.peek()
                : null;
        AttributeNode parentAttributeNode = parentElement != null
                ? parentElement.attributeNode()
                : null;

        if (parentElement != null) flushText(parentElement, true);

        @Nullable SVGNode newNode = nodeSupplier.create(tagName);
        if (newNode == null) return false;

        AttributeNode attributeNode = new AttributeNode(tagName, attributes, parentAttributeNode,
                namedElements, styleSheets, loadHelper);
        String id = attributes.get("id");
        ParsedElement parsedElement = new ParsedElement(id, attributeNode, newNode);

        if (id != null && !namedElements.containsKey(id)) {
            namedElements.put(id, parsedElement);
        }

        if (parentElement != null) {
            parentElement.addChild(parsedElement);
        }
        if (rootNode == null) rootNode = parsedElement;

        if (parsedElement.node() instanceof Style) {
            styleElements.add((Style) parsedElement.node());
        }

        currentNodeStack.push(parsedElement);
        return true;
    }

    public void addTextContent(char @NotNull [] characterData, int startOffset, int endOffset) {
        if (currentNodeStack.isEmpty()) {
            throw new IllegalStateException("Adding text content without a current node");
        }
        ParsedElement currentElement = currentNodeStack.peek();
        if (currentElement.characterDataParser == null) return;
        currentElement.characterDataParser.append(characterData, startOffset, endOffset);
    }

    public void endElement(@NotNull String tagName) {
        if (currentNodeStack.isEmpty()) {
            throw new IllegalStateException("No current node to end");
        }
        ParsedElement currentElement = currentNodeStack.pop();
        String currentNodeTagName = currentElement.attributeNode().tagName();
        if (!currentNodeTagName.equals(tagName)) {
            throw new IllegalStateException(
                    String.format("Closing tag %s doesn't match current node %s)", tagName, currentNodeTagName));
        }
        flushText(currentElement, false);
    }

    private void flushText(@NotNull ParsedElement element, boolean segmentBreak) {
        if (element.characterDataParser != null && element.characterDataParser.canFlush(segmentBreak)) {
            element.node().addContent(element.characterDataParser.flush(segmentBreak));
        }
    }

    public @NotNull SVGDocument build() {
        if (rootNode == null) throw new IllegalStateException("No root node");

        if (!styleElements.isEmpty()) {
            CssParser cssParser = parserProvider.createCssParser();
            for (Style styleElement : styleElements) {
                styleElement.parseStyleSheet(cssParser);
                styleSheets.add(styleElement.styleSheet());
            }
        }

        DomProcessor preProcessor = parserProvider.createPreProcessor();
        if (preProcessor != null) preProcessor.process(rootNode);

        rootNode.build();

        DomProcessor postProcessor = parserProvider.createPostProcessor();
        if (postProcessor != null) postProcessor.process(rootNode);
        return new SVGDocument((SVG) rootNode.node());
    }
}
