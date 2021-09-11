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
package com.github.weisj.jsvg.nodes.mesh;

import static com.github.weisj.jsvg.nodes.mesh.MeshUtil.midPoint;
import static com.github.weisj.jsvg.nodes.mesh.MeshUtil.p;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

class CoonPatch {
    public Bezier north;
    public Bezier east;
    public Bezier south;
    public Bezier west;

    public final CoonValues coonValues;

    CoonPatch(Bezier north, Bezier east, Bezier south, Bezier west) {
        this(north, east, south, west,
                new CoonValues(p(0, 0), p(1, 0), p(1, 1), p(0, 1)));
    }

    CoonPatch(Bezier north, Bezier east, Bezier south, Bezier west, CoonValues coonValues) {
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
        this.coonValues = coonValues;
    }

    public Shape toShape() {
        GeneralPath p = new GeneralPath(Path2D.WIND_EVEN_ODD);
        p.moveTo(north.a.x, north.a.y);
        appendBezier(p, north);
        appendBezier(p, east);
        appendBezier(p, south);
        appendBezier(p, west);
        p.closePath();
        return p;
    }

    private void appendBezier(GeneralPath p, Bezier bezier) {
        p.curveTo(bezier.b.x, bezier.b.y, bezier.c.x, bezier.c.y, bezier.d.x, bezier.d.y);
    }

    public Subdivided<CoonPatch> subdivide() {
        Point2D.Float midNorthLinear = midPoint(north.a, north.d);
        Point2D.Float midSouthLinear = midPoint(south.d, south.a);
        Point2D.Float midWestLinear = midPoint(north.a, south.d);
        Point2D.Float midEastLinear = midPoint(north.d, south.a);

        Split<Bezier> northSplit = north.split();
        Split<Bezier> southSplit = south.split();
        Split<Bezier> westSplit = west.split();
        Split<Bezier> eastSplit = east.split();

        Bezier northLeft = northSplit.left;
        Bezier northRight = northSplit.right;

        Bezier southLeft = southSplit.right;
        Bezier southRight = southSplit.left;

        @SuppressWarnings({"SuspiciousNameCombination"}) Bezier westBottom = westSplit.left;
        @SuppressWarnings({"SuspiciousNameCombination"}) Bezier westTop = westSplit.right;

        @SuppressWarnings({"SuspiciousNameCombination"}) Bezier eastTop = eastSplit.left;
        @SuppressWarnings({"SuspiciousNameCombination"}) Bezier eastBottom = eastSplit.right;

        Bezier midNorthSouth = new Bezier(
                midPoint(north.a, south.d),
                midPoint(north.b, south.c),
                midPoint(north.c, south.b),
                midPoint(north.d, south.a));
        Bezier midEastWest = new Bezier(
                midPoint(east.a, west.d),
                midPoint(east.b, west.c),
                midPoint(east.c, west.b),
                midPoint(east.d, west.a));

        Split<Bezier> splitNorthSouth = Bezier.combine(
                midEastWest,
                Bezier.straightLine(northLeft.d, southLeft.a),
                Bezier.straightLine(midNorthLinear, midSouthLinear)).split();

        Split<Bezier> splitWestEast = Bezier.combine(
                midNorthSouth,
                Bezier.straightLine(westTop.a, eastTop.d),
                Bezier.straightLine(midWestLinear, midEastLinear)).split();

        Subdivided<CoonValues> weights = subdivideWeights();
        CoonPatch northWest = new CoonPatch(
                northLeft, splitNorthSouth.left, splitWestEast.left.inverse(), westTop,
                weights.northWest);
        CoonPatch northEast = new CoonPatch(
                northRight, eastTop, splitWestEast.right.inverse(), splitNorthSouth.left.inverse(),
                weights.northEast);
        CoonPatch southWest = new CoonPatch(
                splitWestEast.left, splitNorthSouth.right, southLeft, westBottom,
                weights.southWest);
        CoonPatch southEast = new CoonPatch(
                splitWestEast.right, eastBottom, southRight, splitNorthSouth.right.inverse(),
                weights.southEast);
        return new Subdivided<>(northWest, northEast, southWest, southEast);
    }

    private Subdivided<CoonValues> subdivideWeights() {
        Point2D.Float midNorthValue = midPoint(coonValues.north, coonValues.east);
        Point2D.Float midWestValue = midPoint(coonValues.north, coonValues.west);
        Point2D.Float midSouthValue = midPoint(coonValues.west, coonValues.south);
        Point2D.Float midEastValue = midPoint(coonValues.east, coonValues.south);

        Point2D.Float gridMidValue = midPoint(midSouthValue, midNorthValue);
        CoonValues northWest = new CoonValues(coonValues.north, midNorthValue, gridMidValue, midWestValue);
        CoonValues northEast = new CoonValues(midNorthValue, coonValues.east, midEastValue, gridMidValue);
        CoonValues southWest = new CoonValues(midWestValue, gridMidValue, midSouthValue, coonValues.west);
        CoonValues southEast = new CoonValues(gridMidValue, midEastValue, coonValues.south, midSouthValue);
        return new Subdivided<>(northWest, northEast, southWest, southEast);
    }

    static Point2D.Float lerp(float t, Point2D.Float a, Point2D.Float b) {
        return new Point2D.Float(lerp(t, b.x, a.x), lerp(t, b.y, a.y));
    }

    static float lerp(float t, float a, float b) {
        return (1 - t) * a + t * b;
    }

    @Override
    public String toString() {
        return "CoonPatch{" +
                "north=" + north +
                ", east=" + east +
                ", south=" + south +
                ", west=" + west +
                ", coonValues=" + coonValues +
                '}';
    }
}
