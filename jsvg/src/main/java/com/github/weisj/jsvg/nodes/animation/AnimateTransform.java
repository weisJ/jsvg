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

import com.github.weisj.jsvg.animation.value.AnimatedTransform;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;

@ElementCategories(Category.Animation)
@PermittedContent(categories = {Category.Descriptive})
public final class AnimateTransform extends BaseAnimationNode {
    public static final String TAG = "animatetransform";

    private TransformPart.TransformType type;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        type = attributeNode.getEnum("type", TransformPart.TransformType.TRANSLATE);
    }

    public @Nullable AnimatedTransform animatedTransform(@NotNull TransformValue initial,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;

        TransformPart[] transforms = new TransformPart[values.length];
        TransformPart identity = TransformPart.identityOfType(type);
        for (int i = 0; i < values.length; i++) {
            if (isPlaceholder(values[i])) {
                transforms[i] = identity;
            } else {
                TransformPart part = attributeNode.parser().parseTransformPart(type, values[i]);
                if (part == null) return null;
                transforms[i] = part;
            }
        }
        return new AnimatedTransform(track, initial, transforms);
    }
}
