/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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

import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.attributes.paint.AwtSVGPaint;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.parser.AttributeNode;

public class PaintContext implements Mutator<PaintContext> {

    public final @Nullable AwtSVGPaint color;
    public final @Nullable SVGPaint fillPaint;
    public final @Nullable SVGPaint strokePaint;

    public final @Percentage float opacity;
    public final @Percentage float fillOpacity;
    public final @Percentage float strokeOpacity;

    public final @Nullable StrokeContext strokeContext;

    public PaintContext(@Nullable AwtSVGPaint color, @Nullable SVGPaint fillPaint, float fillOpacity,
            @Nullable SVGPaint strokePaint, float strokeOpacity, float opacity,
            @Nullable StrokeContext strokeContext) {
        this.color = color;
        this.fillPaint = fillPaint;
        this.strokePaint = strokePaint;
        this.fillOpacity = fillOpacity;
        this.strokeOpacity = strokeOpacity;
        this.opacity = opacity;
        // Avoid creating unnecessary intermediate contexts during painting.
        this.strokeContext = strokeContext == null || strokeContext.isTrivial() ? null : strokeContext;
    }

    public static @NotNull PaintContext createDefault() {
        return new PaintContext(
                SVGPaint.DEFAULT_PAINT,
                SVGPaint.DEFAULT_PAINT, 1,
                SVGPaint.NONE, 1, 1,
                StrokeContext.createDefault());
    }

    public static @NotNull PaintContext parse(@NotNull AttributeNode attributeNode) {
        return new PaintContext(
                parseColorAttribute(attributeNode),
                attributeNode.getPaint("fill"),
                attributeNode.getPercentage("fill-opacity", 1),
                attributeNode.getPaint("stroke"),
                attributeNode.getPercentage("stroke-opacity", 1),
                attributeNode.getPercentage("opacity", 1),
                StrokeContext.parse(attributeNode));
    }

    private static @Nullable AwtSVGPaint parseColorAttribute(@NotNull AttributeNode attributeNode) {
        Color c = attributeNode.getColor("color", null);
        if (c == null) return null;
        return new AwtSVGPaint(c);
    }

    public @NotNull PaintContext derive(@NotNull PaintContext context) {
        return new PaintContext(
                context.color != null ? context.color : color,
                context.fillPaint != null ? context.fillPaint : fillPaint,
                fillOpacity * context.fillOpacity,
                context.strokePaint != null ? context.strokePaint : strokePaint,
                strokeOpacity * context.strokeOpacity,
                opacity * context.opacity,
                strokeContext != null
                        ? strokeContext.derive(context.strokeContext)
                        : context.strokeContext);
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
                '}';
    }
}
