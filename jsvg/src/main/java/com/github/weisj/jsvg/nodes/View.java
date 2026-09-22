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
package com.github.weisj.jsvg.nodes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * A predefined SVG view.
 *
 * @see <a href="https://www.w3.org/TR/SVG2/linking.html#ViewElement">SVG 2 view element</a>
 */
@ElementCategories({/* None */})
@PermittedContent(
    categories = Category.Descriptive
)
public final class View extends MetaSVGNode {
    public static final String TAG = "view";
    private @Nullable ViewBox viewBox;
    private @Nullable PreserveAspectRatio preserveAspectRatio;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        viewBox = attributeNode.getViewBox();
        String preserveAspectRatioValue = attributeNode.getValue("preserveAspectRatio");
        preserveAspectRatio = preserveAspectRatioValue != null
                ? PreserveAspectRatio.parse(preserveAspectRatioValue, attributeNode.parser())
                : null;
    }

    public @Nullable ViewBox viewBox() {
        return viewBox;
    }

    public @Nullable PreserveAspectRatio preserveAspectRatio() {
        return preserveAspectRatio;
    }
}
