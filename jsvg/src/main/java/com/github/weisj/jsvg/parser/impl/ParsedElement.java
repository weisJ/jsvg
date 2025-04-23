/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.DomElement;

public final class ParsedElement implements DomElement {

    private enum BuildStatus {
        NOT_BUILT,
        IN_PROGRESS,
        FINISHED
    }

    private final @Nullable String id;
    private final @NotNull ParsedDocument document;
    private final @Nullable ParsedElement parent;
    private final @NotNull AttributeNode attributeNode;
    private final @NotNull SVGNode node;

    private final @NotNull List<@NotNull ParsedElement> children = new ArrayList<>();
    private final @NotNull List<@NotNull ParsedElement> indirectChildren = new ArrayList<>();
    private final @NotNull Map<String, @NotNull List<@NotNull ParsedElement>> animationElements = new HashMap<>();
    final CharacterDataParser characterDataParser;
    private @NotNull BuildStatus buildStatus = BuildStatus.NOT_BUILT;
    private int outgoingPaths = -1;

    ParsedElement(@Nullable String id, @NotNull ParsedDocument document,
            @Nullable ParsedElement parent, @NotNull AttributeNode element,
            @NotNull SVGNode node) {
        this.document = document;
        this.parent = parent;
        this.attributeNode = element;
        this.node = node;
        this.id = id;
        PermittedContent permittedContent = node.getClass().getAnnotation(PermittedContent.class);
        if (permittedContent == null) {
            throw new IllegalStateException("Element <" + node.tagName() + "> doesn't specify permitted content");
        }
        if (permittedContent.charData()) {
            characterDataParser = new CharacterDataParser();
        } else {
            characterDataParser = null;
        }
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    @Override
    public @NotNull String tagName() {
        return attributeNode.tagName();
    }

    @Override
    public @NotNull List<@NotNull String> classNames() {
        return attributeNode.classNames();
    }

    @Override
    public @NotNull ParsedDocument document() {
        return document;
    }

    @Override
    public @NotNull List<ParsedElement> children() {
        return children;
    }

    @Override
    public @Nullable String attribute(@NotNull String name) {
        return attributeNode.getValue(name);
    }

    @Override
    public void setAttribute(@NotNull String name, @Nullable String value) {
        if (value == null) {
            attributeNode.attributes().remove(name);
        } else {
            attributeNode.attributes().put(name, value);
        }
    }

    public @NotNull Map<String, List<ParsedElement>> animationElements() {
        return animationElements;
    }

    @Override
    public @Nullable ParsedElement parent() {
        return parent;
    }

    public @NotNull SVGNode node() {
        return node;
    }

    public @NotNull SVGNode nodeEnsuringBuildStatus(int depth) {
        if (buildStatus == BuildStatus.IN_PROGRESS) {
            cyclicDependencyDetected();
        } else if (buildStatus == BuildStatus.NOT_BUILT) {
            build(depth);
        }
        return node;
    }

    public @NotNull AttributeNode attributeNode() {
        return attributeNode;
    }

    void addChild(@NotNull ParsedElement parsedElement) {
        if (Category.hasCategory(parsedElement.node, Category.Animation)) {
            String attributeName = BaseAnimationNode.attributeName(parsedElement.attributeNode());
            animationElements.computeIfAbsent(attributeName, k -> new ArrayList<>()).add(parsedElement);
        }
        children.add(parsedElement);
        if (node instanceof Container) {
            ((Container<?>) node).addChild(parsedElement.id, parsedElement.node);
        }
    }

    void addIndirectChild(@NotNull ParsedElement parsedElement) {
        indirectChildren.add(parsedElement);
    }

    void build(int depth) {
        if (buildStatus == BuildStatus.FINISHED) return;
        if (buildStatus == BuildStatus.IN_PROGRESS) {
            cyclicDependencyDetected();
            return;
        }
        buildStatus = BuildStatus.IN_PROGRESS;

        int maxNestingDepth = attributeNode.document().loaderContext().documentLimits().maxNestingDepth();
        if (depth > maxNestingDepth) {
            throw new IllegalStateException(
                    String.format("Maximum nesting depth reached %d > %d.%n", depth, maxNestingDepth)
                            + "Note: You can configure this using LoaderContext#documentLimits()");
        }

        attributeNode.prepareForNodeBuilding();

        // Build depth first to ensure child nodes are processed first.
        // e.g. LinearGradient depends on its stops to be build first.
        for (ParsedElement child : children) {
            child.build(depth + 1);
        }

        document().setCurrentNestingDepth(depth);
        node.build(attributeNode);
        buildStatus = BuildStatus.FINISHED;
    }

    /*
     * Returns the number of outgoing paths from this node terminating in a leaf node.
     */
    int outgoingPaths() {
        if (outgoingPaths == -1) {
            outgoingPaths = 0;
            for (ParsedElement child : children) {
                if (child.node instanceof Renderable) {
                    outgoingPaths += child.outgoingPaths();
                }
            }

            for (ParsedElement child : indirectChildren) {
                outgoingPaths += child.outgoingPaths();
            }

            outgoingPaths = Math.max(outgoingPaths, 1);
        }
        return outgoingPaths;
    }

    @Override
    public String toString() {
        return "ParsedElement{" + "node=" + node + '}';
    }

    private void cyclicDependencyDetected() {
        throw new IllegalStateException("Cyclic dependency involving node '" + id + "' detected.");
    }
}
