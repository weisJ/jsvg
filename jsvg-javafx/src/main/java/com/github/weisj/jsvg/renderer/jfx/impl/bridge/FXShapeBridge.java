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

    public static void strokeShape(@NotNull GraphicsContext ctx, @NotNull Shape shape) {
        if (shape instanceof Line2D) {
            strokeLine(ctx, (Line2D) shape);
        } else if (shape instanceof Rectangle2D) {
            strokeRect(ctx, (Rectangle2D) shape);
        } else if (shape instanceof RoundRectangle2D) {
            strokeRoundRect(ctx, (RoundRectangle2D) shape);
        } else if (shape instanceof Ellipse2D) {
            strokeEllipse(ctx, (Ellipse2D) shape);
        } else if (shape instanceof Arc2D) {
            strokeArc(ctx, (Arc2D) shape);
        } else if (shape instanceof QuadCurve2D) {
            strokeQuadCurve(ctx, (QuadCurve2D) shape);
        } else if (shape instanceof CubicCurve2D) {
            strokeCubicCurve(ctx, (CubicCurve2D) shape);
        } else {
            strokeShapeAsPath(ctx, shape);
        }
    }

    public static void fillShape(@NotNull GraphicsContext ctx, @NotNull Shape shape) {
        if (shape instanceof Line2D) {
            // do nothing - lines can't be filled
        } else if (shape instanceof Rectangle2D) {
            fillRect(ctx, (Rectangle2D) shape);
        } else if (shape instanceof RoundRectangle2D) {
            fillRoundRect(ctx, (RoundRectangle2D) shape);
        } else if (shape instanceof Ellipse2D) {
            fillEllipse(ctx, (Ellipse2D) shape);
        } else if (shape instanceof Arc2D) {
            fillArc(ctx, (Arc2D) shape);
        } else if (shape instanceof QuadCurve2D) {
            fillQuadCurve(ctx, (QuadCurve2D) shape);
        } else if (shape instanceof CubicCurve2D) {
            fillCubicCurve(ctx, (CubicCurve2D) shape);
        } else {
            fillShapeAsPath(ctx, shape);
        }
    }

    public static void strokeLine(@NotNull GraphicsContext ctx, @NotNull Line2D line) {
        ctx.strokeLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public static void strokeRect(@NotNull GraphicsContext ctx, @NotNull Rectangle2D rect) {
        ctx.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    public static void strokeRoundRect(@NotNull GraphicsContext ctx, @NotNull RoundRectangle2D roundRect) {
        ctx.strokeRoundRect(roundRect.getX(), roundRect.getY(), roundRect.getWidth(), roundRect.getHeight(),
                roundRect.getArcWidth(), roundRect.getArcHeight());
    }

    public static void strokeEllipse(@NotNull GraphicsContext ctx, @NotNull Ellipse2D ellipse) {
        ctx.strokeOval(ellipse.getX(), ellipse.getY(), ellipse.getWidth(), ellipse.getHeight());
    }

    public static void strokeArc(@NotNull GraphicsContext ctx, @NotNull Arc2D arc2D) {
        ctx.strokeArc(arc2D.getX(), arc2D.getY(), arc2D.getWidth(), arc2D.getHeight(), arc2D.getAngleStart(),
                arc2D.getAngleExtent(), toArcType(arc2D.getArcType()));
    }

    public static void strokeQuadCurve(@NotNull GraphicsContext ctx, @NotNull QuadCurve2D quad) {
        ctx.beginPath();
        ctx.moveTo(quad.getX1(), quad.getY1());
        ctx.quadraticCurveTo(quad.getCtrlX(), quad.getCtrlY(), quad.getX2(), quad.getY2());
        ctx.stroke();
    }

    public static void strokeCubicCurve(@NotNull GraphicsContext ctx, @NotNull CubicCurve2D cubic) {
        ctx.beginPath();
        ctx.moveTo(cubic.getX1(), cubic.getY1());
        ctx.bezierCurveTo(cubic.getCtrlX1(), cubic.getCtrlY1(), cubic.getCtrlX2(), cubic.getCtrlY2(), cubic.getX2(),
                cubic.getY2());
        ctx.stroke();
    }

    public static void strokeShapeAsPath(@NotNull GraphicsContext ctx, @NotNull Shape shape) {
        FillRule prevFillRule = ctx.getFillRule();
        PathIterator awtIterator = shape.getPathIterator(null);
        appendPathIterator(ctx, awtIterator);
        applyWindingRule(ctx, awtIterator.getWindingRule());
        ctx.stroke();
        ctx.setFillRule(prevFillRule);
    }

    public static void fillRect(@NotNull GraphicsContext ctx, @NotNull Rectangle2D rect) {
        ctx.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    public static void fillRoundRect(@NotNull GraphicsContext ctx, @NotNull RoundRectangle2D roundRect) {
        ctx.fillRoundRect(roundRect.getX(), roundRect.getY(), roundRect.getWidth(), roundRect.getHeight(),
                roundRect.getArcWidth(), roundRect.getArcHeight());
    }

    public static void fillEllipse(@NotNull GraphicsContext ctx, @NotNull Ellipse2D ellipse) {
        ctx.fillOval(ellipse.getX(), ellipse.getY(), ellipse.getWidth(), ellipse.getHeight());
    }

    public static void fillArc(@NotNull GraphicsContext ctx, @NotNull Arc2D arc2D) {
        ctx.fillArc(arc2D.getX(), arc2D.getY(), arc2D.getWidth(), arc2D.getHeight(), arc2D.getAngleStart(),
                arc2D.getAngleExtent(), toArcType(arc2D.getArcType()));
    }

    public static void fillQuadCurve(@NotNull GraphicsContext ctx, @NotNull QuadCurve2D quad) {
        ctx.beginPath();
        ctx.moveTo(quad.getX1(), quad.getY1());
        ctx.quadraticCurveTo(quad.getCtrlX(), quad.getCtrlY(), quad.getX2(), quad.getY2());
        ctx.fill();
    }

    public static void fillCubicCurve(@NotNull GraphicsContext ctx, @NotNull CubicCurve2D cubic) {
        ctx.beginPath();
        ctx.moveTo(cubic.getX1(), cubic.getY1());
        ctx.bezierCurveTo(cubic.getCtrlX1(), cubic.getCtrlY1(), cubic.getCtrlX2(), cubic.getCtrlY2(), cubic.getX2(),
                cubic.getY2());
        ctx.fill();
    }

    public static void fillShapeAsPath(@NotNull GraphicsContext ctx, @NotNull Shape shape) {
        FillRule prevFillRule = ctx.getFillRule();
        PathIterator awtIterator = shape.getPathIterator(null);
        appendPathIterator(ctx, awtIterator);
        applyWindingRule(ctx, awtIterator.getWindingRule());
        ctx.fill();
        ctx.setFillRule(prevFillRule);
    }

    public static void applyWindingRule(@NotNull GraphicsContext ctx, int awtWindingRule) {
        ctx.setFillRule(toFillRule(awtWindingRule));
    }

    public static void appendPathIterator(@NotNull GraphicsContext ctx, @NotNull PathIterator awtIterator) {
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
}
