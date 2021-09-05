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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.util.Todo;

@ElementCategories(Category.Container)
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <filter>, <font>, <font-face>, <foreignObject>,
     * <marker>, <mask>, <script>, <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Image.class, Pattern.class, Style.class, Text.class, View.class}
)
public final class Pattern extends ContainerNode implements SVGPaint {
    public static final String TAG = "pattern";

    private Length x;
    private Length y;
    private Length width;
    private Length height;

    private UnitType patternUnits;
    private UnitType patternContentUnits;
    private AffineTransform patternTransform;

    private ViewBox viewBox;
    private PreserveAspectRatio preserveAspectRatio;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        Pattern template = parseTemplate(attributeNode);

        x = attributeNode.getLength("x", template != null ? template.x : Length.ZERO);
        y = attributeNode.getLength("y", template != null ? template.y : Length.ZERO);
        // Note: width == 0 || height == 0 implies nothing should be painted.
        width = attributeNode.getLength("width", template != null ? template.width : Length.ZERO);
        height = attributeNode.getLength("height", template != null ? template.height : Length.ZERO);

        viewBox = attributeNode.getViewBox();
        if (viewBox == null && template != null) viewBox = template.viewBox;

        preserveAspectRatio = PreserveAspectRatio.parse(
                attributeNode.getValue("preserveAspectRatio"),
                template != null ? template.preserveAspectRatio : null);

        patternTransform = attributeNode.parseTransform("patternTransform");
        if (patternTransform == null && template != null) patternTransform = template.patternTransform;

        patternUnits = attributeNode.getEnum("patternUnits",
                template != null ? template.patternUnits : UnitType.ObjectBoundingBox);
        patternContentUnits = attributeNode.getEnum("patternContentUnits",
                template != null ? template.patternContentUnits : UnitType.ObjectBoundingBox);
    }

    @Nullable
    private Pattern parseTemplate(@NotNull AttributeNode attributeNode) {
        return attributeNode.getElementByHref(Pattern.class, attributeNode.getHref());
    }

    @Override
    public boolean isVisible() {
        return !width.isZero() && !height.isZero() && SVGPaint.super.isVisible();
    }

    @Override
    public @NotNull Paint paintForBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds) {
        // Todo: Implement pattern paints. Option: Paint to a buffer and use a TexturePaint
        return Todo.todo("Pattern not et implemented");
    }
}
