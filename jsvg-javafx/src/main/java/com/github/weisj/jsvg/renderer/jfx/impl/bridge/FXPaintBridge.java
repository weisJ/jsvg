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
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;

public final class FXPaintBridge {

    static final Logger LOGGER = Logger.getLogger(FXPaintBridge.class.getName());

    private FXPaintBridge() {}

    public static void applyPaint(@NotNull GraphicsContext ctx, @NotNull Paint awtPaint, double globalOpacity) {
        javafx.scene.paint.Paint jfxPaint = convertPaint(awtPaint, globalOpacity);
        ctx.setFill(jfxPaint);
        ctx.setStroke(jfxPaint);
    }

    public static boolean isWrappingPaint(@NotNull Paint awtPaint) {
        return awtPaint instanceof GraphicsUtil.WrappingPaint;
    }

    public static boolean supportedPaint(@NotNull Paint awtPaint) {
        if (awtPaint instanceof MultipleGradientPaint) {
            return isGradientOpaque((MultipleGradientPaint) awtPaint);
        }
        return awtPaint instanceof Color
                || awtPaint instanceof TexturePaint
                || awtPaint instanceof RGBColor;
    }

    public static javafx.scene.paint.Paint convertPaint(@NotNull Paint awtPaint, double globalOpacity) {
        if (awtPaint instanceof Color) {
            return convertColor((Color) awtPaint, globalOpacity);
        } else if (awtPaint instanceof LinearGradientPaint) {
            return convertLinearGradient((LinearGradientPaint) awtPaint, globalOpacity);
        } else if (awtPaint instanceof RadialGradientPaint) {
            return convertRadialGradient((RadialGradientPaint) awtPaint, globalOpacity);
        } else if (awtPaint instanceof TexturePaint) {
            return convertTexturePaint((TexturePaint) awtPaint, globalOpacity);
        } else if (awtPaint instanceof RGBColor) {
            return convertRGBColor((RGBColor) awtPaint, globalOpacity);
        } else {
            LOGGER.log(Level.WARNING, "Unsupported paint type, JavaFX doesn't support: " + awtPaint);
            return javafx.scene.paint.Color.WHITE;
        }
    }

    public static Color convertColor(@NotNull javafx.scene.paint.Color fxColor) {
        return new Color(
                (int) (fxColor.getRed() * 255),
                (int) (fxColor.getGreen() * 255),
                (int) (fxColor.getBlue() * 255),
                (int) (fxColor.getOpacity() * 255));
    }

    public static javafx.scene.paint.Color convertColor(@NotNull Color awtColor, double globalOpacity) {
        return javafx.scene.paint.Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                (awtColor.getAlpha() / 255.0) * globalOpacity);
    }

    public static javafx.scene.paint.Color convertRGBColor(@NotNull RGBColor rgbColor, double globalOpacity) {
        return convertColor(rgbColor.toColor(), globalOpacity);
    }

    public static LinearGradient convertLinearGradient(@NotNull LinearGradientPaint awtGradient,
            double globalOpacity) {
        AffineTransform transform = awtGradient.getTransform();
        Point2D start = awtGradient.getStartPoint();
        Point2D end = awtGradient.getEndPoint();

        start = transform.transform(start, start);
        end = transform.transform(end, end);

        Color[] colors = awtGradient.getColors();
        float[] fractions = awtGradient.getFractions();
        MultipleGradientPaint.CycleMethod cycleMethod = awtGradient.getCycleMethod();

        return new LinearGradient(
                start.getX(),
                start.getY(),
                end.getX(),
                end.getY(),
                false,
                toGradientCycleMethod(cycleMethod),
                convertGradientStops(colors, fractions, globalOpacity));
    }

    public static RadialGradient convertRadialGradient(@NotNull RadialGradientPaint awtGradient,
            double globalOpacity) {
        AffineTransform transform = awtGradient.getTransform();
        Point2D centerPt = awtGradient.getCenterPoint();
        Point2D focusPt = awtGradient.getFocusPoint();
        double radius = awtGradient.getRadius();
        double focusAngle = calculateGradientFocusAngle(centerPt, focusPt);
        double focusDistance = centerPt.distance(focusPt) / radius;

        centerPt = transform.transform(centerPt, centerPt);

        // Only uniform scaling is supported.
        radius *= transform.getScaleX();

        Color[] colors = awtGradient.getColors();
        float[] fractions = awtGradient.getFractions();
        MultipleGradientPaint.CycleMethod cycleMethod = awtGradient.getCycleMethod();

        return new RadialGradient(
                focusAngle,
                focusDistance,
                // Place the focus at the center of a pixel (matches AWT more accurately)
                centerPt.getX() + 0.5D,
                centerPt.getY() + 0.5D,
                radius,
                false,
                toGradientCycleMethod(cycleMethod),
                convertGradientStops(colors, fractions, globalOpacity));
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

    private static double calculateGradientFocusAngle(Point2D center, Point2D focus) {
        if (center.equals(focus)) {
            return 0.0;
        }

        double dx = focus.getX() - center.getX();
        double dy = focus.getY() - center.getY();

        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private static java.util.List<Stop> convertGradientStops(Color[] colors, float[] offsets,
            double globalOpacity) {
        List<Stop> stops = new ArrayList<>(colors.length);
        for (int i = 0; i < colors.length; i++) {
            javafx.scene.paint.Color fxColor = convertColor(colors[i], globalOpacity);
            stops.add(new Stop(offsets[i], fxColor));
        }
        return stops;
    }

    public static boolean isGradientOpaque(MultipleGradientPaint awtGradient) {
        for (Color color : awtGradient.getColors()) {
            if (color.getAlpha() != 255) {
                return false;
            }
        }
        return true;
    }

    public static ImagePattern convertTexturePaint(@NotNull TexturePaint awtGradient, double globalOpacity) {
        Rectangle2D rect = awtGradient.getAnchorRect();
        return new ImagePattern(
                FXImageBridge.convertImageWithOpacity(awtGradient.getImage(), globalOpacity),
                rect.getX(),
                rect.getY(),
                rect.getWidth(),
                rect.getHeight(),
                false);
    }

    public static javafx.scene.paint.Color toPreMultipliedColor(javafx.scene.paint.Color c) {
        return javafx.scene.paint.Color.color(
                c.getRed() * c.getOpacity(),
                c.getGreen() * c.getOpacity(),
                c.getBlue() * c.getOpacity(),
                c.getOpacity());
    }

    public static javafx.scene.paint.Color toUnmultipliedColor(javafx.scene.paint.Color c) {
        if (c.getOpacity() == 0) {
            return javafx.scene.paint.Color.color(0, 0, 0, 0);
        }
        return javafx.scene.paint.Color.color(
                c.getRed() / c.getOpacity(),
                c.getGreen() / c.getOpacity(),
                c.getBlue() / c.getOpacity(),
                c.getOpacity());
    }
}
