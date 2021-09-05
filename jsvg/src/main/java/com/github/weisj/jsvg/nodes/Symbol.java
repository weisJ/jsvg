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

import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.AttributeParser;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.container.InnerViewContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;

@ElementCategories({Category.Container, Category.Structural})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <filter>, <font>, <font-face>, <foreignObject>,
     * <marker>, <mask>, <script>, <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Image.class, Pattern.class, Style.class, Text.class, View.class}
)
public final class Symbol extends InnerViewContainer {
    public static final String TAG = "symbol";

    private static final Length TopOrLeft = new Length(Unit.PERCENTAGE, 0f);
    private static final Length Center = new Length(Unit.PERCENTAGE, 50f);
    private static final Length BottomOrRight = new Length(Unit.PERCENTAGE, 100f);

    private Length refX;
    private Length refY;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected Point2D innerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(-refX.resolveWidth(context), -refY.resolveHeight(context));
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        refX = parseReferenceLength(attributeNode.getValue("refX"), "left", "right");
        refY = parseReferenceLength(attributeNode.getValue("refY"), "top", "bottom");
    }

    private Length parseReferenceLength(@Nullable String value, @NotNull String topLeft, @NotNull String bottomRight) {
        if (topLeft.equals(value)) {
            return TopOrLeft;
        } else if ("center".equals(value)) {
            return Center;
        } else if (bottomRight.equals(value)) {
            return BottomOrRight;
        } else {
            return AttributeParser.parseLength(value, Length.ZERO);
        }
    }

    @Override
    public boolean requiresInstantiation() {
        return true;
    }
}
