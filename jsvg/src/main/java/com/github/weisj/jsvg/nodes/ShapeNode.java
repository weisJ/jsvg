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
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.MarkerOrientation;
import com.github.weisj.jsvg.attributes.Radian;
import com.github.weisj.jsvg.geometry.MeasurableShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
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

        // Todo: marker property prototype
        markerStart = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-start"));
        markerMid = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-mid"));
        markerEnd = attributeNode.getElementByHref(Marker.class, attributeNode.getValue("marker-end"));
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
    public final void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
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

    private void renderMarkers(@NotNull RenderContext context, @NotNull Shape shape, @NotNull Graphics2D g) {
        if (markerStart == null && markerMid == null && markerEnd == null) return;
        renderMarkers(context, g, shape.getPathIterator(null), markerStart, markerMid, markerEnd);
    }

    protected boolean shouldPaintStartEndMarkersInMiddle() {
        return true;
    }

    private void renderMarkers(@NotNull RenderContext context, @NotNull Graphics2D g,
            @NotNull PathIterator iterator,
            @Nullable Marker start, @Nullable Marker mid, @Nullable Marker end) {
        float[] args = new float[6];

        float x = 0;
        float y = 0;
        float xStart = 0;
        float yStart = 0;

        float dxIn = 0;
        float dyIn = 0;
        float dxOut = 0;
        float dyOut = 0;

        boolean onlyFirst = mid == null && end == null;
        boolean startEndInMiddle = shouldPaintStartEndMarkersInMiddle();

        Marker markerToPaint = null;
        MarkerOrientation.MarkerType markerToPaintType = null;

        pathWhile: while (!iterator.isDone()) {
            int type = iterator.currentSegment(args);
            iterator.next();

            Marker nextMarker = iterator.isDone()
                    ? end
                    : mid;
            MarkerOrientation.MarkerType nextMarkerType = iterator.isDone()
                    ? MarkerOrientation.MarkerType.End
                    : MarkerOrientation.MarkerType.Mid;

            float xPaint = x;
            float yPaint = y;
            float dx = dxIn;
            float dy = dyIn;

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    dxIn = dxOut = 0;
                    dyIn = dyOut = 0;
                    x = xStart = args[0];
                    y = yStart = args[1];
                    if (startEndInMiddle) {
                        nextMarker = start;
                        nextMarkerType = MarkerOrientation.MarkerType.Start;
                    }
                    if (markerToPaint != null) {
                        paintSingleMarker(context, g, markerToPaintType, markerToPaint,
                                xPaint, yPaint, 0, 0, dx, dy);
                    }
                    markerToPaint = nextMarker;
                    markerToPaintType = nextMarkerType;
                    continue pathWhile;
                case PathIterator.SEG_LINETO:
                    dxOut = dxIn = args[0] - x;
                    dyOut = dyIn = args[1] - y;
                    x = args[0];
                    y = args[1];
                    break;
                case PathIterator.SEG_QUADTO:
                    dxOut = args[0] - x;
                    dyOut = args[1] - y;
                    dxIn = args[2] - args[0];
                    dyIn = args[3] - args[1];
                    x = args[2];
                    y = args[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    dxOut = args[0] - x;
                    dyOut = args[1] - y;
                    dxIn = args[4] - args[2];
                    dyIn = args[5] - args[3];
                    x = args[4];
                    y = args[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    dxOut = dxIn = xStart - x;
                    dyOut = dyIn = yStart - y;
                    x = xStart;
                    y = yStart;
                    if (startEndInMiddle) {
                        nextMarker = end;
                        nextMarkerType = MarkerOrientation.MarkerType.End;
                    }
                    break;
            }

            paintSingleMarker(context, g, markerToPaintType, markerToPaint,
                    xPaint, yPaint, dx, dy, dxOut, dyOut);
            if (onlyFirst) return;

            markerToPaint = nextMarker;
            markerToPaintType = nextMarkerType;
        }
        paintSingleMarker(context, g, markerToPaintType, markerToPaint, x, y, dxIn, dyIn, 0, 0);
    }

    private void paintSingleMarker(@NotNull RenderContext context, @NotNull Graphics2D g,
            MarkerOrientation.MarkerType type,
            @Nullable Marker marker, float x, float y, float dxIn, float dyIn, float dxOut, float dyOut) {
        if (marker == null) return;
        MarkerOrientation orientation = marker.orientation();
        @Radian float rotation = orientation.orientationFor(type, dxIn, dyIn, dxOut, dyOut);

        Graphics2D markerGraphics = (Graphics2D) g.create();
        markerGraphics.translate(x, y);
        FloatSize size = marker.size(context);

        GeneralPath p = new GeneralPath();
        p.moveTo(0, size.height / 2f);
        p.lineTo(size.width, size.height / 2f);
        p.moveTo(0.8 * size.width, 0.35f * size.height);
        p.lineTo(size.width, size.height / 2f);
        p.lineTo(0.8 * size.width, 0.65f * size.height);


        markerGraphics.setStroke(new BasicStroke(0.5f));

        markerGraphics.setColor(Color.MAGENTA.darker().darker());
        markerGraphics.draw(new Rectangle2D.Float(0, 0, size.width, size.height));
        markerGraphics.draw(p);

        markerGraphics.rotate(rotation);

        markerGraphics.setColor(Color.MAGENTA);
        markerGraphics.draw(new Rectangle2D.Float(0, 0, size.width, size.height));
        markerGraphics.draw(p);

        marker.render(context, markerGraphics);
        markerGraphics.dispose();
    }
}
