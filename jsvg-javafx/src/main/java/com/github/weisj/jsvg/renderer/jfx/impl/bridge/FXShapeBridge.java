/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.impl.bridge;

import java.awt.*;
import java.awt.geom.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;

import org.jetbrains.annotations.NotNull;

public class FXShapeBridge {


    public static FillRule toFillRule(int awtWindingRule) {
        switch (awtWindingRule) {
            case PathIterator.WIND_EVEN_ODD:
                return FillRule.EVEN_ODD;
            case PathIterator.WIND_NON_ZERO:
                return FillRule.NON_ZERO;
            default:
                throw new IllegalArgumentException("Unknown winding rule: " + awtWindingRule);
        }
    }

    public static ArcType toArcType(int awtArcType) {
        switch (awtArcType) {
            case Arc2D.OPEN:
                return ArcType.OPEN;
            case Arc2D.CHORD:
                return ArcType.CHORD;
            case Arc2D.PIE:
                return ArcType.ROUND;
            default:
                throw new IllegalArgumentException("Unknown arc type: " + awtArcType);
        }
    }

    public static void drawShape(@NotNull GraphicsContext ctx, @NotNull Shape shape) {
        if (shape instanceof Line2D) {
            Line2D line = (Line2D) shape;
            ctx.strokeLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) shape;
            ctx.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        } else if (shape instanceof RoundRectangle2D) {
            RoundRectangle2D roundRect = (RoundRectangle2D) shape;
            ctx.strokeRoundRect(roundRect.getX(), roundRect.getY(), roundRect.getWidth(), roundRect.getHeight(),
                    roundRect.getArcWidth(), roundRect.getArcHeight());
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D ellipse = (Ellipse2D) shape;
            ctx.strokeOval(ellipse.getX(), ellipse.getY(), ellipse.getWidth(), ellipse.getHeight());
        } else if (shape instanceof Arc2D) {
            Arc2D arc2D = (Arc2D) shape;
            ctx.strokeArc(arc2D.getX(), arc2D.getY(), arc2D.getWidth(), arc2D.getHeight(), arc2D.getAngleStart(),
                    arc2D.getAngleExtent(), toArcType(arc2D.getArcType()));
        } else if (shape instanceof QuadCurve2D) {
            QuadCurve2D quad = (QuadCurve2D) shape;
            ctx.beginPath();
            ctx.moveTo(quad.getX1(), quad.getY1());
            ctx.quadraticCurveTo(quad.getCtrlX(), quad.getCtrlY(), quad.getX2(), quad.getY2());
            ctx.stroke();
        } else if (shape instanceof CubicCurve2D) {
            CubicCurve2D cubic = (CubicCurve2D) shape;
            ctx.beginPath();
            ctx.moveTo(cubic.getX1(), cubic.getY1());
            ctx.bezierCurveTo(cubic.getCtrlX1(), cubic.getCtrlY1(), cubic.getCtrlX2(), cubic.getCtrlY2(), cubic.getX2(),
                    cubic.getY2());
            ctx.stroke();
        } else {
            FillRule prevFillRule = ctx.getFillRule();
            PathIterator awtIterator = shape.getPathIterator(null);
            applyPathIterator(ctx, awtIterator);
            applyWindingRule(ctx, awtIterator.getWindingRule());
            ctx.stroke();
            ctx.setFillRule(prevFillRule);
        }
    }

    public static void fillShape(@NotNull GraphicsContext ctx, @NotNull Shape shape) {
        if (shape instanceof Line2D) {
            // do nothing - lines can't be filled
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) shape;
            ctx.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        } else if (shape instanceof RoundRectangle2D) {
            RoundRectangle2D roundRect = (RoundRectangle2D) shape;
            ctx.fillRoundRect(roundRect.getX(), roundRect.getY(), roundRect.getWidth(), roundRect.getHeight(),
                    roundRect.getArcWidth(), roundRect.getArcHeight());
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D ellipse = (Ellipse2D) shape;
            ctx.fillOval(ellipse.getX(), ellipse.getY(), ellipse.getWidth(), ellipse.getHeight());
        } else if (shape instanceof Arc2D) {
            Arc2D arc2D = (Arc2D) shape;
            ctx.fillArc(arc2D.getX(), arc2D.getY(), arc2D.getWidth(), arc2D.getHeight(), arc2D.getAngleStart(),
                    arc2D.getAngleExtent(), toArcType(arc2D.getArcType()));
        } else if (shape instanceof QuadCurve2D) {
            QuadCurve2D quad = (QuadCurve2D) shape;
            ctx.beginPath();
            ctx.moveTo(quad.getX1(), quad.getY1());
            ctx.quadraticCurveTo(quad.getCtrlX(), quad.getCtrlY(), quad.getX2(), quad.getY2());
            ctx.fill();
        } else if (shape instanceof CubicCurve2D) {
            CubicCurve2D cubic = (CubicCurve2D) shape;
            ctx.beginPath();
            ctx.moveTo(cubic.getX1(), cubic.getY1());
            ctx.bezierCurveTo(cubic.getCtrlX1(), cubic.getCtrlY1(), cubic.getCtrlX2(), cubic.getCtrlY2(), cubic.getX2(),
                    cubic.getY2());
            ctx.fill();
        } else {
            FillRule prevFillRule = ctx.getFillRule();
            PathIterator awtIterator = shape.getPathIterator(null);
            applyPathIterator(ctx, awtIterator);
            applyWindingRule(ctx, awtIterator.getWindingRule());
            ctx.fill();
            ctx.setFillRule(prevFillRule);
        }
    }

    public static void applyWindingRule(@NotNull GraphicsContext ctx, int awtWindingRule) {
        ctx.setFillRule(toFillRule(awtWindingRule));
    }

    public static void applyPathIterator(@NotNull GraphicsContext ctx, @NotNull PathIterator awtIterator) {
        ctx.beginPath();

        float[] segment = new float[6];
        for (; !awtIterator.isDone(); awtIterator.next()) {
            int type = awtIterator.currentSegment(segment);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    ctx.moveTo(segment[0], segment[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    ctx.lineTo(segment[0], segment[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    ctx.quadraticCurveTo(segment[0], segment[1], segment[2], segment[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    ctx.bezierCurveTo(segment[0], segment[1], segment[2], segment[3], segment[4], segment[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    ctx.closePath();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown segment type: " + type);
            }
        }
    }
}
