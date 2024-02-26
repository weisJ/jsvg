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
package com.github.weisj.jsvg.nodes.prototype.spec;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.SVGNode;

public enum Category {
    // Animations aren't supported
    Animation(false),
    BasicShape,
    Container,
    // Descriptions don't provide any information
    // in a static context.
    Descriptive(false),
    FilterPrimitive,
    TransferFunctionElement,
    Gradient,
    Graphic,
    GraphicsReferencing,
    Shape,
    Structural,
    TextContent,
    TextContentChild,
    None;

    private final boolean effectivelyAllowed;

    Category() {
        this(true);
    }

    Category(boolean effectivelyAllowed) {
        this.effectivelyAllowed = effectivelyAllowed;
    }

    public boolean isEffectivelyAllowed() {
        return effectivelyAllowed;
    }

    public static @NotNull Category @NotNull [] categoriesOf(@NotNull SVGNode node) {
        Class<? extends SVGNode> nodeType = node.getClass();
        ElementCategories categories = nodeType.getAnnotation(ElementCategories.class);
        if (categories == null) {
            throw new IllegalStateException(
                    "Element <" + node.tagName() + "> doesn't specify element category information");
        }
        return categories.value();
    }

    public static boolean hasCategory(@NotNull SVGNode node, @NotNull Category category) {
        Category[] categories = categoriesOf(node);
        for (Category c : categories) {
            if (c == category) return true;
        }
        return false;
    }
}
