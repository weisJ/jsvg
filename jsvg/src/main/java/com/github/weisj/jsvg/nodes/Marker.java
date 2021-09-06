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

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.MarkerOrientation;
import com.github.weisj.jsvg.attributes.MarkerUnitType;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.BaseInnerViewContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.Container)
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <filter>, <font>, <font-face>, <foreignObject>, <mask>,
     * <script>, <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Image.class, Marker.class, Pattern.class, Style.class, Text.class,
            View.class}
)
public final class Marker extends BaseInnerViewContainer {
    public static final String TAG = "marker";

    private Length refX;
    private Length refY;

    private MarkerOrientation orientation;

    private MarkerUnitType markerUnits;
    private Length markerHeight;
    private Length markerWidth;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public @NotNull MarkerOrientation orientation() {
        return orientation;
    }

    @Override
    protected Point2D outerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(0, 0);
    }

    @Override
    protected Point2D innerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(-refX.resolveWidth(context), -refY.resolveHeight(context));
    }

    @Override
    public @NotNull FloatSize size(@NotNull RenderContext context) {
        MeasureContext measure = context.measureContext();
        if (markerUnits == MarkerUnitType.StrokeWidth) {
            Length strokeWidthLength = context.strokeContext().strokeWidth;
            assert strokeWidthLength != null;
            float strokeWidth = strokeWidthLength.resolveLength(measure);
            return new FloatSize(markerWidth.raw() * strokeWidth, markerHeight.raw() * strokeWidth);
        } else {
            return new FloatSize(markerWidth.resolveWidth(measure), markerHeight.resolveHeight(measure));
        }
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        refX = attributeNode.getHorizontalReferenceLength("refX");
        refY = attributeNode.getHorizontalReferenceLength("refY");

        orientation = MarkerOrientation.parse(attributeNode.getValue("orient"));

        markerUnits = attributeNode.getEnum("markerUnits", MarkerUnitType.StrokeWidth);
        markerWidth = attributeNode.getLength("markerWidth", 3);
        markerHeight = attributeNode.getLength("markerHeight", 3);
    }

    @Override
    public boolean requiresInstantiation() {
        return true;
    }
}
