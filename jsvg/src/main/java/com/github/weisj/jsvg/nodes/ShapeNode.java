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
package com.github.weisj.jsvg.nodes;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.FontSize;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.geometry.MeasurableShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.prototype.*;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.PaintContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.ShapeRenderer;

public abstract class ShapeNode extends RenderableSVGNode
        implements HasShape, HasPaintContext, HasFontContext, HasVectorEffects, Instantiator {

    private PaintOrder paintOrder;

    private PaintContext paintContext;
    private FontSize fontSize;
    private Length fontSizeAdjust;

    private Length pathLength;
    private MeasurableShape shape;

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

    public @NotNull MeasurableShape shape() {
        return shape;
    }

    @Override
    public @NotNull Set<VectorEffect> vectorEffects() {
        return vectorEffects;
    }

    @Override
    public final void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        paintOrder = PaintOrder.parse(attributeNode);
        paintContext = PaintContext.parse(attributeNode);

        fontSize = FontParser.parseFontSize(attributeNode);
        fontSizeAdjust = FontParser.parseSizeAdjust(attributeNode);

        shape = buildShape(attributeNode);
        pathLength = attributeNode.getLength("pathLength", Length.UNSPECIFIED);

        // Todo: These are actually inheritable and hence have to go into the RenderContext
        // Todo: The marker shorthand is a bit more complicated than just being a template.
        // https://www.w3.org/TR/svg-markers/#MarkerShorthand
        Marker template = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker"));
        markerStart = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-start"));
        if (markerStart == null) markerStart = template;

        markerMid = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-mid"));
        if (markerMid == null) markerMid = template;

        markerEnd = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-end"));
        if (markerEnd == null) markerEnd = template;

        vectorEffects = VectorEffect.parse(attributeNode);
    }

    protected abstract @NotNull MeasurableShape buildShape(@NotNull AttributeNode attributeNode);

    @Override
    public @NotNull Shape untransformedElementShape(@NotNull RenderContext context) {
        return shape.shape(context);
    }

    @Override
    public @NotNull Rectangle2D untransformedElementBounds(@NotNull RenderContext context) {
        return shape.bounds(context, true);
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return super.isVisible(context);
    }

    @Override
    public boolean canInstantiate(@NotNull SVGNode node) {
        return node instanceof Marker;
    }

    private @NotNull Stroke computeEffectiveStroke(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        float pathLengthFactor = 1f;
        if (pathLength.isSpecified()) {
            double effectiveLength = pathLength.resolveLength(measureContext);
            double actualLength = shape.pathLength(measureContext);
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
        ShapeRenderer.renderWithPaintOrder(output, shape.canBeFilled(), paintOrder,
                new ShapeRenderer.ShapePaintContext(context, vectorEffects(), effectiveStroke, transform()),
                new ShapeRenderer.PaintShape(paintShape, bounds),
                new ShapeRenderer.ShapeMarkerInfo(this, markerStart, markerMid, markerEnd,
                        shouldPaintStartEndMarkersInMiddle()));
    }

    protected boolean shouldPaintStartEndMarkersInMiddle() {
        return true;
    }
}
