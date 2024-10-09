/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;


public abstract class PathCommand {

    private final boolean isRelative;
    private final int nodeCount;

    protected PathCommand(int nodeCount) {
        this(false, nodeCount);
    }

    protected PathCommand(boolean isRelative, int nodeCount) {
        this.isRelative = isRelative;
        this.nodeCount = nodeCount;
    }

    protected Point2D.Float offset(@NotNull BuildHistory hist) {
        if (isRelative()) {
            return new Point2D.Float(hist.lastPoint.x, hist.lastPoint.y);
        } else {
            return new Point2D.Float(0, 0);
        }
    }

    protected @NotNull Point2D.Float lastKnotReflectionCubic(@NotNull BuildHistory hist) {
        return lastKnotReflection(hist.lastPoint, hist.lastCubicKnot);
    }

    protected @NotNull Point2D.Float lastKnotReflectionQuadratic(@NotNull BuildHistory hist) {
        return lastKnotReflection(hist.lastPoint, hist.lastQuadraticKnot);
    }

    private @NotNull Point2D.Float lastKnotReflection(@NotNull Point2D.Float point, @NotNull Point2D.Float knot) {
        // Calc knot as reflection of old knot
        float kx = point.x * 2f - knot.x;
        float ky = point.y * 2f - knot.y;
        return new Point2D.Float(kx, ky);
    }

    public boolean isRelative() {
        return isRelative;
    }

    public abstract void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist);

    public int nodeCount() {
        return nodeCount;
    }
}
