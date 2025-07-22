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
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FXStrokeBridge {

    public static void applyStroke(@NotNull GraphicsContext ctx, @NotNull Stroke awtStroke) {
        if (awtStroke instanceof BasicStroke) {
            BasicStroke awtBasicStroke = (BasicStroke) awtStroke;
            ctx.setLineWidth(awtBasicStroke.getLineWidth());
            ctx.setLineCap(toStrokeLineCap(awtBasicStroke.getEndCap()));
            ctx.setLineJoin(toStrokeLineJoin(awtBasicStroke.getLineJoin()));
            ctx.setMiterLimit(awtBasicStroke.getMiterLimit());
            ctx.setLineDashes(convertDashArray(awtBasicStroke.getDashArray()));
            ctx.setLineDashOffset(awtBasicStroke.getDashPhase());
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

    public static double @Nullable [] convertDashArray(float @Nullable [] dashes) {
        if (dashes == null) {
            return null;
        }
        double[] doubles = new double[dashes.length];
        for (int i = 0; i < dashes.length; i++) {
            doubles[i] = dashes[i];
        }
        return doubles;
    }
}
