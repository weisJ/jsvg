/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.prototype.impl;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.transform.TransformBox;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;

public final class HasGeometryContextImpl implements HasGeometryContext {

    private static final List<ComponentValue> CENTER = Collections.singletonList(new Token.Ident("center"));

    private final @Nullable TransformValue transform;
    private final @NotNull Coordinate<LengthValue> transformOrigin;
    private final @NotNull TransformBox transformBox;

    private final @Nullable ClipPath clipPath;
    private final @Nullable Mask mask;
    private final @Nullable Filter filter;

    private HasGeometryContextImpl(@Nullable TransformValue transform, @NotNull Coordinate<LengthValue> transformOrigin,
            @NotNull TransformBox transformBox, @Nullable ClipPath clipPath,
            @Nullable Mask mask, @Nullable Filter filter) {
        this.transform = transform;
        this.transformOrigin = transformOrigin;
        this.transformBox = transformBox;
        this.clipPath = clipPath;
        this.mask = mask;
        this.filter = filter;
    }

    public static @NotNull HasGeometryContext parse(@NotNull AttributeNode attributeNode) {
        return new HasGeometryContextImpl(
                attributeNode.parseTransform("transform", Inherited.NO, Animatable.YES),
                parseTransformOrigin(attributeNode),
                attributeNode.getEnum("transform-box", TransformBox.ViewBox),
                attributeNode.getClipPath(),
                attributeNode.getMask(),
                attributeNode.getFilter());
    }

    private static @NotNull Coordinate<LengthValue> parseTransformOrigin(@NotNull AttributeNode node) {
        List<List<ComponentValue>> tokenParts =
                node.getSplitTokenList("transform-origin", SeparatorMode.WHITESPACE_ONLY);
        // absent or empty: default "0 0"
        if (tokenParts == null || tokenParts.isEmpty()) return new Coordinate<>(Length.ZERO, Length.ZERO);
        return resolveOrigin(node, tokenParts);
    }

    /** Assigns transform-origin parts to x/y per CSS Transforms 1 §3. */
    private static @NotNull Coordinate<LengthValue> resolveOrigin(@NotNull AttributeNode node,
            @NotNull List<@NotNull List<@NotNull ComponentValue>> parts) {
        List<ComponentValue> originX;
        List<ComponentValue> originY;
        if (parts.size() == 1) {
            List<ComponentValue> value = parts.get(0);
            // lone vertical keyword → y; else → x
            if (node.isVerticalKeyword(value)) {
                originX = CENTER;
                originY = value;
            } else {
                originX = value;
                originY = CENTER;
            }
        } else {
            // keywords may appear in either order
            List<ComponentValue> first = parts.get(0);
            List<ComponentValue> second = parts.get(1);
            if (node.isVerticalKeyword(first) || node.isHorizontalKeyword(second)) {
                originX = second;
                originY = first;
            } else {
                originX = first;
                originY = second;
            }
        }
        return new Coordinate<>(node.getHorizontalReferenceLength(originX), node.getVerticalReferenceLength(originY));
    }

    @Override
    public @Nullable ClipPath clipPath() {
        return clipPath;
    }

    @Override
    public @Nullable Mask mask() {
        return mask;
    }

    @Override
    public @Nullable Filter filter() {
        return filter;
    }

    @Override
    public @Nullable TransformValue transform() {
        return transform;
    }

    @Override
    public @NotNull TransformBox transformBox() {
        return transformBox;
    }

    @Override
    public @NotNull Coordinate<LengthValue> transformOrigin() {
        return transformOrigin;
    }
}
