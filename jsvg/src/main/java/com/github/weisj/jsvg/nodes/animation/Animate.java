/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.size.AnimatedLength;
import com.github.weisj.jsvg.animation.time.Duration;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.MetaSVGNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.SeparatorMode;

@ElementCategories(Category.Animation)
@PermittedContent(categories = {Category.Descriptive})
public final class Animate extends MetaSVGNode {
    public static final String TAG = "animate";

    private String[] values;
    private Duration duration;
    private float repeatCount;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public static @Nullable String attributeName(@NotNull AttributeNode attributeNode) {
        return attributeNode.getValue("attributeName");
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        attributeName = attributeNode.getValue("attributeName");
        values = attributeNode.getStringList("values", SeparatorMode.SEMICOLON_ONLY);
        duration = attributeNode.getDuration("dur", Duration.INDEFINITE);
        String repeatCountStr = attributeNode.getValue("repeatCount");
        if ("indefinite".equals(repeatCountStr)) {
            repeatCount = Integer.MAX_VALUE;
        } else {
            repeatCount = attributeNode.parser().parseFloat(repeatCountStr, 1);
        }
    }

    public @Nullable AnimatedLength animatedLength(@NotNull Length initial, @NotNull AttributeNode attributeNode) {
        if (duration.isIndefinite()) return null;
        if (duration.milliseconds() < 0) return null;
        if (repeatCount <= 0) return null;

        Length[] lengths = new Length[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = attributeNode.parser().parseLength(values[i], null);
            if (lengths[i] == null) return null;
        }
        return new AnimatedLength(duration, repeatCount, initial, lengths);
    }

}
