/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import com.github.weisj.jsvg.animation.Additive;
import com.github.weisj.jsvg.animation.AnimationValuesType;
import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.SeparatorMode;

public abstract class BaseAnimationNode extends AbstractSVGNode {

    // Note: Needs to be replaced by the additive neutral element of the type.
    private static final @NotNull String INITIAL_PLACEHOLDER = "<<initial>>";

    protected String[] values;
    protected @Nullable Track track;

    BaseAnimationNode() {}

    public static @Nullable String attributeName(@NotNull AttributeNode attributeNode) {
        return attributeNode.getValue("attributeName");
    }

    @SuppressWarnings("ReferenceEquality")
    protected static boolean isPlaceholder(@NotNull String value) {
        return value == INITIAL_PLACEHOLDER;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        String from = attributeNode.getValue("from");
        String to = attributeNode.getValue("to");
        String by = attributeNode.getValue("by");

        if (to != null) by = null;

        AnimationValuesType valuesType = null;
        if (attributeNode.getValue("values") != null) {
            values = attributeNode.getStringList("values", SeparatorMode.SEMICOLON_ONLY);
            valuesType = AnimationValuesType.VALUES;
        } else {
            if (from != null && to != null) {
                values = new String[] {from, to};
                valuesType = AnimationValuesType.FROM_TO;
            } else if (from != null && by != null) {
                values = new String[] {from, by};
                valuesType = AnimationValuesType.FROM_BY;
            } else if (by != null) {
                values = new String[] {INITIAL_PLACEHOLDER, by};
                valuesType = AnimationValuesType.BY;
            } else if (to != null) {
                values = new String[] {INITIAL_PLACEHOLDER, to};
                valuesType = AnimationValuesType.TO;
            }
        }

        if (valuesType != null) {
            Additive additive = valuesType.endIsBy()
                    ? Additive.SUM
                    : attributeNode.getEnum("additive", Additive.REPLACE);
            track = Track.parse(attributeNode, valuesType, additive);
        }
    }

    public @Nullable Track track() {
        return track;
    }
}
