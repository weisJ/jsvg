/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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

import com.github.weisj.jsvg.nodes.container.RenderableContainerNode;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;

/**
 * As jsvg is only a static renderer without interaction the anchor tag behaves just as if it were a
 * group.
 */
@ElementCategories(Category.Container)
@PermittedContent(
    categories = {Category.Container, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient,
            Category.Animation},
    /*
     * <altGlyphDef> <color-profile>, <cursor>, <filter>, <font>, <font-face>, <foreignObject>, <image>,
     * <marker>, <mask>, <pattern>, <script>, <switch>, <view>
     */
    anyOf = {Anchor.class, ClipPath.class, Style.class, Text.class}
)
public final class Anchor extends RenderableContainerNode implements ShapedContainer<SVGNode> {
    public static final String TAG = "a";

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }
}
