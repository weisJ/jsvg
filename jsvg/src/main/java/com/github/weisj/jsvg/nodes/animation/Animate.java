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
package com.github.weisj.jsvg.nodes.animation;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.time.TimeValue;
import com.github.weisj.jsvg.geometry.size.AnimatedLength;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories(Category.Animation)
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {/* <script> */}
)
public final class Animate extends AbstractSVGNode {
    public static final String TAG = "animate";

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        String attributeName = attributeNode.getValue("attributeName");
        if (attributeName == null) return;

        TimeValue begin = attributeNode.getTime("begin", TimeValue.ZERO);
        TimeValue end = attributeNode.getTime("end");
        if (end == null) {
            TimeValue duration = attributeNode.getTime("dur");
            end = duration != null ? begin.add(duration) : begin;
        }

        Length from = attributeNode.getLength("from");
        Length to = attributeNode.getLength("to");
        if (from == null || to == null) return;

        AnimatedLength animatedLength = new AnimatedLength(from, to, begin, end);
        attributeNode.registerAnimationLength(attributeName, animatedLength);
    }
}
