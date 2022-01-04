/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.nodes.container;

import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

public abstract class BaseContainerNode<E> extends AbstractSVGNode implements Container<E> {
    private static final boolean EXHAUSTIVE_CHECK = true;
    private static final Logger LOGGER = Logger.getLogger(BaseContainerNode.class.getName());

    @Override
    public final void addChild(@Nullable String id, @NotNull SVGNode node) {
        if (isAcceptableType(node) && acceptChild(id, node)) {
            doAdd(node);
        }
    }

    protected abstract void doAdd(@NotNull SVGNode node);

    /**
     * Determine whether the container accepts this {@link SVGNode} as a child.
     * By default, this will always report true but subclasses may choose to reject certain
     * types of nodes.
     *
     * @param id the id of the node
     * @param node the node itself
     * @return whether the node can be inserted as a child.
     */
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return true;
    }

    protected boolean isAcceptableType(@NotNull SVGNode node) {
        PermittedContent allowedNodes = getClass().getAnnotation(PermittedContent.class);
        if (allowedNodes == null) {
            throw new IllegalStateException(
                    String.format("Element <%s> doesn't specify permitted content information", tagName()));
        }
        if (allowedNodes.any()) return true;

        Class<? extends SVGNode> nodeType = node.getClass();
        ElementCategories categories = nodeType.getAnnotation(ElementCategories.class);
        if (categories == null) {
            throw new IllegalStateException(
                    "Element <" + node.tagName() + "> doesn't specify element category information");
        }
        CategoryCheckResult result = doIntersect(allowedNodes.categories(), categories.value());
        if (result == CategoryCheckResult.Allowed) return true;
        for (Class<? extends SVGNode> type : allowedNodes.anyOf()) {
            if (type.isAssignableFrom(nodeType)) return true;
        }
        if (EXHAUSTIVE_CHECK && result != CategoryCheckResult.Excluded) {
            LOGGER.warning(() -> String.format("Element <%s> not allowed in <%s> (or not implemented)",
                    node.tagName(), tagName()));
        }
        return false;
    }

    private CategoryCheckResult doIntersect(Category[] requested, Category[] provided) {
        // Expected sizes for theses arrays is pretty small hence we don't need to
        // be smart about the intersection check.
        CategoryCheckResult result = CategoryCheckResult.Denied;
        for (Category request : requested) {
            boolean effectivelyAllowed = request.isEffectivelyAllowed();
            if (!effectivelyAllowed && !EXHAUSTIVE_CHECK) continue;
            for (Category category : provided) {
                if (request == category) {
                    if (effectivelyAllowed) return CategoryCheckResult.Allowed;
                    // Keep searching. Element may be allowed by another category.
                    result = CategoryCheckResult.Excluded;
                }
            }
        }
        return result;
    }

    private enum CategoryCheckResult {
        /**
         * The element is allowed as a child node.
         */
        Allowed,
        /**
         * The element isn't allowed as a child node.
         */
        Denied,
        /**
         * The element is allowed but excluded due by the library
         * due to some other reason.
         */
        Excluded
    }
}
