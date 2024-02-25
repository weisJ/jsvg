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
package com.github.weisj.jsvg.geometry.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.FloatInsets;

public final class GeometryUtil {
    private static final float EPS = 0.0001f;

    private GeometryUtil() {}

    public static boolean approximatelyEqual(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    public static boolean approximatelyZero(double a) {
        return approximatelyEqual(a, 0);
    }

    public static boolean notablyGreater(double a, double b) {
        return a - b > EPS;
    }

    public static boolean approximatelyNegative(double a) {
        return a < EPS;
    }

    public static double scaleXOfTransform(@Nullable AffineTransform at) {
        if (at == null) return 1;
        double sx = at.getScaleX();
        double shy = at.getShearY();
        return Math.sqrt(sx * sx + shy * shy);
    }

    public static double scaleYOfTransform(@Nullable AffineTransform at) {
        if (at == null) return 1;
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

    public static @NotNull Rectangle2D containingBoundsAfterTransform(@NotNull AffineTransform transform,
            @NotNull Rectangle2D rect) {
        Point2D.Double p1 = new Point2D.Double(rect.getX(), rect.getY());
        Point2D.Double p2 = new Point2D.Double(rect.getX() + rect.getWidth(), rect.getY());
        Point2D.Double p3 = new Point2D.Double(rect.getX(), rect.getY() + rect.getHeight());
        Point2D.Double p4 = new Point2D.Double(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight());
        Rectangle2D r1 = rect.getBounds2D();
        r1.setFrameFromDiagonal(transform.transform(p1, p1), transform.transform(p2, p2));
        Rectangle2D r2 = rect.getBounds2D();
        r2.setFrameFromDiagonal(transform.transform(p3, p3), transform.transform(p4, p4));
        Rectangle2D.union(r1, r2, r1);
        return r1;
    }

    public static float left(@NotNull Rectangle2D rect) {
        return (float) rect.getX();
    }

    public static float top(@NotNull Rectangle2D rect) {
        return (float) rect.getY();
    }

    public static float right(@NotNull Rectangle2D rect) {
        return (float) (rect.getX() + rect.getWidth());
    }

    public static float bottom(@NotNull Rectangle2D rect) {
        return (float) (rect.getY() + rect.getHeight());
    }

    public static @NotNull Rectangle2D grow(@NotNull Rectangle2D bounds, FloatInsets grow) {
        return new Rectangle2D.Double(
                bounds.getX() - grow.left(),
                bounds.getY() - grow.top(),
                bounds.getWidth() + grow.left() + grow.right(),
                bounds.getHeight() + grow.top() + grow.bottom());
    }

    public static @NotNull Rectangle2D grow(@NotNull Rectangle2D bounds, double increase) {
        return new Rectangle2D.Double(
                bounds.getX() - increase,
                bounds.getY() - increase,
                bounds.getWidth() + 2 * increase,
                bounds.getHeight() + 2 * increase);
    }

    public static @NotNull FloatInsets max(@NotNull FloatInsets in1, @NotNull FloatInsets in2) {
        return new FloatInsets(
                Math.max(in1.top(), in2.top()),
                Math.max(in1.left(), in2.left()),
                Math.max(in1.bottom(), in2.bottom()),
                Math.max(in1.right(), in2.right()));
    }

    public static @NotNull FloatInsets min(@NotNull FloatInsets in1, @NotNull FloatInsets in2) {
        return new FloatInsets(
                Math.min(in1.top(), in2.top()),
                Math.min(in1.left(), in2.left()),
                Math.min(in1.bottom(), in2.bottom()),
                Math.min(in1.right(), in2.right()));
    }

    public static @NotNull FloatInsets overhangInsets(@NotNull Rectangle2D reference, @NotNull Rectangle2D bounds) {
        return new FloatInsets(
                Math.max(0, top(reference) - top(bounds)),
                Math.max(0, left(reference) - left(bounds)),
                Math.max(0, bottom(bounds) - bottom(reference)),
                Math.max(0, right(bounds) - right(reference)));
    }

    public static @NotNull String compactRepresentation(@NotNull Rectangle2D rect) {
        return "[" + rect.getX() + ", " + rect.getY() + ", " + rect.getWidth() + "x" + rect.getHeight() + "]";
    }

    public static @NotNull Rectangle2D toIntegerBounds(@NotNull Rectangle2D in, @NotNull Rectangle2D out) {
        double minY = Math.floor(in.getMinY());
        double minX = Math.floor(in.getMinX());
        double maxX = Math.ceil(in.getMaxX());
        double maxY = Math.ceil(in.getMaxY());
        out.setFrame(minX, minY, maxX - minX, maxY - minY);
        return out;
    }

    public static @NotNull Rectangle2D adjustForAliasing(@NotNull Rectangle2D r) {
        return toIntegerBounds(r, r);
    }
}
