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
package com.github.weisj.jsvg.renderer.jfx.impl;

import java.awt.*;
import java.awt.Paint;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;

/**
 * The bridge between JavaFX and AWT.
 */
public class FXAWTBridge {

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

    public static StrokeLineCap toStrokeLineCap(int awtLineCap) {
        switch (awtLineCap) {
            case BasicStroke.CAP_BUTT:
                return StrokeLineCap.BUTT;
            case BasicStroke.CAP_ROUND:
                return StrokeLineCap.ROUND;
            case BasicStroke.CAP_SQUARE:
                return StrokeLineCap.SQUARE;
            default:
                throw new IllegalArgumentException("Unknown stroke line cap: " + awtLineCap);
        }
    }

    public static StrokeLineJoin toStrokeLineJoin(int awtLineJoin) {
        switch (awtLineJoin) {
            case BasicStroke.JOIN_BEVEL:
                return StrokeLineJoin.BEVEL;
            case BasicStroke.JOIN_MITER:
                return StrokeLineJoin.MITER;
            case BasicStroke.JOIN_ROUND:
                return StrokeLineJoin.ROUND;
            default:
                throw new IllegalArgumentException("Unknown stroke line join: " + awtLineJoin);
        }
    }

    public static CycleMethod toGradientCycleMethod(MultipleGradientPaint.CycleMethod awtCycleMethod) {
        switch (awtCycleMethod) {
            case NO_CYCLE:
                return CycleMethod.NO_CYCLE;
            case REFLECT:
                return CycleMethod.REFLECT;
            case REPEAT:
                return CycleMethod.REPEAT;
            default:
                throw new IllegalStateException("Unknown cycle method " + awtCycleMethod);
        }
    }

    public static BlendMode toBlendMode(com.github.weisj.jsvg.attributes.filter.BlendMode jsvgBlendMode) {
        switch (jsvgBlendMode) {
            case Normal:
                return BlendMode.SRC_OVER;
            case Multiply:
                return BlendMode.MULTIPLY;
            case Screen:
                return BlendMode.SCREEN;
            case Overlay:
                return BlendMode.OVERLAY;
            case Darken:
                return BlendMode.DARKEN;
            case Lighten:
                return BlendMode.LIGHTEN;
            case ColorDodge:
                return BlendMode.COLOR_DODGE;
            case ColorBurn:
                return BlendMode.COLOR_BURN;
            case HardLight:
                return BlendMode.HARD_LIGHT;
            case SoftLight:
                return BlendMode.SOFT_LIGHT;
            case Difference:
                return BlendMode.DIFFERENCE;
            case Exclusion:
                return BlendMode.EXCLUSION;
            case Hue:
            case Saturation:
            case Color:
            case Luminosity:
                // JavaFX doesn't support these modes.
                return BlendMode.SRC_OVER;
            default:
                return BlendMode.SRC_OVER;
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

    public static Affine convertAffineTransform(AffineTransform awtTransform) {
        return new Affine(awtTransform.getScaleX(), awtTransform.getShearY(), awtTransform.getShearX(),
                awtTransform.getScaleY(), awtTransform.getTranslateX(), awtTransform.getTranslateY());
    }

    public static AffineTransform convertAffine(Affine jfxAffine) {
        return new AffineTransform(jfxAffine.getMxx(), jfxAffine.getMyx(), jfxAffine.getMxy(), jfxAffine.getMyy(),
                jfxAffine.getTx(), jfxAffine.getTy());
    }

    public static boolean isWrappingPaint(@NotNull java.awt.Paint awtPaint) {
        return awtPaint instanceof GraphicsUtil.WrappingPaint;
    }

    public static boolean supportedPaint(@NotNull java.awt.Paint awtPaint) {
        if (awtPaint instanceof MultipleGradientPaint) {
            return isGradientOpaque((MultipleGradientPaint) awtPaint);
        }
        return awtPaint instanceof java.awt.Color
                || awtPaint instanceof TexturePaint
                || awtPaint instanceof RGBColor;
    }

    public static javafx.scene.paint.Paint convertPaint(@NotNull java.awt.Paint awtPaint) {
        if (awtPaint instanceof java.awt.Color) {
            return convertColor((java.awt.Color) awtPaint);
        } else if (awtPaint instanceof java.awt.LinearGradientPaint) {
            return convertLinearGradient((LinearGradientPaint) awtPaint);
        } else if (awtPaint instanceof java.awt.RadialGradientPaint) {
            return convertRadialGradient((RadialGradientPaint) awtPaint);
        } else if (awtPaint instanceof java.awt.TexturePaint) {
            return convertTexturePaint((TexturePaint) awtPaint);
        } else if (awtPaint instanceof RGBColor) {
            return convertRGBColor((RGBColor) awtPaint);
        } else {
            return Color.WHITE;
        }
    }

    public static java.awt.Color convertColor(@NotNull javafx.scene.paint.Color fxColor) {
        return new java.awt.Color(
                (int) (fxColor.getRed() * 255),
                (int) (fxColor.getGreen() * 255),
                (int) (fxColor.getBlue() * 255),
                (int) (fxColor.getOpacity() * 255));
    }

    public static javafx.scene.paint.Color convertColor(@NotNull java.awt.Color awtColor) {
        return javafx.scene.paint.Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                (double) awtColor.getAlpha() / 255.0);
    }

    public static javafx.scene.paint.Color convertRGBColor(@NotNull RGBColor rgbColor) {
        return convertColor(rgbColor.toColor());
    }

    public static LinearGradient convertLinearGradient(@NotNull LinearGradientPaint awtGradient) {
        AffineTransform transform = awtGradient.getTransform();
        Point2D start = awtGradient.getStartPoint();
        Point2D end = awtGradient.getEndPoint();

        start = transform.transform(start, start);
        end = transform.transform(end, end);

        java.awt.Color[] colors = awtGradient.getColors();
        float[] fractions = awtGradient.getFractions();
        MultipleGradientPaint.CycleMethod cycleMethod = awtGradient.getCycleMethod();

        return new LinearGradient(
                start.getX(),
                start.getY(),
                end.getX(),
                end.getY(),
                false,
                toGradientCycleMethod(cycleMethod),
                convertGradientStops(colors, fractions));
    }

    public static RadialGradient convertRadialGradient(@NotNull RadialGradientPaint awtGradient) {
        AffineTransform transform = awtGradient.getTransform();
        Point2D centerPt = awtGradient.getCenterPoint();
        Point2D focusPt = awtGradient.getFocusPoint();
        double radius = awtGradient.getRadius();
        double focusAngle = calculateGradientFocusAngle(centerPt, focusPt);
        double focusDistance = centerPt.distance(focusPt) / radius;

        centerPt = transform.transform(centerPt, centerPt);

        // Only uniform scaling is supported.
        radius *= transform.getScaleX();

        java.awt.Color[] colors = awtGradient.getColors();
        float[] fractions = awtGradient.getFractions();
        MultipleGradientPaint.CycleMethod cycleMethod = awtGradient.getCycleMethod();

        return new RadialGradient(
                focusAngle,
                focusDistance,
                //Place the focus at the center of a pixel (matches AWT more accurately)
                centerPt.getX() + 0.5D,
                centerPt.getY() + 0.5D,
                radius,
                false,
                toGradientCycleMethod(cycleMethod),
                convertGradientStops(colors, fractions));
    }

    private static double calculateGradientFocusAngle(Point2D center, Point2D focus) {
        if (center.equals(focus)) {
            return 0.0;
        }

        double dx = focus.getX() - center.getX();
        double dy = focus.getY() - center.getY();

        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private static java.util.List<Stop> convertGradientStops(java.awt.Color[] colors, float[] offsets) {
        List<Stop> stops = new ArrayList<>(colors.length);
        for (int i = 0; i < colors.length; i++) {
            javafx.scene.paint.Color fxColor = convertColor(colors[i]);
            stops.add(new Stop(offsets[i], fxColor));
        }
        return stops;
    }

    public static boolean isGradientOpaque(MultipleGradientPaint awtGradient) {
        for (java.awt.Color color : awtGradient.getColors()) {
            if (color.getAlpha() != 255) {
                return false;
            }
        }
        return true;
    }

    public static ImagePattern convertTexturePaint(@NotNull TexturePaint awtGradient) {
        Rectangle2D rect = awtGradient.getAnchorRect();
        return new ImagePattern(
                convertImage(awtGradient.getImage()),
                rect.getX(),
                rect.getY(),
                rect.getWidth(),
                rect.getHeight(),
                false);
    }

    public static WritableImage convertImage(@NotNull BufferedImage image) {
        return SwingFXUtils.toFXImage(image, null);
    }

    public static WritableImage convertImage(@NotNull Image image) {
        if (image instanceof BufferedImage) {
            return convertImage((BufferedImage) image);
        }
        BufferedImage dst = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        Graphics2D graphics = dst.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return convertImage(dst);
    }

    public static BufferedImage convertRasterToBufferedImage(@NotNull ColorModel colorModel, @NotNull Raster raster) {
        BufferedImage image = new BufferedImage(colorModel, raster.createCompatibleWritableRaster(),
                colorModel.isAlphaPremultiplied(), null);
        image.setData(raster);
        return image;
    }

    public static Color toPreMultipliedColor(Color c) {
        return Color.color(
                c.getRed() * c.getOpacity(),
                c.getGreen() * c.getOpacity(),
                c.getBlue() * c.getOpacity(),
                c.getOpacity());
    }

    public static double[] floatToDoubleArray(float[] floats) {
        if (floats == null) {
            return null;
        }
        double[] doubles = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }
        return doubles;
    }

    public static void drawImage(@NotNull GraphicsContext ctx, @NotNull BufferedImage awtImage) {
        ctx.drawImage(convertImage(awtImage), 0, 0);
    }

    public static void drawImage(@NotNull GraphicsContext ctx, @NotNull Image awtImage) {
        ctx.drawImage(convertImage(awtImage), 0, 0);
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

    public static void setOpacity(@NotNull GraphicsContext ctx, float opacity) {
        ctx.setGlobalAlpha(opacity);
    }

    public static void applyStroke(@NotNull GraphicsContext ctx, @NotNull Stroke awtStroke) {
        if (awtStroke instanceof BasicStroke) {
            BasicStroke awtBasicStroke = (BasicStroke) awtStroke;
            ctx.setLineWidth(awtBasicStroke.getLineWidth());
            ctx.setLineCap(toStrokeLineCap(awtBasicStroke.getEndCap()));
            ctx.setLineJoin(toStrokeLineJoin(awtBasicStroke.getLineJoin()));
            ctx.setMiterLimit(awtBasicStroke.getMiterLimit());
            ctx.setLineDashes(floatToDoubleArray(awtBasicStroke.getDashArray()));
            ctx.setLineDashOffset(awtBasicStroke.getDashPhase());
        }
    }

    public static void applyPaint(@NotNull GraphicsContext ctx, @NotNull Paint awtPaint) {
        javafx.scene.paint.Paint jfxPaint = convertPaint(awtPaint);
        ctx.setFill(jfxPaint);
        ctx.setStroke(jfxPaint);
    }

    public static void setTransform(@NotNull GraphicsContext ctx, @NotNull AffineTransform awtTransform) {
        ctx.setTransform(awtTransform.getScaleX(), awtTransform.getShearY(), awtTransform.getShearX(),
                awtTransform.getScaleY(), awtTransform.getTranslateX(), awtTransform.getTranslateY());
    }

    public static void applyTransform(@NotNull GraphicsContext ctx, @NotNull AffineTransform awtTransform) {
        ctx.transform(awtTransform.getScaleX(), awtTransform.getShearY(), awtTransform.getShearX(),
                awtTransform.getScaleY(), awtTransform.getTranslateX(), awtTransform.getTranslateY());
    }

}
