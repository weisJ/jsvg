/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jannis Weis
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.transform.TransformBox;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.parser.impl.AttributeNode;

public final class HasGeometryContextImpl implements HasGeometryContext {

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
        String[] transformOrigin = attributeNode.getStringList("transform-origin");
        String originX = transformOrigin.length > 0 ? transformOrigin[0] : null;
        String originY = transformOrigin.length > 1 ? transformOrigin[1] : null;
        return new HasGeometryContextImpl(
                attributeNode.parseTransform("transform", Inherited.NO, Animatable.YES),
                new Coordinate<>(
                        attributeNode.parser().parseLength(originX, Length.ZERO, PercentageDimension.WIDTH),
                        attributeNode.parser().parseLength(originY, Length.ZERO, PercentageDimension.HEIGHT)),
                attributeNode.getEnum("transform-box", TransformBox.ViewBox),
                attributeNode.getClipPath(),
                attributeNode.getMask(),
                attributeNode.getFilter());
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
