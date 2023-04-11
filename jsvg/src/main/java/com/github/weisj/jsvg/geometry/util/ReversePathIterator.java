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
package com.github.weisj.jsvg.geometry.util;

import java.awt.geom.IllegalPathStateException;
import java.awt.geom.PathIterator;

/**
 *  A path iterator which iterates over a path in the reverse direction.
 *  This is missing in the java.awt.geom package, although it's quite simple to implement.
 *  After initialization the original PathIterator is not used any longer.
 *  <p>
 *
 *  @author <a href="mailto:rammi@caff.de">Rammi</a>
 *  @version $Revision: 1.3 $
 */
public final class ReversePathIterator implements PathIterator {
    /** The winding rule. */
    private final int windingRule;
    /** The reversed coordinates. */
    private final double[] coordinates;
    /** The reversed segment types. */
    private final int[] segmentTypes;
    /** The index into the coordinates during iteration. */
    private int coordIndex = 0;
    /** The index into the segment types during iteration. */
    private int segmentIndex = 0;

    /**
     *  Create an inverted path iterator from a standard one, keeping the winding rule.
     *  @param original original iterator
     */
    public ReversePathIterator(PathIterator original) {
        this(original, original.getWindingRule());
    }

    /**
     *  Create an inverted path iterator from a standard one.
     *  @param original original iterator
     *  @param windingRule winding rule of newly created iterator
     */
    public ReversePathIterator(PathIterator original, int windingRule) {
        this.windingRule = windingRule;

        double[] coords = new double[16];
        int coordPos = 0;
        int[] segTypes = new int[8];
        int segPos = 0;
        boolean first = true;

        double[] temp = new double[6];
        while (!original.isDone()) {
            if (segPos == segTypes.length) {
                // resize
                int[] dummy = new int[2 * segPos];
                System.arraycopy(segTypes, 0, dummy, 0, segPos);
                segTypes = dummy;
            }
            final int segType = segTypes[segPos++] = original.currentSegment(temp);
            if (first) {
                if (segType != SEG_MOVETO) {
                    throw new IllegalPathStateException("missing initial moveto in path definition");
                }
                first = false;
            }
            final int copy = coordinatesForSegmentType(segType);
            if (copy > 0) {
                if (coordPos + copy > coords.length) {
                    // resize
                    double[] dummy = new double[coords.length * 2];
                    System.arraycopy(coords, 0, dummy, 0, coords.length);
                    coords = dummy;
                }
                for (int c = 0; c < copy; ++c) {
                    coords[coordPos++] = temp[c];
                }
            }
            original.next();
        }

        // === reverse everything ===
        // --- reverse coordinates ---
        coordinates = new double[coordPos];
        for (int p = coordPos / 2 - 1; p >= 0; --p) {
            coordinates[2 * p] = coords[coordPos - 2 * p - 2];
            coordinates[2 * p + 1] = coords[coordPos - 2 * p - 1];
        }

        // --- reverse segment types ---
        segmentTypes = new int[segPos];
        if (segPos > 0) {
            boolean pendingClose = false;
            int sr = 0;
            segmentTypes[sr++] = SEG_MOVETO;
            for (int s = segPos - 1; s > 0; --s) {
                switch (segTypes[s]) {
                    case SEG_MOVETO:
                        if (pendingClose) {
                            pendingClose = false;
                            segmentTypes[sr++] = SEG_CLOSE;
                        }
                        segmentTypes[sr++] = SEG_MOVETO;
                        break;
                    case SEG_CLOSE:
                        pendingClose = true;
                        break;
                    default:
                        segmentTypes[sr++] = segTypes[s];
                        break;
                }
            }
            if (pendingClose) {
                segmentTypes[sr] = SEG_CLOSE;
            }
        }
    }

    /**
     * Returns the winding rule for determining the interior of the
     * path.
     *
     * @return the winding rule.
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    @Override
    public int getWindingRule() {
        return windingRule;
    }

    /**
     *  Get the number of coordinates needed for a segment type.
     *  @param segtype segment type
     *  @return coordinates needed
     */
    private static int coordinatesForSegmentType(int segtype) {
        switch (segtype) {
            case SEG_MOVETO:
            case SEG_LINETO:
                return 2;
            case SEG_QUADTO:
                return 4;
            case SEG_CUBICTO:
                return 6;
            default:
                return 0;
        }
    }

    /**
     * Moves the iterator to the next segment of the path forwards
     * along the primary direction of traversal as long as there are
     * more points in that direction.
     */
    @Override
    public void next() {
        coordIndex += coordinatesForSegmentType(segmentTypes[segmentIndex++]);
    }

    /**
     * Tests if the iteration is complete.
     *
     * @return <code>true</code> if all the segments have
     *         been read; <code>false</code> otherwise.
     */
    @Override
    public boolean isDone() {
        return segmentIndex >= segmentTypes.length;
    }

    /**
     * Returns the coordinates and type of the current path segment in
     * the iteration.
     * The return value is the path-segment type:
     * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
     * A double array of length 6 must be passed in and can be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of double x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types returns one point,
     * SEG_QUADTO returns two points,
     * SEG_CUBICTO returns 3 points
     * and SEG_CLOSE does not return any points.
     *
     * @param coords an array that holds the data returned from
     *               this method
     * @return the path-segment type of the current path segment.
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    @Override
    public int currentSegment(double[] coords) {
        final int segmentType = segmentTypes[segmentIndex];
        final int copy = coordinatesForSegmentType(segmentType);
        if (copy > 0) {
            System.arraycopy(coordinates, coordIndex, coords, 0, copy);
        }
        return segmentType;
    }

    /**
     * Returns the coordinates and type of the current path segment in
     * the iteration.
     * The return value is the path-segment type:
     * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
     * A float array of length 6 must be passed in and can be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of float x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types returns one point,
     * SEG_QUADTO returns two points,
     * SEG_CUBICTO returns 3 points
     * and SEG_CLOSE does not return any points.
     *
     * @param coords an array that holds the data returned from
     *               this method
     * @return the path-segment type of the current path segment.
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    @Override
    public int currentSegment(float[] coords) {
        final int segmentType = segmentTypes[segmentIndex];
        final int copy = coordinatesForSegmentType(segmentType);
        if (copy > 0) {
            for (int c = 0; c < copy; ++c) {
                coords[c] = (float) coordinates[coordIndex + c];
            }
        }
        return segmentType;
    }
}
