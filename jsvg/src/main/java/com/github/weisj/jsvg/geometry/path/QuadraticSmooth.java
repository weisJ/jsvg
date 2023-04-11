/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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

final class QuadraticSmooth extends PathCommand {

    private final float x;
    private final float y;

    public QuadraticSmooth(boolean isRelative, float x, float y) {
        super(isRelative, 4);
        this.x = x;
        this.y = y;
    }

    @Override
    public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
        Point2D.Float offset = offset(hist);
        Point2D.Float knot = lastKnotReflection(hist);

        path.quadTo(knot.x, knot.y, x + offset.x, y + offset.y);
        hist.setLastPoint(path.getCurrentPoint());
        hist.setLastKnot(knot);
    }

    @Override
    public String toString() {
        return "T " + x + " " + y;
    }
}
