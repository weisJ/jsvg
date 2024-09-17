/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.renderer;


import java.awt.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.paint.AwtSVGPaint;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.parser.AttributeNode;

public final class PaintContext implements Mutator<PaintContext> {

    public final @Nullable SVGPaint color;
    public final @Nullable SVGPaint fillPaint;
    public final @Nullable SVGPaint strokePaint;

    public final PercentageValue opacity;
    public final PercentageValue fillOpacity;
    public final PercentageValue strokeOpacity;

    public final @Nullable PaintOrder paintOrder;

    public final @Nullable StrokeContext strokeContext;
    public final @Nullable FillRule fillRule;

    public PaintContext(@Nullable SVGPaint color, @Nullable SVGPaint fillPaint, PercentageValue fillOpacity,
            @Nullable SVGPaint strokePaint, PercentageValue strokeOpacity, PercentageValue opacity,
            @Nullable PaintOrder paintOrder,
            @Nullable StrokeContext strokeContext, @Nullable FillRule fillRule) {
        this.color = color;
        this.fillPaint = fillPaint;
        this.strokePaint = strokePaint;
        this.fillOpacity = fillOpacity;
        this.strokeOpacity = strokeOpacity;
        this.opacity = opacity;
        this.paintOrder = paintOrder;
        // Avoid creating unnecessary intermediate contexts during painting.
        this.strokeContext = strokeContext == null || strokeContext.isTrivial() ? null : strokeContext;
        this.fillRule = fillRule;
    }

    public static @NotNull PaintContext createDefault() {
        return new PaintContext(
                SVGPaint.DEFAULT_PAINT,
                SVGPaint.DEFAULT_PAINT, Percentage.ONE,
                SVGPaint.NONE, Percentage.ONE, Percentage.ONE,
                PaintOrder.NORMAL,
                StrokeContext.createDefault(),
                FillRule.Nonzero);
    }

    public static @NotNull PaintContext parse(@NotNull AttributeNode attributeNode) {
        return new PaintContext(
                parseColorAttribute(attributeNode),
                attributeNode.getPaint("fill", Animatable.YES),
                attributeNode.getPercentage("fill-opacity", Percentage.ONE, Animatable.YES),
                attributeNode.getPaint("stroke", Animatable.YES),
                attributeNode.getPercentage("stroke-opacity", Percentage.ONE),
                attributeNode.getPercentage("opacity", Percentage.ONE),
                PaintOrder.parse(attributeNode),
                StrokeContext.parse(attributeNode),
                FillRule.parse(attributeNode));
    }

    private static @Nullable AwtSVGPaint parseColorAttribute(@NotNull AttributeNode attributeNode) {
        Color c = attributeNode.getColor("color", null);
        if (c == null) return null;
        return new AwtSVGPaint(c);
    }

    public @NotNull PaintContext derive(@NotNull PaintContext context) {
        return new PaintContext(
                SVGPaint.derive(color, context.color),
                SVGPaint.derive(fillPaint, context.fillPaint),
                fillOpacity.multiply(context.fillOpacity),
                SVGPaint.derive(strokePaint, context.strokePaint),
                strokeOpacity.multiply(context.strokeOpacity),
                opacity.multiply(context.opacity),
                context.paintOrder != null ? context.paintOrder : paintOrder,
                strokeContext != null
                        ? strokeContext.derive(context.strokeContext)
                        : context.strokeContext,
                context.fillRule != null && context.fillRule != FillRule.Inherit
                        ? context.fillRule
                        : this.fillRule);
    }

    @Override
    public @NotNull PaintContext mutate(@NotNull PaintContext element) {
        return element.derive(this);
    }

    @Override
    public String toString() {
        return "PaintContext{" +
                "color=" + color +
                ", fillPaint=" + fillPaint +
                ", strokePaint=" + strokePaint +
                ", opacity=" + opacity +
                ", fillOpacity=" + fillOpacity +
                ", strokeOpacity=" + strokeOpacity +
                ", strokeContext=" + strokeContext +
                ", paintOrder=" + paintOrder +
                ", fillRule=" + fillRule +
                '}';
    }
}
