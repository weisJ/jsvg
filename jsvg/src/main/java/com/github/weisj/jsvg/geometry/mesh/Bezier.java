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
import static com.github.weisj.jsvg.geometry.util.GeometryUtil.lerp;
import static com.github.weisj.jsvg.geometry.util.GeometryUtil.midPoint;

import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.SVGLine;

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

    public Bezier inverse() {
        return new Bezier(d, c, b, a);
    }

    public Split<Bezier> split() {
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

    public static Bezier straightLine(Point2D.Float a, Point2D.Float b) {
        return new Bezier(a, lerp(1 / 3f, b, a), lerp(2 / 3f, b, a), b);
    }

    public static Bezier combine(Bezier b1, Bezier b2, Bezier b3) {
        return new Bezier(
                p(b1.a.x + b2.a.x - b3.a.x, b1.a.y + b2.a.y - b3.a.y),
                p(b1.b.x + b2.b.x - b3.b.x, b1.b.y + b2.b.y - b3.b.y),
                p(b1.c.x + b2.c.x - b3.c.x, b1.c.y + b2.c.y - b3.c.y),
                p(b1.d.x + b2.d.x - b3.d.x, b1.d.y + b2.d.y - b3.d.y));
    }

    public int estimateStepCount(float scaleX, float scaleY) {
        float steps = Math.max(
                Math.max(distanceSquared(a, b, scaleX, scaleY), distanceSquared(c, d, scaleX, scaleY)),
                Math.max(distanceSquared(a, c, scaleX, scaleY) / 4, distanceSquared(b, d, scaleX, scaleY) / 4));
        steps *= 18;
        steps = Math.max(1, steps);
        return (Math.getExponent(steps) + 1) / 2;
    }

    private static float distanceSquared(@NotNull Point2D.Float p1, @NotNull Point2D.Float p2, float scaleX,
            float scaleY) {
        return (float) SVGLine.lineLength(scaleX * p1.x, scaleY * p1.y, scaleX * p2.x, scaleY * p2.y);
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
