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

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.FontSize;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.prototype.*;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.AttributeNode.ElementRelation;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ShapeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;

public abstract class ShapeNode extends RenderableSVGNode
        implements HasShape, HasPaintContext, HasFontContext, HasVectorEffects, Instantiator {

    private PaintContext paintContext;
    private FontSize fontSize;
    private Length fontSizeAdjust;

    private Length pathLength;
    private SVGShape shape;

    private Marker markerStart;
    private Marker markerMid;
    private Marker markerEnd;

    private Set<VectorEffect> vectorEffects;

    @Override
    public @NotNull PaintContext paintContext() {
        return paintContext;
    }

    @Override
    public @NotNull Mutator<MeasurableFontSpec> fontSpec() {
        return s -> s.withFontSize(fontSize, fontSizeAdjust);
    }

    public @NotNull SVGShape shape() {
        return shape;
    }

    @Override
    public @NotNull Set<VectorEffect> vectorEffects() {
        return vectorEffects;
    }

    @Override
    public final void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        paintContext = PaintContext.parse(attributeNode);

        fontSize = FontParser.parseFontSize(attributeNode);
        fontSizeAdjust = FontParser.parseSizeAdjust(attributeNode);

        shape = buildShape(attributeNode);
        pathLength = attributeNode.getLength("pathLength", PercentageDimension.NONE, Length.UNSPECIFIED);

        // Todo: These are actually inheritable and hence have to go into the RenderContext
        // Todo: The marker shorthand is a bit more complicated than just being a template.
        // https://www.w3.org/TR/svg-markers/#MarkerShorthand
        Marker template = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker"),
                ElementRelation.TEMPLATE);
        markerStart = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-start"),
                ElementRelation.TEMPLATE);
        if (markerStart == null) markerStart = template;

        markerMid = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-mid"),
                ElementRelation.TEMPLATE);
        if (markerMid == null) markerMid = template;

        markerEnd = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-end"),
                ElementRelation.TEMPLATE);
        if (markerEnd == null) markerEnd = template;

        vectorEffects = VectorEffect.parse(attributeNode);
    }

    protected abstract @NotNull SVGShape buildShape(@NotNull AttributeNode attributeNode);

    @Override
    public @NotNull Shape untransformedElementShape(@NotNull RenderContext context, Box box) {
        Shape realShape = shape.shape(context);
        switch (box) {
            case BoundingBox:
                return realShape;
            case StrokeBox:
                Area area = new Area(realShape);
                area.add(new Area(computeEffectiveStroke(context).createStrokedShape(realShape)));
                return area;
            default:
                throw new IllegalStateException("Unexpected value: " + box);
        }
    }

    @Override
    public @NotNull Rectangle2D untransformedElementBounds(@NotNull RenderContext context, Box box) {
        Rectangle2D bounds = shape.bounds(context, true);
        switch (box) {
            case BoundingBox:
                return bounds;
            case StrokeBox: {
                LengthValue strokeWidth = RenderContextAccessor.instance().strokeContext(context).strokeWidth;
                if (strokeWidth != null) {
                    float stroke = strokeWidth.resolve(context.measureContext());
                    if (stroke > 0) bounds = GeometryUtil.grow(bounds, stroke);
                }
                return bounds;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + box);
        }
    }

    @Override
    public boolean canInstantiate(@NotNull SVGNode node) {
        return node instanceof Marker;
    }

    private @NotNull Stroke computeEffectiveStroke(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        float pathLengthFactor = 1f;
        if (pathLength.isSpecified()) {
            double effectiveLength = pathLength.resolve(measureContext);
            double actualLength = shape.pathLength(context);
            pathLengthFactor = (float) (actualLength / effectiveLength);
        }
        return context.stroke(pathLengthFactor);
    }

    @Override
    public final void render(@NotNull RenderContext context, @NotNull Output output) {
        Shape paintShape = shape.shape(context);
        @Nullable Rectangle2D bounds = shape.usesOptimizedBoundsCalculation()
                ? shape.bounds(context, false)
                : null;

        Stroke effectiveStroke = computeEffectiveStroke(context);
        ShapeRenderer.renderWithPaintOrder(output, shape.canBeFilled(),
                RenderContextAccessor.instance().paintOrder(context),
                new ShapeRenderer.ShapePaintContext(context, vectorEffects(), effectiveStroke,
                        GeometryUtil.toAwtTransform(context, transform())),
                new ShapeRenderer.PaintShape(paintShape, bounds),
                new ShapeRenderer.ShapeMarkerInfo(this, markerStart, markerMid, markerEnd,
                        shouldPaintStartEndMarkersInMiddle()));
    }

    protected boolean shouldPaintStartEndMarkersInMiddle() {
        return true;
    }
}
