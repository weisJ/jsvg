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
package com.github.weisj.jsvg.nodes.text;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.geometry.util.SegmentIteratorWithLookBehind;

class PathGlyphCursor extends GlyphCursor {

    private float remainingSegmentLength;
    private float segmentLength;
    private SegmentIteratorWithLookBehind.Segment currentSegment;

    private final @NotNull SegmentIteratorWithLookBehind segmentIterator;

    PathGlyphCursor(@NotNull PathIterator pathIterator, float startOffset) {
        super(0, 0, new AffineTransform());
        this.segmentIterator = new SegmentIteratorWithLookBehind(pathIterator, 0);
        setupInitialData();
        advance(startOffset);
    }

    PathGlyphCursor(@NotNull GlyphCursor cursor, float startOffset, @NotNull PathIterator pathIterator) {
        super(cursor);
        this.segmentIterator = new SegmentIteratorWithLookBehind(pathIterator, 0);
        setupInitialData();
        advance(startOffset);
    }

    private void setupInitialData() {
        this.currentSegment = segmentIterator.currentSegment();
        this.segmentLength = this.remainingSegmentLength = (float) currentSegment.length();
        this.x = currentSegment.xStart;
        this.y = currentSegment.yStart;
    }

    private PathGlyphCursor(@NotNull PathGlyphCursor pathCursor) {
        super(pathCursor);
        // We only ever transition one into a PathGlyphCursor (from a linear one)
        // hence we can share all our state without the problem of overwriting it.
        this.segmentIterator = pathCursor.segmentIterator;
        this.remainingSegmentLength = pathCursor.remainingSegmentLength;
        this.segmentLength = pathCursor.segmentLength;
        this.currentSegment = pathCursor.currentSegment;
    }

    @Override
    GlyphCursor derive() {
        return new PathGlyphCursor(this);
    }

    @Override
    void updateFrom(GlyphCursor local) {
        super.updateFrom(local);
        assert local instanceof PathGlyphCursor;
        PathGlyphCursor glyphCursor = (PathGlyphCursor) local;
        remainingSegmentLength = glyphCursor.remainingSegmentLength;
        segmentLength = glyphCursor.segmentLength;
        currentSegment = glyphCursor.currentSegment;
    }

    @Override
    public void setAdvancement(@NotNull GlyphAdvancement advancement) {
        super.setAdvancement(advancement);
        // We don't want to reduce the lookbehind for future segments hence coercion to the bigger value is
        // needed.
        segmentIterator.setMaxLookBehindLength(
                Math.max(advancement.maxLookBehind(), segmentIterator.maxLookBehindLength()));
    }

    @Override
    @Nullable
    AffineTransform advance(@NotNull MeasureContext measure, @NotNull Glyph glyph) {
        // Todo: Absolute x positions require arbitrary moves along the path
        // dx can be done by using the look behind iterator.
        // Absolute x can use a look up table for the segment/state.
        if (segmentIterator.isDone() && remainingSegmentLength < GeometryUtil.EPS) return null;

        float deltaX = nextDeltaX(measure);
        if (deltaX != 0) advance(deltaX);

        // Move the advance of the glyph
        float advanceDist = advancement.glyphAdvancement(glyph);
        float halfAdvance = advanceDist / 2f;

        advance(halfAdvance);
        float walkedFraction = halfAdvance / segmentLength;
        float slopeX = walkedFraction * (currentSegment.xEnd - currentSegment.xStart);
        float slopeY = walkedFraction * (currentSegment.yEnd - currentSegment.yStart);
        float anchorX = x - slopeX;
        float anchorY = y - slopeY;

        // The glyph midpoint is outside the path and should not be made visible. Abort
        if (segmentIterator.isDone() && remainingSegmentLength < GeometryUtil.EPS) return null;
        advance(halfAdvance);

        transform.setToTranslation(anchorX, anchorY);
        float charRotation = calculateSegmentRotation(anchorX, anchorY, x + slopeX, y + slopeY);
        transform.rotate(charRotation, 0, 0);

        float deltaY = nextDeltaY(measure);
        if (deltaY != 0) {
            // Adjust the location along the paths normal vector.
            float nx = -(y - anchorX);
            float ny = (x - anchorY);
            float nn = deltaY / norm(nx, ny);
            transform.translate(nx * nn, ny * nn);
        }

        return advancement.glyphTransform(transform);
    }

    @Override
    void advanceSpacing(float letterSpacing) {
        advance(advancement.spacingAdvancement(letterSpacing));
    }

    private void advance(float distance) {
        if (distance >= 0) {
            advanceInsideSegment(advanceIntoSegment(distance));
        } else {
            advanceInsideSegment(-reverseIntoSegment(-distance));
        }
    }

    private float travelledSegmentLength() {
        return segmentLength - remainingSegmentLength;
    }


    private float advanceIntoSegment(float distance) {
        if (distance < GeometryUtil.EPS) return 0;
        while (segmentIterator.hasNext() && remainingSegmentLength < distance) {
            distance -= remainingSegmentLength;
            segmentIterator.moveToNext();
            currentSegment = segmentIterator.currentSegment();
            x = currentSegment.xStart;
            y = currentSegment.yStart;
            segmentLength = (float) currentSegment.length();
            remainingSegmentLength = segmentLength;
        }
        return distance;
    }

    private float reverseIntoSegment(float distance) {
        if (distance < GeometryUtil.EPS) return 0;
        while (segmentIterator.hasPrevious() && travelledSegmentLength() < distance) {
            distance -= travelledSegmentLength();
            segmentIterator.moveToPrevious();
            currentSegment = segmentIterator.currentSegment();
            x = currentSegment.xEnd;
            y = currentSegment.yEnd;
            segmentLength = (float) currentSegment.length();
            remainingSegmentLength = 0;
        }
        if (distance - travelledSegmentLength() > GeometryUtil.EPS) {
            throw new IllegalStateException("Not enough buffer " + distance + " > " + travelledSegmentLength());
        }
        return distance;
    }

    private void advanceInsideSegment(float distance) {
        if (Math.abs(distance) < GeometryUtil.EPS) return;
        if (distance < 0 && -distance > travelledSegmentLength()) {
            throw new IllegalStateException(
                    "Distance too large " + distance + " of maximum " + travelledSegmentLength());
        }
        float fractionWalked = distance / segmentLength;
        x += (currentSegment.xEnd - currentSegment.xStart) * fractionWalked;
        y += (currentSegment.yEnd - currentSegment.yStart) * fractionWalked;
        remainingSegmentLength -= distance;
    }

    private float calculateSegmentRotation(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    private float norm(float a, float b) {
        return (float) Math.sqrt(a * a + b * b);
    }
}
