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
package com.github.weisj.jsvg.renderer.impl;

import java.awt.*;
import java.awt.geom.*;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.MarkerOrientation;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.Marker;
import com.github.weisj.jsvg.nodes.ShapeNode;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;

public final class ShapeRenderer {
    private static final boolean DEBUG_MARKERS = false;

    private ShapeRenderer() {}

    private static final class PaintWithOpacity {
        private final @NotNull SVGPaint paint;
        private final float opacity;

        private PaintWithOpacity(@NotNull SVGPaint paint, float opacity) {
            this.paint = paint;
            this.opacity = opacity;
        }

        boolean isVisible(@NotNull RenderContext context) {
            return opacity > 0 && paint.isVisible(context);
        }
    }

    public static final class PaintShape {
        private final @NotNull Shape shape;
        private final @Nullable Rectangle2D bounds;

        public PaintShape(@NotNull Shape shape, @Nullable Rectangle2D bounds) {
            this.shape = shape;
            this.bounds = bounds;
        }
    }

    public static final class ShapePaintContext {
        private final @NotNull RenderContext context;
        private final @NotNull Set<VectorEffect> vectorEffects;
        private final @NotNull Stroke stroke;
        private final @Nullable AffineTransform transform;

        public ShapePaintContext(@NotNull RenderContext context, @NotNull Set<VectorEffect> vectorEffects,
                @NotNull Stroke stroke, @Nullable AffineTransform transform) {
            this.context = context;
            this.vectorEffects = vectorEffects;
            this.stroke = stroke;
            this.transform = transform;
        }
    }

    public static final class ShapeMarkerInfo {
        private final @NotNull ShapeNode node;
        private final @Nullable Marker markerStart;
        private final @Nullable Marker markerMid;
        private final @Nullable Marker markerEnd;
        private final boolean shouldPaintStartEndMarkersInMiddle;

        public ShapeMarkerInfo(@NotNull ShapeNode node, @Nullable Marker markerStart, @Nullable Marker markerMid,
                @Nullable Marker markerEnd, boolean shouldPaintStartEndMarkersInMiddle) {
            this.node = node;
            this.markerStart = markerStart;
            this.markerMid = markerMid;
            this.markerEnd = markerEnd;
            this.shouldPaintStartEndMarkersInMiddle = shouldPaintStartEndMarkersInMiddle;
        }
    }

    public static void renderWithPaintOrder(@NotNull Output output, boolean canBeFilledHint,
            @NotNull PaintOrder paintOrder, @NotNull ShapePaintContext shapePaintContext,
            @NotNull PaintShape paintShape, @Nullable ShapeMarkerInfo markerInfo) {
        Set<VectorEffect> vectorEffects = shapePaintContext.vectorEffects;
        VectorEffect.applyEffects(shapePaintContext.vectorEffects, output,
                shapePaintContext.context, shapePaintContext.transform);
        Output.SafeState safeState = output.safeState();

        // FIXME: Opacity should be uniform on fill and stroke. Fill shouldn't be visible beneath stroke.
        boolean fillPainted = false;
        for (PaintOrder.Phase phase : paintOrder.phases()) {
            RenderContext phaseContext =
                    RenderContextAccessor.instance().deriveForChildGraphics(shapePaintContext.context);
            switch (phase) {
                case FILL:
                    if (canBeFilledHint) {
                        ShapeRenderer.renderShapeFill(phaseContext, output, paintShape);
                    }
                    fillPainted = true;
                    break;
                case STROKE:
                    PaintShape strokeShape = null;
                    Shape shape = paintShape.shape;
                    if (vectorEffects.contains(VectorEffect.NonScalingStroke)
                            && !vectorEffects.contains(VectorEffect.NonScalingSize)) {
                        Shape stroked = VectorEffect.applyNonScalingStroke(
                                output, phaseContext, shapePaintContext.stroke, paintShape.shape);
                        strokeShape = new PaintShape(stroked, stroked.getBounds2D());
                    }
                    ShapeRenderer.renderShapeStroke(phaseContext, output,
                            new PaintShape(shape, paintShape.bounds),
                            strokeShape, shapePaintContext.stroke,
                            !fillPainted && canBeFilledHint);
                    break;
                case MARKERS:
                    if (markerInfo != null) renderMarkers(output, phaseContext, paintShape, markerInfo);
                    break;
            }
            safeState.restore();
        }
    }

    private static void renderMarkers(@NotNull Output output, @NotNull RenderContext context,
            @NotNull PaintShape paintShape, @NotNull ShapeMarkerInfo markerInfo) {
        if (markerInfo.markerStart == null && markerInfo.markerMid == null && markerInfo.markerEnd == null) return;
        renderMarkersImpl(output, context, paintShape.shape.getPathIterator(null), markerInfo);
    }

    private static void renderShapeStroke(@NotNull RenderContext context, @NotNull Output output,
            @NotNull PaintShape paintShape, @Nullable PaintShape strokeShape,
            @Nullable Stroke stroke, boolean willBeFilledAfterwards) {
        PaintWithOpacity paintWithOpacity = new PaintWithOpacity(context.strokePaint(), context.strokeOpacity());
        if (!(stroke != null && paintWithOpacity.isVisible(context))) return;
        output.applyOpacity(paintWithOpacity.opacity);
        output.setStroke(stroke);

        PaintShape strokeShapeToPaint =
                computeEffectiveStrokeShape(context, output, paintShape, strokeShape, stroke, willBeFilledAfterwards);

        if (strokeShapeToPaint != null) {
            paintWithOpacity.paint.fillShape(output, context, strokeShapeToPaint.shape, strokeShapeToPaint.bounds);
        } else {
            paintWithOpacity.paint.drawShape(output, context, paintShape.shape, paintShape.bounds);
        }

    }

    private static PaintShape computeEffectiveStrokeShape(@NotNull RenderContext context, @NotNull Output output,
            @NotNull PaintShape paintShape, @Nullable PaintShape strokeShape, @NotNull Stroke stroke,
            boolean willBeFilledAfterwards) {
        PaintShape strokeShapeToPaint = strokeShape;

        boolean removeFillArea = output.hasMaskedPaint()
                && willBeFilledAfterwards
                && context.fillPaint().isVisible(context)
                && context.fillOpacity() == 1
                && output.currentOpacity() == 1;
        if (removeFillArea) {
            Area s = new Area(strokeShape != null
                    ? strokeShape.shape
                    : stroke.createStrokedShape(paintShape.shape));
            s.subtract(new Area(paintShape.shape));
            Rectangle2D strokedBounds = paintShape.bounds;
            if (strokedBounds != null) {
                if (stroke instanceof BasicStroke) {
                    strokedBounds = GeometryUtil.grow(strokedBounds, ((BasicStroke) stroke).getLineWidth() / 2);
                } else {
                    strokedBounds = s.getBounds2D();
                }
            }
            strokeShapeToPaint = new PaintShape(s, strokedBounds);
        }
        return strokeShapeToPaint;
    }

    private static void renderShapeFill(@NotNull RenderContext context, @NotNull Output output,
            @NotNull PaintShape paintShape) {
        PaintWithOpacity paintWithOpacity = new PaintWithOpacity(context.fillPaint(), context.fillOpacity());
        if (!paintWithOpacity.isVisible(context)) return;
        output.applyOpacity(paintWithOpacity.opacity);
        paintWithOpacity.paint.fillShape(output, context, paintShape.shape, paintShape.bounds);
    }

    private static void renderMarkersImpl(@NotNull Output output, @NotNull RenderContext context,
            @NotNull PathIterator iterator, @NotNull ShapeMarkerInfo markerInfo) {
        float[] args = new float[6];

        float x = 0;
        float y = 0;
        float xStart = 0;
        float yStart = 0;

        float dxIn = 0;
        float dyIn = 0;
        float dxOut;
        float dyOut;

        Marker start = markerInfo.markerStart;
        Marker mid = markerInfo.markerMid;
        Marker end = markerInfo.markerEnd;

        boolean onlyFirst = mid == null && end == null;

        Marker markerToPaint = null;
        MarkerOrientation.MarkerType markerToPaintType = null;

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(args);
            iterator.next();

            Marker nextMarker = iterator.isDone()
                    ? end
                    : mid;
            MarkerOrientation.MarkerType nextMarkerType = iterator.isDone()
                    ? MarkerOrientation.MarkerType.END
                    : MarkerOrientation.MarkerType.MID;

            float xPaint = x;
            float yPaint = y;
            float dx = dxIn;
            float dy = dyIn;

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    dxIn = 0;
                    dyIn = 0;
                    x = xStart = args[0];
                    y = yStart = args[1];
                    if (markerInfo.shouldPaintStartEndMarkersInMiddle || markerToPaint == null) {
                        nextMarker = start;
                        nextMarkerType = MarkerOrientation.MarkerType.START;
                    }
                    if (markerToPaint != null) {
                        paintSingleMarker(markerInfo.node, context, output, markerToPaintType, markerToPaint,
                                xPaint, yPaint, 0, 0, dx, dy);
                        if (onlyFirst) return;
                    }
                    markerToPaint = nextMarker;
                    markerToPaintType = nextMarkerType;
                    continue;
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
                    if (markerInfo.shouldPaintStartEndMarkersInMiddle) {
                        nextMarker = end;
                        nextMarkerType = MarkerOrientation.MarkerType.END;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }

            paintSingleMarker(markerInfo.node, context, output, markerToPaintType, markerToPaint,
                    xPaint, yPaint, dx, dy, dxOut, dyOut);
            if (onlyFirst) return;

            markerToPaint = nextMarker;
            markerToPaintType = nextMarkerType;
        }
        paintSingleMarker(markerInfo.node, context, output, markerToPaintType, markerToPaint,
                x, y, dxIn, dyIn, 0, 0);
    }

    public static void paintSingleMarker(@NotNull ShapeNode shapeNode, @NotNull RenderContext context,
            @NotNull Output output, @Nullable MarkerOrientation.MarkerType type, @Nullable Marker marker,
            float x, float y, float dxIn, float dyIn, float dxOut, float dyOut) {
        if (marker == null) return;
        assert type != null;

        MarkerOrientation orientation = marker.orientation();
        float rotation = orientation.orientationFor(type, dxIn, dyIn, dxOut, dyOut);

        Output markerOutput = output.createChild();
        RenderContext markerContext = RenderContextAccessor.instance().deriveForChildGraphics(context);

        markerContext.translate(markerOutput, x, y);

        if (DEBUG_MARKERS) {
            markerOutput.debugPaint(g -> paintDebugMarker(markerContext, g, marker, rotation));
        }
        markerContext.rotate(markerOutput, rotation);

        NodeRenderer.renderNode(marker, markerContext, markerOutput, shapeNode);

        markerOutput.dispose();
    }

    private static void paintDebugMarker(@NotNull RenderContext context, @NotNull Graphics2D g,
            @NotNull Marker marker, float rotation) {
        FloatSize size = marker.size(context);

        Path2D p = new Path2D.Float();
        p.moveTo(0, size.height / 2f);
        p.lineTo(size.width, size.height / 2f);
        p.moveTo(0.8 * size.width, 0.35f * size.height);
        p.lineTo(size.width, size.height / 2f);
        p.lineTo(0.8 * size.width, 0.65f * size.height);

        g.setStroke(new BasicStroke(0.5f));

        g.setColor(Color.MAGENTA.darker().darker());
        g.draw(new Rectangle2D.Float(0, 0, size.width, size.height));
        g.draw(p);

        g.rotate(rotation);

        g.setColor(Color.MAGENTA);
        g.draw(new Rectangle2D.Float(0, 0, size.width, size.height));
        g.draw(p);
    }
}
