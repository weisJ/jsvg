/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

public final class ParsedElement {

    private enum BuildStatus {
        NOT_BUILT,
        IN_PROGRESS,
        FINISHED
    }

    private final @Nullable String id;
    private final @NotNull AttributeNode attributeNode;
    private final @NotNull SVGNode node;

    private final @NotNull List<@NotNull ParsedElement> children = new ArrayList<>();
    private final @NotNull List<@NotNull ParsedElement> animationElements = new ArrayList<>();
    final CharacterDataParser characterDataParser;
    private @NotNull BuildStatus buildStatus = BuildStatus.NOT_BUILT;

    ParsedElement(@Nullable String id, @NotNull AttributeNode element, @NotNull SVGNode node) {
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

    public void registerNamedElement(@NotNull String name, @NotNull Object element) {
        attributeNode.document().registerNamedElement(name, element);
    }

    public @Nullable String id() {
        return id;
    }

    public @NotNull List<ParsedElement> children() {
        return children;
    }

    public @NotNull SVGNode node() {
        return node;
    }

    public @NotNull SVGNode nodeEnsuringBuildStatus() {
        if (buildStatus == BuildStatus.IN_PROGRESS) {
            cyclicDependencyDetected();
        } else if (buildStatus == BuildStatus.NOT_BUILT) {
            build();
        }
        return node;
    }

    public @NotNull AttributeNode attributeNode() {
        return attributeNode;
    }

    void addChild(ParsedElement parsedElement) {
        if (Category.hasCategory(parsedElement.node, Category.Animation)) {
            animationElements.add(parsedElement);
        }
        children.add(parsedElement);
        if (node instanceof Container) {
            ((Container<?>) node).addChild(parsedElement.id, parsedElement.node);
        }
    }

    void build() {
        if (buildStatus == BuildStatus.FINISHED) return;
        if (buildStatus == BuildStatus.IN_PROGRESS) {
            cyclicDependencyDetected();
            return;
        }
        buildStatus = BuildStatus.IN_PROGRESS;

        attributeNode.prepareForNodeBuilding(this);

        // Build depth first to ensure child nodes are processed first.
        // e.g. LinearGradient depends on its stops to be build first.
        for (ParsedElement child : children) {
            child.build();
        }
        node.build(attributeNode);
        buildStatus = BuildStatus.FINISHED;
    }

    @Override
    public String toString() {
        return "ParsedElement{" + "node=" + node + '}';
    }

    private void cyclicDependencyDetected() {
        throw new IllegalStateException("Cyclic dependency involving node '" + id + "' detected.");
    }
}
