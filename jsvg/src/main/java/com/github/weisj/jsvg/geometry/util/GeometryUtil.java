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
package com.github.weisj.jsvg.geometry.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;

public final class GeometryUtil {
    public static final float EPS = 0.0001f;

    private GeometryUtil() {}

    public static double scaleXOfTransform(@NotNull AffineTransform at) {
        double sx = at.getScaleX();
        double shy = at.getShearY();
        return Math.sqrt(sx * sx + shy * shy);
    }

    public static double scaleYOfTransform(@NotNull AffineTransform at) {
        double sy = at.getScaleY();
        double shx = at.getShearX();
        return Math.sqrt(sy * sy + shx * shx);
    }

    public static @NotNull Point2D.Float midPoint(@NotNull Point2D.Float x, @NotNull Point2D.Float y) {
        return new Point2D.Float((x.x + y.x) / 2, (x.y + y.y) / 2);
    }

    public static @NotNull Point2D.Float lerp(float t, @NotNull Point2D.Float a, @NotNull Point2D.Float b) {
        return new Point2D.Float(lerp(t, b.x, a.x), lerp(t, b.y, a.y));
    }

    public static float lerp(float t, float a, float b) {
        return (1 - t) * a + t * b;
    }

    public static double distanceSquared(@NotNull Point2D.Float p1, @NotNull Point2D.Float p2, float scaleX,
            float scaleY) {
        return distanceSquared(scaleX * p1.x, scaleY * p1.y, scaleX * p2.x, scaleY * p2.y);
    }

    public static double distanceSquared(@NotNull Point2D.Float p1, @NotNull Point2D.Float p2) {
        return distanceSquared(p1.x, p1.y, p2.x, p2.y);
    }

    public static double distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static double pathLength(@NotNull Shape shape) {
        PathLengthCalculator pathLengthCalculator = new PathLengthCalculator();
        PathIterator pathIterator = shape.getPathIterator(null);
        double length = 0;
        double[] args = new double[6];
        while (!pathIterator.isDone()) {
            length += pathLengthCalculator.segmentLength(pathIterator.currentSegment(args), args);
            pathIterator.next();
        }
        return length;
    }

    public static double lineLength(double x1, double y1, double x2, double y2) {
        return Math.sqrt(GeometryUtil.distanceSquared(x1, y1, x2, y2));
    }
}
