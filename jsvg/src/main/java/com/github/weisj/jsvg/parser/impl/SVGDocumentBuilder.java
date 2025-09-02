/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jannis Weis
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

import java.net.URI;
import java.util.*;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.nodes.*;
import com.github.weisj.jsvg.nodes.container.CommonRenderableContainerNode;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.StyleSheet;

public final class SVGDocumentBuilder {
    private final @NotNull ParsedDocument parsedDocument;
    private final @NotNull List<@NotNull Use> useElements = new ArrayList<>();
    private final @NotNull List<@NotNull ParsedElement> styleElements = new ArrayList<>();
    private final @NotNull List<@NotNull StyleSheet> styleSheets = new ArrayList<>();
    private final @NotNull Deque<@NotNull ParsedElement> currentNodeStack = new ArrayDeque<>();

    private final @NotNull LoaderContext loaderContext;
    private final @NotNull NodeSupplier nodeSupplier;

    private ParsedElement rootNode;

    public SVGDocumentBuilder(
            @Nullable URI rootURI,
            @NotNull LoaderContext loaderContext,
            @NotNull NodeSupplier nodeSupplier) {
        LoadHelper loadHelper = new LoadHelper(
                new AttributeParser(loaderContext.paintParser()),
                loaderContext);
        this.loaderContext = loaderContext;
        this.nodeSupplier = nodeSupplier;
        this.parsedDocument = new ParsedDocument(rootURI, loaderContext, loadHelper);
    }

    @ApiStatus.Internal
    @NotNull
    ParsedDocument parsedDocument() {
        return parsedDocument;
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

        if (parentElement != null) flushText(parentElement, true);

        @Nullable SVGNode newNode = nodeSupplier.create(tagName);
        if (newNode == null) return false;

        AttributeNode attributeNode = new AttributeNode(tagName, attributes, styleSheets);
        String id = attributes.get("id");
        ParsedElement parsedElement = new ParsedElement(id, parsedDocument, parentElement, attributeNode, newNode);
        attributeNode.setElement(parsedElement);

        if (id != null && !parsedDocument.hasElementWithId(id)) {
            parsedDocument.registerNamedElement(id, parsedElement);
        }

        if (parentElement != null) {
            parentElement.addChild(parsedElement);
        }
        if (rootNode == null) rootNode = parsedElement;

        if (parsedElement.node() instanceof Style) {
            styleElements.add(parsedElement);
        }

        if (parsedElement.node() instanceof Use) {
            useElements.add((Use) parsedElement.node());
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
            element.textContent().currentContentList().add(element.characterDataParser.flush(segmentBreak));
        }
    }

    void preProcess() {
        if (rootNode == null) throw new IllegalStateException("No root node");

        DomProcessor preProcessor = loaderContext.preProcessor();
        if (preProcessor != null) preProcessor.process(rootNode);
    }

    public @NotNull SVGDocument build() {
        preProcess();
        processStyleSheets();
        rootNode.build(0);
        validatePathCount();
        validateUseElementsDepth();
        return DocumentConstructorAccessor.constructor().create((SVG) rootNode.node());
    }

    private void processStyleSheets() {
        if (styleElements.isEmpty()) return;
        CssParser cssParser = loaderContext.cssParser();
        for (ParsedElement styleElement : styleElements) {
            styleElement.build(0);
            Style styleNode = (Style) styleElement.node();
            styleNode.parseStyleSheet(cssParser);
            styleSheets.add(styleNode.styleSheet());
        }
    }

    private void validatePathCount() {
        int pathCount = rootNode.outgoingPaths();
        int maxPathCount = parsedDocument.loaderContext().documentLimits().maxPathCount();
        if (pathCount > maxPathCount) {
            throw new IllegalStateException(
                    String.format("Maximum count of rendered element instances exceeded %d > %d.%n",
                            pathCount, maxPathCount)
                            + "Note: You can configure this using LoaderContext#documentLimits()");
        }
    }

    private void validateUseElementsDepth() {
        if (useElements.isEmpty()) return;
        Map<SVGNode, Integer> checkedNodes = new HashMap<>();
        int useNestingLimit = parsedDocument.loaderContext().documentLimits().maxUseNestingDepth();
        for (Use useElement : useElements) {
            int depth = nestingDepthOf(useElement, checkedNodes);
            if (depth > useNestingLimit) {
                throw new IllegalStateException(String.format(
                        "Maximum nesting depth for <use> exceeded %d > %d starting from node with id '%s'%n",
                        depth, useNestingLimit, useElement.id())
                        + "Note: You can configure this using LoaderContext#documentLimits()");
            }
        }
    }

    private int nestingDepthOf(@NotNull SVGNode node, @NotNull Map<SVGNode, Integer> checkedNodes) {
        int cached = checkedNodes.getOrDefault(node, -1);
        if (cached >= 0) return cached;

        int depth = 0;
        if (node instanceof Use) {
            SVGNode referenced = ((Use) node).referencedNode();
            if (referenced != null) {
                depth = nestingDepthOf(referenced, checkedNodes) + 1;
            }
        } else if (node instanceof CommonRenderableContainerNode) {
            for (SVGNode child : ((CommonRenderableContainerNode) node).children()) {
                depth = Math.max(depth, nestingDepthOf(child, checkedNodes));
            }
        }
        checkedNodes.put(node, depth);

        return depth;
    }
}
