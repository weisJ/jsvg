/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

final class MultiConvolveOp implements BufferedImageOp, RasterOp {

    private final @NotNull ConvolveOp[] ops;
    private final @NotNull ConvolveOp op;

    public MultiConvolveOp(@NotNull ConvolveOp[] ops) {
        if (ops.length == 0) throw new IllegalStateException("Must supply at least one op");
        this.ops = ops;
        this.op = ops[0];
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage result1 = op.filter(src, dest);
        if (ops.length == 1) return result1;
        BufferedImage result2 = ops[1].filter(result1, null);
        BufferedImage r = result2;
        for (int i = 2; i < ops.length; i++) {
            result1 = ops[1].filter(result2, result1);
            r = result1;
            result1 = result2;
            result2 = r;
        }
        return r;
    }


    @Override
    public WritableRaster filter(Raster src, WritableRaster dest) {
        WritableRaster result1 = op.filter(src, dest);
        if (ops.length == 1) return result1;
        WritableRaster result2 = ops[1].filter(result1, null);
        WritableRaster r = result2;
        for (int i = 2; i < ops.length; i++) {
            result1 = ops[1].filter(result2, result1);
            r = result1;
            result1 = result2;
            result2 = r;
        }
        return r;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return op.createCompatibleDestImage(src, destCM);
    }

    @Override
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return op.createCompatibleDestRaster(src);
    }

    @Override
    public Rectangle2D getBounds2D(@NotNull BufferedImage src) {
        return op.getBounds2D(src);
    }

    @Override
    public Rectangle2D getBounds2D(@NotNull Raster src) {
        return op.getBounds2D(src);
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return op.getPoint2D(srcPt, dstPt);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return op.getRenderingHints();
    }
}
