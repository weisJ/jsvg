/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.impl;

import java.awt.*;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.jfx.impl.bridge.FXShapeBridge;

public final class FXOutputClipStack {

    private final FXOutput fxOutput;
    private final Deque<ClipShape> clipStack = new ArrayDeque<>();

    FXOutputClipStack(FXOutput fxOutput) {
        this.fxOutput = fxOutput;
    }

    public void pushClip(Shape awtClipShape) {
        PathIterator awtIterator = awtClipShape.getPathIterator(null);
        FXShapeBridge.appendPathIterator(fxOutput.ctx, awtIterator);
        FXShapeBridge.applyWindingRule(fxOutput.ctx, awtIterator.getWindingRule());

        int savePoint = fxOutput.ctxSaveCounter.save();
        fxOutput.ctx.clip();

        clipStack.add(new ClipShape(awtClipShape, savePoint));
    }

    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        FXOutputState currentState = new FXOutputState(fxOutput, FXOutputState.SaveClipStack.NO);
        ClipShape clipShape = clipStack.removeLast();
        fxOutput.ctxSaveCounter.restoreTo(clipShape.savePoint);
        currentState.restore();
    }

    public void clearClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        FXOutputState currentState = new FXOutputState(fxOutput, FXOutputState.SaveClipStack.NO);
        while (!clipStack.isEmpty()) {
            ClipShape clipShape = clipStack.removeLast();
            fxOutput.ctxSaveCounter.restoreTo(clipShape.savePoint);
        }
        currentState.restore();
    }

    public void restoreClipStack(@NotNull java.util.List<Shape> originalClipStack) {
        if (clipStack.isEmpty() && originalClipStack.isEmpty()) {
            return;
        }

        FXOutputState currentState = new FXOutputState(fxOutput, FXOutputState.SaveClipStack.NO);

        int validClips = 0;
        int minSize = Math.min(clipStack.size(), originalClipStack.size());

        // Compare clips in both stacks to find the first non-matching clip
        for (ClipShape currentClip : clipStack) {
            if (validClips >= minSize) {
                break;
            }
            Shape originalClipShape = originalClipStack.get(validClips);

            if (currentClip == null || !currentClip.shape.equals(originalClipShape)) {
                break;
            }
            validClips++;
        }

        // Remove invalid clips from the current stack
        int clipsToRemove = clipStack.size() - validClips;
        for (int i = 0; i < clipsToRemove; i++) {
            ClipShape clipShape = clipStack.removeLast();
            fxOutput.ctxSaveCounter.restoreTo(clipShape.savePoint);
        }

        currentState.restore();

        // Add missing clips from the original stack
        for (int i = validClips; i < originalClipStack.size(); i++) {
            Shape originalClipShape = originalClipStack.get(i);
            fxOutput.applyClip(originalClipShape);
        }
    }

    public java.util.List<Shape> snapshot() {
        List<Shape> snapshot = new ArrayList<>(this.clipStack.size());
        for (ClipShape clipShape : this.clipStack) {
            snapshot.add(clipShape.shape);
        }
        return snapshot;
    }

    public Rectangle2D getClipBounds() {
        if (clipStack.isEmpty()) {
            return new Rectangle2D.Double(0, 0, fxOutput.ctx.getCanvas().getWidth(),
                    fxOutput.ctx.getCanvas().getHeight());
        }
        return clipStack.peekLast().getBounds();
    }

    private static class ClipShape {

        private final Shape shape;
        private Rectangle2D bounds;
        private final int savePoint; // Save point before the clip has been applied

        private ClipShape(Shape shape, int savePoint) {
            this.savePoint = savePoint;
            this.shape = shape;
        }

        public Rectangle2D getBounds() {
            if (bounds == null) {
                // We need to apply the save points inverse transform to this
                bounds = shape.getBounds2D();
            }
            return bounds;
        }
    }
}
