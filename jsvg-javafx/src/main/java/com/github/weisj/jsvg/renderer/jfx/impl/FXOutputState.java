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
import java.awt.geom.AffineTransform;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.output.Output;

public final class FXOutputState implements Output.SafeState {

    public enum SaveClipStack {
        YES,
        NO
    }

    private final FXOutput fxOutput;
    private final AffineTransform originalTransform;
    private final Paint originalPaint;
    private final Stroke originalStroke;
    private final float originalOpacity;
    private final List<Shape> originalClipStack;

    FXOutputState(FXOutput fxOutput, SaveClipStack saveClip) {
        this.fxOutput = fxOutput;
        this.originalTransform = fxOutput.transform();
        this.originalPaint = fxOutput.currentPaint;
        this.originalStroke = fxOutput.currentStroke;
        this.originalOpacity = fxOutput.currentOpacity;
        this.originalClipStack = saveClip == SaveClipStack.YES ? fxOutput.clipStack.snapshot() : null;
    }

    public @NotNull GraphicsContext context() {
        return fxOutput.ctx;
    }

    @Override
    public void restore() {
        if (originalClipStack != null) {
            fxOutput.clipStack.restoreClipStack(originalClipStack);
        }
        fxOutput.setOpacity(originalOpacity);
        fxOutput.setTransform(originalTransform);
        fxOutput.setPaint(originalPaint);
        fxOutput.setStroke(originalStroke);
    }
}
