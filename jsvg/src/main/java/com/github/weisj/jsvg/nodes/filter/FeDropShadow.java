/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.filter;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeDropShadow extends ChainedFilterPrimitive {
    public static final String TAG = "fedropshadow";

    private FilterPrimitive[] primitives;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        AttributeNode child = attributeNode.copy();

        String resultKey = "result";

        FilterChannelKey inputId = attributeNode.getFilterChannelKey("in", outerLastResult);
        String resultId = child.getValue(resultKey);
        if (resultId == null) {
            resultId = "dropshadow-" + attributeNode.hashCode();
        }
        child.setValue(resultKey, resultId);

        FeGaussianBlur blur = new FeGaussianBlur();
        blur.build(child);
        blur.setOnlyAlpha(true);

        child.setValue("in", resultId);

        String offsetResultId = resultId + "-offset-" + resultId.hashCode();
        child.setValue(resultKey, offsetResultId);
        FeOffset offset = new FeOffset();
        offset.build(child);

        child.setValue(resultKey, resultId);
        FeFlood flood = new FeFlood();
        flood.build(child);

        child.setValue("in2", offsetResultId);
        child.setValue("operator", "in");
        FeComposite composite = new FeComposite();
        composite.build(child);

        FeMergeNode node1 = new FeMergeNode();
        node1.build(child);

        child.setValue("in", inputId.key().toString());
        FeMergeNode node2 = new FeMergeNode();
        node2.build(child);

        FeMerge merge = new FeMerge();
        merge.addChild(null, node1);
        merge.addChild(null, node2);
        merge.build(child);

        primitives = new FilterPrimitive[] {
                blur,
                offset,
                flood,
                composite,
                merge
        };
    }

    @Override
    protected @NotNull FilterPrimitive @NotNull [] primitives() {
        return primitives;
    }
}
