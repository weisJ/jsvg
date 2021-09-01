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
package com.github.weisj.jsvg.renderer;


import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;

public class PaintContext {

    public final SVGPaint fillPaint;
    public final SVGPaint strokePaint;

    public final @Percentage float opacity;
    public final @Percentage float fillOpacity;
    public final @Percentage float strokeOpacity;

    public PaintContext(SVGPaint fillPaint, float fillOpacity,
            SVGPaint strokePaint, float strokeOpacity, float opacity) {
        this.fillPaint = fillPaint;
        this.strokePaint = strokePaint;
        this.fillOpacity = fillOpacity;
        this.strokeOpacity = strokeOpacity;
        this.opacity = opacity;
    }

    public static @NotNull PaintContext createDefault() {
        return new PaintContext(
                SVGPaint.DEFAULT_PAINT, 1,
                SVGPaint.NONE, 1, 1);
    }

    public static @NotNull PaintContext parse(@NotNull AttributeNode attributeNode) {
        return new PaintContext(
                attributeNode.getPaint("fill"),
                attributeNode.getPercentage("fill-opacity", 1),
                attributeNode.getPaint("stroke"),
                attributeNode.getPercentage("stroke-opacity", 1),
                attributeNode.getPercentage("opacity", 1));
    }

    @Override
    public String toString() {
        return "PaintContext{" +
                "fillPaint=" + fillPaint +
                ", strokePaint=" + strokePaint +
                ", opacity=" + opacity +
                ", fillOpacity=" + fillOpacity +
                ", strokeOpacity=" + strokeOpacity +
                '}';
    }
}
