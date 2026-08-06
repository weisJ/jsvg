/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.Use;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.parser.TextContent;

public final class ParsedElement implements DomElement {
    private static final Logger LOGGER = LogFactory.createLogger(ParsedElement.class);

    private enum BuildStatus {
        NOT_BUILT,
        IN_PROGRESS,
        FINISHED
    }

    private final @Nullable String id;
    private final @NotNull ParsedDocument document;
    private final @Nullable ParsedElement parent;
    private final int oneBasedIndexInParent;
    private final int oneBasedIndexAmongSiblingsWithSameTagName;
    private final @NotNull AttributeNode attributeNode;
    private final @NotNull SVGNode node;

    private final @NotNull List<@NotNull ParsedElement> children = new ArrayList<>();
    private final @NotNull Map<String, Integer> childCountsByTagName = new HashMap<>();
    private final @NotNull List<@NotNull ParsedElement> indirectChildren = new ArrayList<>();
    private final @NotNull Map<String, @NotNull List<@NotNull ParsedElement>> animationElements = new HashMap<>();
    private TextContentImpl textContent = null;

    final CharacterDataParser characterDataParser;
    private @NotNull BuildStatus buildStatus = BuildStatus.NOT_BUILT;
    private boolean partOfCycle = false;
    private int outgoingPaths = -1;

    ParsedElement(@Nullable String id,
            @NotNull ParsedDocument document,
            @Nullable ParsedElement parent,
            int oneBasedIndexInParent,
            int oneBasedIndexAmongSiblingsWithSameTagName,
            @NotNull AttributeNode element,
            @NotNull SVGNode node) {
        this.document = document;
        this.parent = parent;
        this.oneBasedIndexInParent = oneBasedIndexInParent;
        this.oneBasedIndexAmongSiblingsWithSameTagName = oneBasedIndexAmongSiblingsWithSameTagName;
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
        return attributeNode.declaredAttributes().get(name);
    }

    @Override
    public void setAttribute(@NotNull String name, @Nullable String value) {
        // Write the raw declared attribute; prepareForNodeBuilding tokenizes, expands and cascades it (like
        // parsed input).
        if (value == null) {
            attributeNode.declaredAttributes().remove(name);
        } else {
            attributeNode.declaredAttributes().put(name, value);
        }
    }

    @Override
    public @NotNull TextContentImpl textContent() {
        if (textContent == null) {
            textContent = new TextContentImpl(this);
        }
        return textContent;
    }

    public @NotNull Map<String, List<ParsedElement>> animationElements() {
        return animationElements;
    }

    @Override
    public @Nullable ParsedElement parent() {
        return parent;
    }

    public int oneBasedIndexInParent() {
        return oneBasedIndexInParent;
    }

    public int oneBasedIndexAmongSiblingsWithSameTagName() {
        return oneBasedIndexAmongSiblingsWithSameTagName;
    }

    /** Number of element children with the given tag name. */
    public int childCountWithTagName(@NotNull String tagName) {
        return childCountsByTagName.getOrDefault(tagName, 0);
    }

    /** Whether this element contains any non-whitespace text (for the {@code :empty} pseudo-class). */
    public boolean hasNonWhitespaceText() {
        return textContent != null && textContent.hasNonWhitespaceText();
    }

    /** Preceding element sibling, or null if first child */
    public @Nullable ParsedElement previousSibling() {
        ParsedElement parent = parent();
        if (parent == null) {
            return null;
        }
        int previousZeroBasedIndex = oneBasedIndexInParent() - 2;
        if (previousZeroBasedIndex < 0) {
            return null;
        }
        return parent.children().get(previousZeroBasedIndex);
    }

    public @NotNull SVGNode node() {
        return node;
    }

    /** Returns null if resolving the node would close a reference cycle. */
    public @Nullable SVGNode nodeEnsuringBuildStatus(int depth) {
        if (buildStatus == BuildStatus.IN_PROGRESS) {
            // Referencing an element currently being built closes a cycle; treat as unresolvable.
            cyclicDependencyDetected();
            return null;
        }
        if (buildStatus == BuildStatus.NOT_BUILT) {
            build(depth);
        }
        return node;
    }

    public @NotNull AttributeNode attributeNode() {
        return attributeNode;
    }

    /**
     * Deep-copies this element as the shadow tree of a {@code <use>}. The copied root is detached (no parent,
     * treated as an only child) so position-dependent selectors re-match against the shadow tree; the copy is
     * unbuilt, so the caller must invoke build().
     * <p>
     * Limitation: {@code url(#id)} references inside the clone resolve to the original, not the clone.
     */
    @NotNull
    ParsedElement copyAsUseInstance(@NotNull NodeSupplier nodeSupplier) {
        return deepCopy(nodeSupplier, null, 1, 1);
    }

    private @NotNull ParsedElement deepCopy(@NotNull NodeSupplier nodeSupplier, @Nullable ParsedElement newParent,
            int indexInParent, int indexAmongSiblingsWithSameTagName) {
        SVGNode freshNode = nodeSupplier.create(tagName());
        if (freshNode == null) {
            throw new IllegalStateException("Cannot copy element <" + tagName() + ">");
        }
        // Fresh AttributeNode with an empty resolved-attribute map so build() re-runs the cascade.
        AttributeNode freshAttributes = attributeNode.copyForReparse();

        ParsedElement copy = new ParsedElement(id, document, newParent, indexInParent,
                indexAmongSiblingsWithSameTagName, freshAttributes, freshNode);
        freshAttributes.setElement(copy);

        // Text content is parsed during SAX parsing and never regenerated at build time, so carry it over.
        if (textContent != null) {
            copy.textContent = textContent.copyFor(copy);
        }

        // The subtree structure is identical to the original, so descendants keep their own indices;
        // only the root (above) is detached.
        for (ParsedElement child : children) {
            copy.addChild(child.deepCopy(nodeSupplier, copy,
                    child.oneBasedIndexInParent, child.oneBasedIndexAmongSiblingsWithSameTagName));
        }
        return copy;
    }

    void addChild(@NotNull ParsedElement parsedElement) {
        if (Category.hasCategory(parsedElement.node, Category.Animation)) {
            String attributeName = BaseAnimationNode.attributeName(parsedElement.attributeNode());
            animationElements.computeIfAbsent(attributeName, k -> new ArrayList<>()).add(parsedElement);
        }
        childCountsByTagName.merge(parsedElement.tagName(), 1, Integer::sum);
        children.add(parsedElement);
    }

    void addIndirectChild(@NotNull ParsedElement parsedElement) {
        indirectChildren.add(parsedElement);
    }

    private void addChildrenAndContent() {
        if (node instanceof Container) {
            int contentListsSize = textContent == null ? 0 : textContent.contentLists().size();
            for (int i = 0; i < children.size(); i++) {
                if (i < contentListsSize) {
                    assert textContent != null;
                    addContentList(textContent.contentLists().get(i));
                }
                ParsedElement child = children.get(i);
                ((Container<?>) node).addChild(child.id, child.node);
            }
            for (int i = children.size(); i < contentListsSize; i++) {
                assert textContent != null;
                addContentList(textContent.contentLists().get(i));
            }
        } else if (textContent != null) {
            for (List<TextContent.Segment> contentList : textContent.contentLists()) {
                addContentList(contentList);
            }
        }
    }

    private void addContentList(@NotNull List<TextContent.@NotNull Segment> contentList) {
        for (TextContent.Segment text : contentList) {
            node.addContent(text);
        }
    }

    void build(int depth) {
        if (buildStatus == BuildStatus.FINISHED) return;
        if (buildStatus == BuildStatus.IN_PROGRESS) {
            // A containment cycle: this element is building further up the stack and its reference
            // (transitively) led back here. Its in-flight reference gets severed below.
            partOfCycle = true;
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

        addChildrenAndContent();

        attributeNode.prepareForNodeBuilding();

        // Build depth first to ensure child nodes are processed first.
        // e.g. LinearGradient depends on its stops to be build first.
        for (ParsedElement child : children) {
            child.build(depth + 1);
        }

        // Children are built depth-first above, so each child's flag already covers its own subtree.
        updateSelectorsUseElementPositionInDomWithChildrenValues();

        document().setCurrentNestingDepth(depth);
        node.build(attributeNode);
        if (partOfCycle && node instanceof Use) {
            // The reference resolved during node.build closes a cycle; sever it (SVG 1.1 § 5.6).
            ((Use) node).setReferencedNode(null);
        }
        buildStatus = BuildStatus.FINISHED;
    }

    private void updateSelectorsUseElementPositionInDomWithChildrenValues() {
        for (ParsedElement child : children) {
            attributeNode.orSelectorsUseElementPositionInDom(
                    child.attributeNode.selectorsUseElementPositionInDom());
        }
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
        LOGGER.log(Level.WARNING, () -> "Cyclic dependency involving node '" + id + "' detected.");
    }
}
