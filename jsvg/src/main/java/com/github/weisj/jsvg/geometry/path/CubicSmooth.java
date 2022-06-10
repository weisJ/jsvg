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
package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
class CubicSmooth extends PathCommand {

    private final float x;
    private final float y;
    private final float k2x;
    private final float k2y;

    public CubicSmooth(boolean isRelative, float k2x, float k2y, float x, float y) {
        super(isRelative, 6);
        this.k2x = k2x;
        this.k2y = k2y;
        this.x = x;
        this.y = y;
    }

    @Override
    public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
        Point2D.Float offset = offset(hist);
        Point2D.Float knot = lastKnotReflection(hist);

        path.curveTo(knot.x, knot.y, k2x + offset.x, k2y + offset.y, x + offset.x, y + offset.y);
        hist.setLastPoint(path.getCurrentPoint());
        hist.setLastKnot(k2x + offset.x, k2y + offset.y);
    }

    @Override
    public String toString() {
        return "S " + k2x + " " + k2y
                + " " + x + " " + y;
    }
}
