/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.Output;

/**
 * A utility class that holds a {@link Graphics2D} object and is able to reset it back to its original configuration,
 * as this is often more efficient than creating a new graphics instance.
 * <p>
 * This class does not track what parameters have been modified, nor does it reset all configuration parameters. Which
 * parameters are reset should be expanded as needed.
 */
public class GraphicsResetHelper implements Output.SafeState {

    private final Graphics2D graphics;

    private final Composite originalComposite;
    private final Paint originalPaint;
    private final Stroke originalStroke;
    private final AffineTransform originalTransform;

    public GraphicsResetHelper(@NotNull Graphics2D graphics) {
        this.graphics = graphics;

        originalComposite = graphics.getComposite();
        originalPaint = graphics.getPaint();
        originalStroke = graphics.getStroke();
        originalTransform = graphics.getTransform();
    }

    public @NotNull Graphics2D graphics() {
        return graphics;
    }

    @Override
    public void restore() {
        graphics.setComposite(originalComposite);
        graphics.setPaint(originalPaint);
        graphics.setStroke(originalStroke);
        graphics.setTransform(originalTransform);
    }
}
