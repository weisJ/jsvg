/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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

import java.awt.geom.PathIterator;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.Length;

public class SegmentIteratorWithLookBehind {

    private final @NotNull PathIterator pathIterator;
    private float maxLookBehindLength;
    private float currentLookBehindLength = 0;

    private final ArrayList<Segment> lookBehind = new ArrayList<>();
    private Segment currentSegment;

    private final float[] cords = new float[2];
    private float moveToX;
    private float moveToY;

    private int lookBehindCursor = -1;


    public SegmentIteratorWithLookBehind(@NotNull PathIterator pathIterator, float maxLookBehindLength) {
        this.pathIterator = pathIterator;
        this.maxLookBehindLength = maxLookBehindLength;
        prepareFirstSegment();
    }

    private void prepareFirstSegment() {
        this.currentSegment = new Segment(
                Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW,
                Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW);
        moveToNext();
        if (Length.isUnspecified(currentSegment.xStart) || Length.isUnspecified(currentSegment.yStart)) {
            throw new IllegalStateException("Path iterator did not establish starting position");
        }
    }

    public void setMaxLookBehindLength(float maxLookBehindLength) {
        this.maxLookBehindLength = maxLookBehindLength;
        trimLookBehindIfNecessary();
    }

    public float maxLookBehindLength() {
        return maxLookBehindLength;
    }

    public boolean hasNext() {
        return lookBehindCursor >= 0 || !pathIterator.isDone();
    }

    public boolean isDone() {
        return !hasNext();
    }

    public boolean hasPrevious() {
        return lookBehindCursor < lookBehind.size() - 1;
    }

    public @NotNull Segment currentSegment() {
        if (lookBehindCursor >= 0) {
            return lookBehind.get(lookBehind.size() - 1 - lookBehindCursor);
        } else {
            return currentSegment;
        }
    }

    public void moveToPrevious() {
        if (!hasPrevious()) {
            throw new IllegalStateException("Can't move back anymore. Maximum capacity is " + maxLookBehindLength);
        }
        lookBehindCursor++;
    }

    public void moveToNext() {
        if (lookBehindCursor >= 0) {
            lookBehindCursor--;
            return;
        }
        Segment nextSegment = new Segment(
                currentSegment.xEnd, currentSegment.yEnd,
                Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW);

        outer: while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(cords)) {
                case PathIterator.SEG_MOVETO:
                    nextSegment.xStart = moveToX = cords[0];
                    nextSegment.yStart = moveToY = cords[1];
                    nextSegment.moveHappened = true;
                    pathIterator.next();
                    break;
                case PathIterator.SEG_CLOSE:
                    nextSegment.xEnd = moveToX;
                    nextSegment.yEnd = moveToY;
                    pathIterator.next();
                    break outer;
                case PathIterator.SEG_LINETO:
                    nextSegment.xEnd = cords[0];
                    nextSegment.yEnd = cords[1];
                    pathIterator.next();
                    break outer;
                default:
                    throw new IllegalStateException("Unsupported segment type");
            }
        }
        if (Float.isNaN(nextSegment.xEnd) || Float.isNaN(nextSegment.yEnd)) {
            nextSegment.xEnd = nextSegment.xStart;
            nextSegment.yEnd = nextSegment.yStart;
        }

        if (maxLookBehindLength > 0) {
            lookBehind.add(currentSegment);
            currentLookBehindLength += currentSegment.length();

            trimLookBehindIfNecessary();
        }
        currentSegment = nextSegment;
    }

    private void trimLookBehindIfNecessary() {
        while (currentLookBehindLength - maxLookBehindLength > GeometryUtil.EPS) {
            Segment segment = lookBehind.get(0);
            double segmentLength = segment.length();
            if (this.currentLookBehindLength - segmentLength < maxLookBehindLength) break;
            this.currentLookBehindLength -= segmentLength;
            lookBehind.remove(0);
        }
    }

    public static class Segment {
        public float xStart;
        public float yStart;
        public float xEnd;
        public float yEnd;

        public boolean moveHappened;

        private Segment(float xStart, float yStart, float xEnd, float yEnd) {
            this.xStart = xStart;
            this.yStart = yStart;
            this.xEnd = xEnd;
            this.yEnd = yEnd;
        }

        public double length() {
            return GeometryUtil.lineLength(xStart, yStart, xEnd, yEnd);
        }

        @Override
        public String toString() {
            return String.format("[%.2f,%.2f] -> [%.2f,%.2f] (moved: %b)", xStart, yStart, xEnd, yEnd, moveHappened);
        }
    }
}
