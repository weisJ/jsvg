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
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.geometry.MeasurableShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.ReversePathIterator;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.renderer.PaintContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.ShapeRenderer;

public abstract class ShapeNode extends RenderableSVGNode implements HasShape {
    private PaintContext paintContext;
    private Length pathLength;
    private MeasurableShape shape;

    private Marker markerStart;
    private Marker markerMid;
    private Marker markerEnd;

    @Override
    public final void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        pathLength = attributeNode.getLength("pathLength", Length.UNSPECIFIED);
        paintContext = PaintContext.parse(attributeNode);
        shape = buildShape(attributeNode);

        markerStart = attributeNode.getElementByHref(Marker.class, "marker-start");
        markerMid = attributeNode.getElementByHref(Marker.class, "marker-mid");
        markerEnd = attributeNode.getElementByHref(Marker.class, "marker-end");
    }

    protected abstract @NotNull MeasurableShape buildShape(@NotNull AttributeNode attributeNode);

    @Override
    public @NotNull SVGShape shape() {
        return shape;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return super.isVisible(context);
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        MeasureContext measureContext = context.measureContext();
        Shape paintShape = shape.shape(context);
        Rectangle2D bounds = shape.usesOptimizedBoundsCalculation()
                ? shape.bounds(context, false)
                : paintShape.getBounds2D();
        float pathLengthFactor = 1f;
        if (pathLength.isSpecified()) {
            double effectiveLength = pathLength.resolveLength(measureContext);
            double actualLength = shape.pathLength(measureContext);
            pathLengthFactor = (float) (actualLength / effectiveLength);
        }
        ShapeRenderer.renderShape(context, paintContext, g, paintShape, bounds,
                shape.canBeFilled(), true, pathLengthFactor);

        renderMarkers(context, paintShape, g);
    }

    protected void renderMarkers(@NotNull RenderContext context, @NotNull Shape shape, @NotNull Graphics2D g) {
        if (markerStart == null && markerMid == null && markerEnd == null) return;
        if (markerStart != null) {
            renderMarkers(context, g, shape.getPathIterator(null),
                    markerStart, markerMid, markerEnd);
        } else {
            // use reverse path iterator. If there is no markerMid this avoids stepping through
            // the complete path iterator.
            renderMarkers(context, g, new ReversePathIterator(shape.getPathIterator(null)),
                    markerEnd, markerMid, markerStart);
        }
    }

    protected boolean shouldPaintStartEndMarkersInMiddle() {
        return true;
    }

    private void renderMarkers(@NotNull RenderContext context, @NotNull Graphics2D g,
            @NotNull PathIterator iterator,
            @Nullable Marker start, @Nullable Marker mid, @Nullable Marker end) {
        float[] args = new float[6];
        float xStart = 0;
        float yStart = 0;

        // Todo: Marker orientation

        boolean first = true;
        boolean onlyFirst = mid == null && end == null;
        boolean startEndInMiddle = shouldPaintStartEndMarkersInMiddle();
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(args);
            iterator.next();

            if (!first && onlyFirst) return;

            if (first && type != PathIterator.SEG_MOVETO) {
                paintSingleMarker(context, g, start, xStart, yStart);
                first = false;
            }

            Marker marker = mid;
            if (first) marker = start;
            if (iterator.isDone()) marker = end;

            first = false;

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    xStart = args[0];
                    yStart = args[1];
                    if (startEndInMiddle) marker = start;
                    paintSingleMarker(context, g, marker, xStart, yStart);
                    break;
                case PathIterator.SEG_LINETO:
                    paintSingleMarker(context, g, marker, args[0], args[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    paintSingleMarker(context, g, marker, args[2], args[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    paintSingleMarker(context, g, marker, args[4], args[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    if (startEndInMiddle) marker = end;
                    paintSingleMarker(context, g, marker, xStart, yStart);
                    break;
            }
        }
    }

    private void paintSingleMarker(@NotNull RenderContext context, @NotNull Graphics2D g,
            @Nullable Marker marker, float x, float y) {
        if (marker == null) return;
        Graphics2D markerGraphics = (Graphics2D) g.create();
        markerGraphics.translate(x, y);
        marker.render(context, markerGraphics);
    }
}
