/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
package com.github.weisj.jsvg.geometry.mesh;

import static com.github.weisj.jsvg.geometry.mesh.MeshUtil.p;
import static com.github.weisj.jsvg.geometry.util.GeometryUtil.distanceSquared;
import static com.github.weisj.jsvg.geometry.util.GeometryUtil.midPoint;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;

public class Bezier {
    public @NotNull Point2D.Float a;
    public @NotNull Point2D.Float b;
    public @NotNull Point2D.Float c;
    public @NotNull Point2D.Float d;

    public Bezier(@NotNull Point2D.Float a, @NotNull Point2D.Float b, @NotNull Point2D.Float c,
            @NotNull Point2D.Float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public void appendTo(@NotNull GeneralPath p) {
        p.curveTo(b.x, b.y, c.x, c.y, d.x, d.y);
    }

    public @NotNull Bezier inverse() {
        return new Bezier(d, c, b, a);
    }

    public @NotNull Split<Bezier> split() {
        Point2D.Float ab = midPoint(a, b);
        Point2D.Float bc = midPoint(b, c);
        Point2D.Float cd = midPoint(c, d);
        Point2D.Float abbc = midPoint(ab, bc);
        Point2D.Float bccd = midPoint(bc, cd);
        Point2D.Float abbcbccd = midPoint(abbc, bccd);
        return new Split<>(
                new Bezier(a, ab, abbc, abbcbccd),
                new Bezier(abbcbccd, bccd, cd, d));
    }

    public static @NotNull Bezier straightLine(Point2D.Float a, Point2D.Float b) {
        return new LineBezier(a, b);
    }

    public static @NotNull Bezier combine(@NotNull Bezier b1, @NotNull Bezier b2, @NotNull Bezier b3) {
        return new Bezier(
                p(b1.a.x + b2.a.x - b3.a.x, b1.a.y + b2.a.y - b3.a.y),
                p(b1.b.x + b2.b.x - b3.b.x, b1.b.y + b2.b.y - b3.b.y),
                p(b1.c.x + b2.c.x - b3.c.x, b1.c.y + b2.c.y - b3.c.y),
                p(b1.d.x + b2.d.x - b3.d.x, b1.d.y + b2.d.y - b3.d.y));
    }

    public int estimateStepCount(float scaleX, float scaleY) {
        double steps = Math.max(
                Math.max(distanceSquared(a, b, scaleX, scaleY), distanceSquared(c, d, scaleX, scaleY)),
                Math.max(distanceSquared(a, c, scaleX, scaleY) / 4, distanceSquared(b, d, scaleX, scaleY) / 4));
        steps *= 18;
        steps = Math.max(1, steps);
        return (Math.getExponent(steps) + 1) / 2;
    }


    @Override
    public String toString() {
        return "Bezier{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                '}';
    }
}
