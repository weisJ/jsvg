/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jannis Weis
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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.github.weisj.jsvg.nodes.SVGNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.TransformBox;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.ElementBounds;
import com.github.weisj.jsvg.renderer.RenderContext;

public final class HasGeometryContextImpl implements HasGeometryContext {

    private final @Nullable AffineTransform transform;
    private final @NotNull Length transformOriginX;
    private final @NotNull Length transformOriginY;
    private final @NotNull TransformBox transformBox;

    private final @Nullable ClipPath clipPath;
    private final @Nullable Mask mask;
    private final @Nullable Filter filter;

    private final @NotNull SVGNode node;

    private HasGeometryContextImpl(@Nullable AffineTransform transform, @NotNull Length transformOriginX,
            @NotNull Length transformOriginY, @NotNull TransformBox transformBox, @Nullable ClipPath clipPath,
            @Nullable Mask mask, @Nullable Filter filter, @NotNull SVGNode node) {
        this.transform = transform;
        this.transformOriginX = transformOriginX;
        this.transformOriginY = transformOriginY;
        this.transformBox = transformBox;
        this.clipPath = clipPath;
        this.mask = mask;
        this.filter = filter;
        this.node = node;
    }

    public static @NotNull HasGeometryContext parse(@NotNull AttributeNode attributeNode, @NotNull SVGNode node) {
        String[] transformOrigin = attributeNode.getStringList("transform-origin");
        String originX = transformOrigin.length > 0 ? transformOrigin[0] : null;
        String originY = transformOrigin.length > 1 ? transformOrigin[1] : null;
        return new HasGeometryContextImpl(
                attributeNode.parseTransform("transform"),
                attributeNode.parser().parseLength(originX, Length.ZERO, PercentageDimension.WIDTH),
                attributeNode.parser().parseLength(originY, Length.ZERO, PercentageDimension.HEIGHT),
                attributeNode.getEnum("transform-box", TransformBox.ViewBox),
                attributeNode.getClipPath(),
                attributeNode.getMask(),
                attributeNode.getFilter(),
                node);
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
    public @Nullable AffineTransform transform() {
        return transform;
    }

    @Override
    public @NotNull Point2D transformOrigin(@NotNull RenderContext context) {
        if (transformBox == TransformBox.ViewBox) {
            MeasureContext measureContext = context.measureContext();
            return new Point2D.Float(
                    transformOriginX.resolve(measureContext),
                    transformOriginY.resolve(measureContext));
        }

        @NotNull ElementBounds bounds = new ElementBounds(node, context);
        @NotNull Rectangle2D box = transformBox == TransformBox.FillBox ? bounds.boundingBox() : bounds.strokeBox();
        @NotNull MeasureContext boundsMeasureContext =
                context.measureContext().derive((float) box.getWidth(), (float) box.getHeight());

        return new Point2D.Float(
                transformOriginX.resolve(boundsMeasureContext) + (float) box.getX(),
                transformOriginY.resolve(boundsMeasureContext) + (float) box.getY());
    }
}
