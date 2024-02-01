/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.util;

import java.awt.*;
import java.awt.geom.*;

import org.jetbrains.annotations.NotNull;

public final class ShapeUtil {

    private static final int NON_RECTILINEAR_TRANSFORM_MASK =
            AffineTransform.TYPE_GENERAL_TRANSFORM | AffineTransform.TYPE_GENERAL_ROTATION;

    private ShapeUtil() {}

    /*
     * Intersect two Shapes by the simplest method, attempting to produce a simplified result. The
     * boolean arguments keep1 and keep2 specify whether or not the first or second shapes can be
     * modified during the operation or whether that shape must be "kept" unmodified.
     */
    @NotNull
    public static Shape intersect(@NotNull Shape s1, @NotNull Shape s2, boolean keep1, boolean keep2) {
        if (s1 instanceof Rectangle && s2 instanceof Rectangle) {
            return ((Rectangle) s1).intersection((Rectangle) s2);
        }
        if (s1 instanceof Rectangle2D) {
            return intersectRectShape((Rectangle2D) s1, s2, keep1, keep2);
        } else if (s2 instanceof Rectangle2D) {
            return intersectRectShape((Rectangle2D) s2, s1, keep2, keep1);
        }
        return intersectByArea(s1, s2, keep1, keep2);
    }

    /*
     * Intersect a Rectangle with a Shape by the simplest method, attempting to produce a simplified
     * result. The boolean arguments keep1 and keep2 specify whether or not the first or second shapes
     * can be modified during the operation or whether that shape must be "kept" unmodified.
     */
    @NotNull
    private static Shape intersectRectShape(@NotNull Rectangle2D r, @NotNull Shape s,
            boolean keep1, boolean keep2) {
        if (s instanceof Rectangle2D) {
            Rectangle2D r2 = (Rectangle2D) s;
            Rectangle2D outputRect;
            if (!keep1) {
                outputRect = r;
            } else if (!keep2) {
                outputRect = r2;
            } else {
                outputRect = new Rectangle2D.Float();
            }
            double x1 = Math.max(r.getX(), r2.getX());
            double x2 = Math.min(r.getX() + r.getWidth(), r2.getX() + r2.getWidth());
            double y1 = Math.max(r.getY(), r2.getY());
            double y2 = Math.min(r.getY() + r.getHeight(), r2.getY() + r2.getHeight());

            if (((x2 - x1) < 0) || ((y2 - y1) < 0))
                // Width or height is negative. No intersection.
                outputRect.setFrameFromDiagonal(0, 0, 0, 0);
            else
                outputRect.setFrameFromDiagonal(x1, y1, x2, y2);
            return outputRect;
        }
        if (r.contains(s.getBounds2D())) {
            if (keep2) {
                s = cloneShape(s);
            }
            return s;
        }
        return intersectByArea(r, s, keep1, keep2);
    }

    /*
     * Intersect two Shapes using the Area class. Presumably other attempts at simpler intersection
     * methods proved fruitless. The boolean arguments keep1 and keep2 specify whether or not the first
     * or second shapes can be modified during the operation or whether that shape must be "kept"
     * unmodified.
     */
    @NotNull
    private static Shape intersectByArea(@NotNull Shape s1, @NotNull Shape s2, boolean keep1, boolean keep2) {
        Area a1, a2;

        // First see if we can find an overwrite-able source shape
        // to use as our destination area to avoid duplication.
        if (!keep1 && (s1 instanceof Area)) {
            a1 = (Area) s1;
        } else if (!keep2 && (s2 instanceof Area)) {
            a1 = (Area) s2;
            s2 = s1;
        } else {
            a1 = new Area(s1);
        }

        if (s2 instanceof Area) {
            a2 = (Area) s2;
        } else {
            a2 = new Area(s2);
        }

        a1.intersect(a2);
        if (a1.isRectangular()) {
            return a1.getBounds();
        }

        return a1;
    }

    public static @NotNull Shape transformShape(@NotNull Shape s, @NotNull AffineTransform transform) {
        if (transform.getType() > AffineTransform.TYPE_TRANSLATION) {
            return transformShape(transform, s);
        } else {
            return transformShape(transform.getTranslateX(), transform.getTranslateY(), s);
        }
    }

    private static Shape transformShape(@NotNull AffineTransform tx, @NotNull Shape shape) {
        if (shape instanceof Rectangle2D &&
                (tx.getType() & NON_RECTILINEAR_TRANSFORM_MASK) == 0) {
            Rectangle2D rect = (Rectangle2D) shape;
            double[] matrix = new double[4];
            matrix[0] = rect.getX();
            matrix[1] = rect.getY();
            matrix[2] = matrix[0] + rect.getWidth();
            matrix[3] = matrix[1] + rect.getHeight();
            tx.transform(matrix, 0, matrix, 0, 2);
            fixRectangleOrientation(matrix, rect);
            return new Rectangle2D.Double(matrix[0], matrix[1],
                    matrix[2] - matrix[0],
                    matrix[3] - matrix[1]);
        }

        if (tx.isIdentity()) {
            return cloneShape(shape);
        }

        return tx.createTransformedShape(shape);
    }

    private static void fixRectangleOrientation(double[] m, @NotNull Rectangle2D r) {
        if (r.getWidth() > 0 != (m[2] - m[0] > 0)) {
            double t = m[0];
            m[0] = m[2];
            m[2] = t;
        }
        if (r.getHeight() > 0 != (m[3] - m[1] > 0)) {
            double t = m[1];
            m[1] = m[3];
            m[3] = t;
        }
    }

    private static @NotNull Shape transformShape(double tx, double ty, @NotNull Shape s) {
        if (s instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) s;
            return new Rectangle2D.Double(
                    rect.getX() + tx,
                    rect.getY() + ty,
                    rect.getWidth(),
                    rect.getHeight());
        }

        if (tx == 0 && ty == 0) {
            return ShapeUtil.cloneShape(s);
        }

        AffineTransform mat = AffineTransform.getTranslateInstance(tx, ty);
        return mat.createTransformedShape(s);
    }

    private static Shape cloneShape(Shape s) {
        return new GeneralPath(s);
    }
}
