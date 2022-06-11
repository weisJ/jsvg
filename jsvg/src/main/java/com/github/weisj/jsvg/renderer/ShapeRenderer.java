/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.MarkerOrientation;
import com.github.weisj.jsvg.attributes.Radian;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.nodes.Marker;
import com.github.weisj.jsvg.nodes.ShapeNode;

public final class ShapeRenderer {
    private static final boolean DEBUG_MARKERS = false;

    private ShapeRenderer() {}

    private static class PaintWithOpacity {
        private final @NotNull SVGPaint paint;
        private final float opacity;

        private PaintWithOpacity(@NotNull SVGPaint paint, float opacity) {
            this.paint = paint;
            this.opacity = opacity;
        }

        boolean isVisible() {
            return opacity > 0 && paint.isVisible();
        }
    }

    public static void renderShapeStroke(@NotNull RenderContext context, @NotNull Graphics2D g,
            @NotNull Shape shape, @Nullable Rectangle2D bounds, @Nullable Stroke stroke) {
        PaintWithOpacity paintWithOpacity = new PaintWithOpacity(context.strokePaint(), context.strokeOpacity());
        if (!(stroke != null && paintWithOpacity.isVisible())) return;
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(paintWithOpacity.opacity));
        g.setStroke(stroke);
        paintWithOpacity.paint.drawShape(g, context.measureContext(), shape, bounds);
        g.setComposite(composite);
    }

    public static void renderShapeFill(@NotNull RenderContext context, @NotNull Graphics2D g,
            @NotNull Shape shape, @Nullable Rectangle2D bounds) {
        PaintWithOpacity paintWithOpacity = new PaintWithOpacity(context.fillPaint(), context.fillOpacity());
        if (!paintWithOpacity.isVisible()) return;
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(paintWithOpacity.opacity));
        paintWithOpacity.paint.fillShape(g, context.measureContext(), shape, bounds);
        g.setComposite(composite);
    }

    public static void renderMarkers(@NotNull ShapeNode shapeNode, @NotNull RenderContext context,
            @NotNull Graphics2D g, @NotNull PathIterator iterator, boolean shouldPaintStartEndMarkersInMiddle,
            @Nullable Marker start, @Nullable Marker mid, @Nullable Marker end) {
        float[] args = new float[6];

        float x = 0;
        float y = 0;
        float xStart = 0;
        float yStart = 0;

        float dxIn = 0;
        float dyIn = 0;
        float dxOut;
        float dyOut;

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
                    if (shouldPaintStartEndMarkersInMiddle || markerToPaint == null) {
                        nextMarker = start;
                        nextMarkerType = MarkerOrientation.MarkerType.START;
                    }
                    if (markerToPaint != null) {
                        paintSingleMarker(shapeNode, context, g, markerToPaintType, markerToPaint,
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
                    if (shouldPaintStartEndMarkersInMiddle) {
                        nextMarker = end;
                        nextMarkerType = MarkerOrientation.MarkerType.END;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }

            paintSingleMarker(shapeNode, context, g, markerToPaintType, markerToPaint,
                    xPaint, yPaint, dx, dy, dxOut, dyOut);
            if (onlyFirst) return;

            markerToPaint = nextMarker;
            markerToPaintType = nextMarkerType;
        }
        paintSingleMarker(shapeNode, context, g, markerToPaintType, markerToPaint,
                x, y, dxIn, dyIn, 0, 0);
    }

    public static void paintSingleMarker(@NotNull ShapeNode shapeNode, @NotNull RenderContext context,
            @NotNull Graphics2D g,
            @Nullable MarkerOrientation.MarkerType type, @Nullable Marker marker,
            float x, float y, float dxIn, float dyIn, float dxOut, float dyOut) {
        if (marker == null) return;
        assert type != null;

        MarkerOrientation orientation = marker.orientation();
        @Radian float rotation = orientation.orientationFor(type, dxIn, dyIn, dxOut, dyOut);

        Graphics2D markerGraphics = (Graphics2D) g.create();
        markerGraphics.translate(x, y);

        if (DEBUG_MARKERS) {
            Graphics2D debugGraphics = (Graphics2D) markerGraphics.create();
            paintDebugMarker(context, debugGraphics, marker, rotation);
            debugGraphics.dispose();
        }
        markerGraphics.rotate(rotation);

        try (NodeRenderer.Info info = NodeRenderer.createRenderInfo(marker, context, markerGraphics, shapeNode)) {
            if (info != null) info.renderable.render(info.context, info.graphics());
        }

        markerGraphics.dispose();
    }

    private static void paintDebugMarker(@NotNull RenderContext context, @NotNull Graphics2D g,
            @NotNull Marker marker, @Radian float rotation) {
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
