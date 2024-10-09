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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;

/**
 * This is a little used SVG function, as most editors will save curves as
 * Béziers.  To reduce the need to rely on the Batik library, this functionality
 * is being bypassed for the time being.  In the future, it would be nice to
 * extend the GeneralPath command to include the arcTo ability provided by Batik.
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 * @author Jannis Weis
 */
final class Arc extends PathCommand {

    private final float rx;
    private final float ry;
    private final float xAxisRot;
    private final boolean largeArc;
    private final boolean sweep;
    private final float x;
    private final float y;

    public Arc(boolean isRelative,
            float rx, float ry,
            float xAxisRot,
            boolean largeArc, boolean sweep,
            float x, float y) {
        super(isRelative, 6);
        this.rx = rx;
        this.ry = ry;
        this.xAxisRot = xAxisRot;
        this.largeArc = largeArc;
        this.sweep = sweep;
        this.x = x;
        this.y = y;
    }

    @Override
    public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
        Point2D.Float offset = offset(hist);

        arcTo(path, rx, ry, xAxisRot, largeArc, sweep,
                x + offset.x, y + offset.y,
                hist.lastPoint.x, hist.lastPoint.y);
        hist.setLast(path.getCurrentPoint());
    }

    /**
     * Adds an elliptical arc, defined by two radii, an angle from the
     * x-axis, a flag to choose the large arc or not, a flag to
     * indicate if we increase or decrease the angles and the final
     * point of the arc.
     *
     * @param path The path that the arc will be appended to.
     *
     * @param rx the x radius of the ellipse
     * @param ry the y radius of the ellipse
     *
     * @param angle the angle from the x-axis of the current
     * coordinate system to the x-axis of the ellipse in degrees.
     *
     * @param largeArcFlag the large arc flag. If true the arc
     * spanning less than or equal to 180 degrees is chosen, otherwise
     * the arc spanning greater than 180 degrees is chosen
     *
     * @param sweepFlag the sweep flag. If true the line joining
     * center to arc sweeps through decreasing angles otherwise it
     * sweeps through increasing angles
     *
     * @param x the absolute x coordinate of the final point of the arc.
     * @param y the absolute y coordinate of the final point of the arc.
     * @param x0 - The absolute x coordinate of the initial point of the arc.
     * @param y0 - The absolute y coordinate of the initial point of the arc.
     */
    private void arcTo(@NotNull Path2D path, float rx, float ry,
            float angle,
            boolean largeArcFlag,
            boolean sweepFlag,
            float x, float y, float x0, float y0) {

        // Ensure radii are valid
        if (rx == 0 || ry == 0) {
            path.lineTo(x, y);
            return;
        }

        if (x0 == x && y0 == y) {
            // If the endpoints (x, y) and (x0, y0) are identical, then this
            // is equivalent to omitting the elliptical arc segment entirely.
            return;
        }

        Arc2D arc = computeRawArc(x0, y0, rx, ry, angle, largeArcFlag, sweepFlag, x, y);

        AffineTransform t = AffineTransform.getRotateInstance(
                Math.toRadians(angle), arc.getCenterX(), arc.getCenterY());
        Shape s = t.createTransformedShape(arc);
        path.append(s, true);
    }


    /**
      * This constructs a non-rotated Arc2D from the SVG specification of an
     * Elliptical arc.  To get the final arc you need to apply a rotation
     * transform such as:
     * <p>
     * AffineTransform.getRotateInstance
     *     (angle, arc.getX()+arc.getWidth()/2, arc.getY()+arc.getHeight()/2);
     *
     * @param x0 origin of arc in x
     * @param y0 origin of arc in y
     * @param rx radius of arc in x
     * @param ry radius of arc in y
     * @param angle number of radians in arc
     * @param largeArcFlag the large arc flag
     * @param sweepFlag the sweep flag
     * @param x ending coordinate of arc in x
     * @param y ending coordinate of arc in y
     * @return arc shape
     *
     */
    private static @NotNull Arc2D computeRawArc(
            double x0, double y0,
            double rx, double ry,
            double angle,
            boolean largeArcFlag,
            boolean sweepFlag,
            double x, double y) {
        //
        // Elliptical arc implementation based on the SVG specification notes
        //

        // Compute the half distance between the current and the final point
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        // Convert angle from degrees to radians
        angle = Math.toRadians(angle % 360.0);
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        //
        // Step 1 : Compute (x1, y1)
        //
        double x1 = cosAngle * dx2 + sinAngle * dy2;
        double y1 = -sinAngle * dx2 + cosAngle * dy2;
        // Ensure radii are large enough
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double Prx = rx * rx;
        double Pry = ry * ry;
        double Px1 = x1 * x1;
        double Py1 = y1 * y1;
        // check that radii are large enough
        double radiiCheck = Px1 / Prx + Py1 / Pry;
        if (radiiCheck > 1) {
            rx = Math.sqrt(radiiCheck) * rx;
            ry = Math.sqrt(radiiCheck) * ry;
            Prx = rx * rx;
            Pry = ry * ry;
        }

        //
        // Step 2 : Compute (cx1, cy1)
        //
        double sign = largeArcFlag == sweepFlag ? -1 : 1;
        double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1)) / ((Prx * Py1) + (Pry * Px1));
        sq = sq < 0 ? 0 : sq;
        double coefficient = sign * Math.sqrt(sq);
        double cx1 = coefficient * ((rx * y1) / ry);
        double cy1 = coefficient * -((ry * x1) / rx);

        //
        // Step 3 : Compute (cx, cy) from (cx1, cy1)
        //
        double sx2 = (x0 + x) / 2.0;
        double sy2 = (y0 + y) / 2.0;
        double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
        double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

        //
        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
        //
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double p, n;
        // Compute the angle start
        n = Math.sqrt((ux * ux) + (uy * uy));
        p = ux; // (1 * ux) + (0 * uy)
        sign = (uy < 0) ? -1d : 1d;
        double angleStart = Math.toDegrees(sign * Math.acos(p / n));

        // Compute the angle extent
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        sign = ux * vy - uy * vx < 0 ? -1d : 1d;
        double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
        if (!sweepFlag && angleExtent > 0) {
            angleExtent -= 360f;
        } else if (sweepFlag && angleExtent < 0) {
            angleExtent += 360f;
        }
        angleExtent %= 360f;
        angleStart %= 360f;

        //
        // We can now build the resulting Arc2D in double precision
        //
        Arc2D.Double arc = new Arc2D.Double();
        arc.x = cx - rx;
        arc.y = cy - ry;
        arc.width = rx * 2.0;
        arc.height = ry * 2.0;
        arc.start = -angleStart;
        arc.extent = -angleExtent;

        return arc;
    }

    @Override
    public String toString() {
        return "A " + rx + " " + ry
                + " " + xAxisRot + " " + largeArc
                + " " + sweep
                + " " + x + " " + y;
    }
}
