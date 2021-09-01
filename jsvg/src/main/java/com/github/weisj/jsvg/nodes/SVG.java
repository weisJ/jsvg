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
package com.github.weisj.jsvg.nodes;

import java.awt.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.container.RenderableContainerNode;
import com.github.weisj.jsvg.nodes.prototype.MaybeHasViewBox;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories({Category.Container, Category.Structural})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <filter>, <font>, <font-face>, <foreignObject>,
     * <image>, <marker>, <mask>, <pattern>, <script>, <switch>, <view>
     */
    anyOf = {Anchor.class, ClipPath.class, Style.class, Text.class}
)
public final class SVG extends RenderableContainerNode implements MaybeHasViewBox {
    public static final String TAG = "svg";

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private ViewBox viewBox;
    private PreserveAspectRatio preserveAspectRatio;


    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    public Length width() {
        return width;
    }

    public Length height() {
        return height;
    }

    @Override
    public @NotNull ViewBox viewBox(@NotNull MeasureContext measureContext) {
        if (viewBox != null) return viewBox;
        // If there is no viewBox specified we still have to establish a new frame with out current size.
        // Only if we have no sizes (i.e. they are negative) we use the parent viewBox.
        return new ViewBox(
                width.resolveWidth(measureContext),
                height.resolveHeight(measureContext));
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", 0);
        y = attributeNode.getLength("y", 0);
        viewBox = parseViewBox(attributeNode);
        width = attributeNode.getLength("width",
                viewBox != null ? Unit.Raw.valueOf(viewBox.width) : Length.UNSPECIFIED).coerceNonNegative();
        height = attributeNode.getLength("height",
                viewBox != null ? Unit.Raw.valueOf(viewBox.height) : Length.UNSPECIFIED).coerceNonNegative();
        preserveAspectRatio = PreserveAspectRatio.parse(attributeNode.getValue("preserveAspectRatio"));
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        if (width.isZero() || height.isZero()) return;
        MeasureContext measureContext = context.measureContext();
        g.translate(x.resolveWidth(measureContext), y.resolveHeight(measureContext));
        ViewBox viewport = new ViewBox(
                width.orElseIfUnspecified(measureContext.viewWidth()).resolveWidth(measureContext),
                height.orElseIfUnspecified(measureContext.viewHeight()).resolveLength(measureContext));
        Graphics2D viewportGraphics = preserveAspectRatio.prepareViewPort(g, viewport, viewBox);
        super.render(context, viewportGraphics);
        viewportGraphics.dispose();
    }
}
