/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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

import com.github.weisj.jsvg.attributes.MarkerOrientation;
import com.github.weisj.jsvg.attributes.MarkerUnitType;
import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.container.BaseInnerViewContainer;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

@ElementCategories(Category.Container)
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <font>, <font-face>, <foreignObject>, <script>,
     * <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Filter.class, Image.class, Mask.class, Marker.class, Pattern.class,
            Style.class, Text.class, View.class}
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
    protected @NotNull Point2D outerLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(0, 0);
    }

    @Override
    protected @NotNull Point2D anchorLocation(@NotNull MeasureContext context) {
        return new Point2D.Float(-refX.resolve(context), -refY.resolve(context));
    }

    @Override
    protected @NotNull Overflow defaultOverflow() {
        return Overflow.Hidden;
    }

    @Override
    public @NotNull FloatSize size(@NotNull RenderContext context) {
        MeasureContext measure = context.measureContext();
        if (markerUnits == MarkerUnitType.StrokeWidth) {
            LengthValue strokeWidthLength = RenderContextAccessor.instance().strokeContext(context).strokeWidth;
            assert strokeWidthLength != null;
            float strokeWidth = strokeWidthLength.resolve(measure);
            return new FloatSize(markerWidth.raw() * strokeWidth, markerHeight.raw() * strokeWidth);
        } else {
            return new FloatSize(markerWidth.resolve(measure), markerHeight.resolve(measure));
        }
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        refX = attributeNode.getHorizontalReferenceLength("refX");
        refY = attributeNode.getHorizontalReferenceLength("refY");

        orientation = MarkerOrientation.parse(attributeNode.getValue("orient"), attributeNode.parser());

        markerUnits = attributeNode.getEnum("markerUnits", MarkerUnitType.StrokeWidth);
        markerWidth = attributeNode.getLength("markerWidth", PercentageDimension.WIDTH, 3);
        markerHeight = attributeNode.getLength("markerHeight", PercentageDimension.HEIGHT, 3);
    }

    @Override
    public boolean requiresInstantiation() {
        return true;
    }

    @Override
    protected @NotNull RenderContext createInnerContext(@NotNull RenderContext context, @NotNull ViewBox viewBox) {
        // Markers do not inherit properties from the element they are referenced by.
        return NodeRenderer.setupInnerViewRenderContext(viewBox, context, false);
    }
}
