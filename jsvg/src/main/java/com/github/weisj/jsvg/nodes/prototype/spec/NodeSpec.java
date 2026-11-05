/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.prototype.spec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.SVGNode;

/** Annotation values of a node class, read once since every access through the annotation proxy is expensive. */
public final class NodeSpec {
    private static final ClassValue<NodeSpec> SPECS = new ClassValue<NodeSpec>() {
        @Override
        protected NodeSpec computeValue(Class<?> type) {
            return new NodeSpec(type);
        }
    };

    private final boolean hasPermittedContent;
    private final boolean anyContent;
    private final boolean charData;
    private final Category @NotNull [] permittedCategories;
    private final Class<? extends SVGNode> @NotNull [] permittedTypes;
    private final Category @Nullable [] categories;
    private final boolean neverRendered;

    @SuppressWarnings("unchecked")
    private NodeSpec(@NotNull Class<?> type) {
        PermittedContent permitted = type.getAnnotation(PermittedContent.class);
        hasPermittedContent = permitted != null;
        anyContent = permitted != null && permitted.any();
        charData = permitted != null && permitted.charData();
        permittedCategories = permitted != null ? permitted.categories() : new Category[0];
        permittedTypes = permitted != null ? permitted.anyOf() : (Class<? extends SVGNode>[]) new Class<?>[0];
        ElementCategories elementCategories = type.getAnnotation(ElementCategories.class);
        categories = elementCategories != null ? elementCategories.value() : null;
        neverRendered = elementCategories != null && elementCategories.neverRendered();
    }

    public static @NotNull NodeSpec of(@NotNull SVGNode node) {
        return SPECS.get(node.getClass());
    }

    public boolean hasPermittedContent() {
        return hasPermittedContent;
    }

    public boolean anyContent() {
        return anyContent;
    }

    public boolean charData() {
        return charData;
    }

    /** Not to be modified. */
    public Category @NotNull [] permittedCategories() {
        return permittedCategories;
    }

    /** Not to be modified. */
    public Class<? extends SVGNode> @NotNull [] permittedTypes() {
        return permittedTypes;
    }

    /** Not to be modified. */
    public Category @NotNull [] categories(@NotNull SVGNode node) {
        if (categories == null) {
            throw new IllegalStateException(
                    "Element <" + node.tagName() + "> doesn't specify element category information");
        }
        return categories;
    }

    public boolean neverRendered(@NotNull SVGNode node) {
        categories(node);
        return neverRendered;
    }

    public boolean hasCategory(@NotNull SVGNode node, @NotNull Category category) {
        for (Category c : categories(node)) {
            if (c == category) return true;
        }
        return false;
    }
}
