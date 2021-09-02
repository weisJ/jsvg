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
package com.github.weisj.jsvg.nodes.text;

import java.awt.font.GlyphMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.MeasureContext;

class PathGlyphCursor extends GlyphCursor {

    private static final float EPS = 0.0001f;

    private float xStart;
    private float yStart;
    private float segmentLength;

    private final float[] cords;
    private final @NotNull PathIterator pathIterator;

    PathGlyphCursor(@NotNull PathIterator pathIterator, @NotNull AffineTransform transform) {
        super(0, 0, transform);
        this.pathIterator = pathIterator;
        this.cords = new float[2];
        setupIterator(pathIterator);
    }

    PathGlyphCursor(@NotNull GlyphCursor cursor, @NotNull PathIterator pathIterator) {
        super(cursor);
        this.pathIterator = pathIterator;
        this.cords = new float[2];
        setupIterator(pathIterator);
    }

    private PathGlyphCursor(@NotNull PathGlyphCursor pathCursor) {
        super(pathCursor);
        // We only ever transition one into a PathGlyphCursor (from a linear one)
        // hence we can share all out state without the problem of overwriting it.
        this.pathIterator = pathCursor.pathIterator;
        this.xStart = pathCursor.xStart;
        this.yStart = pathCursor.yStart;
        this.segmentLength = pathCursor.segmentLength;
        this.cords = pathCursor.cords;
    }

    @Override
    GlyphCursor derive() {
        return new PathGlyphCursor(this);
    }

    void updateFrom(GlyphCursor local) {
        super.updateFrom(local);
        assert local instanceof PathGlyphCursor;
        PathGlyphCursor glyphCursor = (PathGlyphCursor) local;
        xStart = glyphCursor.xStart;
        yStart = glyphCursor.yStart;
        segmentLength = glyphCursor.segmentLength;
    }

    private void setupIterator(@NotNull PathIterator pathIterator) {
        if (!pathIterator.isDone()) {
            if (pathIterator.currentSegment(cords) != PathIterator.SEG_MOVETO) {
                throw new IllegalStateException("Path iterator didn't establish starting position");
            }
            xStart = cords[0];
            yStart = cords[1];
            x = xStart;
            y = yStart;
        } else {
            xStart = x;
            yStart = y;
        }
    }

    @Override
    @Nullable
    AffineTransform advance(char c, @NotNull MeasureContext measure, @NotNull GlyphMetrics gm, float letterSpacing) {
        // Todo: Incorporate TextSpans. their x/dx properties move along the path, though y/dy doesn't
        if (pathIterator.isDone() && segmentLength < EPS) return null;

        // Safe starting location of glyph
        float curX = x;
        float curY = y;
        // Move the advance of the glyph
        advance(gm.getAdvanceX());

        transform.setToTranslation(curX, curY);
        float charRotation = calculateSegmentRotation(curX, curY, x, y);
        transform.rotate(charRotation, 0, 0);

        advance(letterSpacing);
        return transform;
    }

    private void advance(float distance) {
        advanceInsideSegment(advanceIntoSegment(distance));
    }

    private float advanceIntoSegment(float distance) {
        if (distance < EPS) return 0;
        // Fixme: This gets weird if we are on a vertex.
        while (!pathIterator.isDone() && segmentLength < distance) {
            distance -= segmentLength;
            iterateToNextSegment();
            segmentLength = calculateSegmentLength();
        }
        return distance;
    }

    private void advanceInsideSegment(float distance) {
        if (distance < EPS) return;
        float xStep = cords[0] - x;
        float yStep = cords[1] - y;
        float fraction = distance / segmentLength;
        x += xStep * fraction;
        y += yStep * fraction;
        segmentLength -= distance;
    }

    private void iterateToNextSegment() {
        assert !pathIterator.isDone();
        do {
            x = cords[0];
            y = cords[1];
            switch (pathIterator.currentSegment(cords)) {
                case PathIterator.SEG_CLOSE:
                    // We are closing the path. Restore cords to last moved-to location.
                    cords[0] = xStart;
                    cords[1] = yStart;
                    return;
                case PathIterator.SEG_LINETO:
                    // The coordinates of the segment end are in cords.
                    pathIterator.next();
                    return;
                case PathIterator.SEG_MOVETO:
                    // Moving doesn't advance into a new line segment.
                    xStart = x;
                    yStart = y;
                    break;
                default:
                    throw new IllegalStateException();
            }
            pathIterator.next();
        } while (!pathIterator.isDone());
    }

    private float calculateSegmentRotation(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    private float calculateSegmentLength() {
        return norm(cords[0] - x, cords[1] - y);
    }

    private float norm(float a, float b) {
        return (float) Math.sqrt(a * a + b * b);
    }
}
