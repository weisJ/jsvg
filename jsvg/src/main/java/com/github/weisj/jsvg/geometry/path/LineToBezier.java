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

import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.mesh.Bezier;

final class LineToBezier implements BezierPathCommand {

    private final boolean relative;
    private final float x;
    private final float y;

    public LineToBezier(boolean relative, float x, float y) {
        this.relative = relative;
        this.x = x;
        this.y = y;
    }

    @Override
    public @NotNull Bezier createBezier(@NotNull Point2D.Float start) {
        if (relative) {
            return Bezier.straightLine(start, new Point2D.Float(start.x + x, start.y + y));
        } else {
            return Bezier.straightLine(start, new Point2D.Float(x, y));
        }
    }
}
