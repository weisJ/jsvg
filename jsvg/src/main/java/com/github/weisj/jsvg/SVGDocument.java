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
package com.github.weisj.jsvg;

import java.awt.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.renderer.RenderContext;

public class SVGDocument {
    private static final boolean DEBUG = false;
    private final @NotNull SVG root;

    public SVGDocument(@NotNull SVG root) {
        this.root = root;
    }

    public @NotNull FloatSize size() {
        float em = SVGFont.defaultFontSize();
        return root.sizeForTopLevel(em, SVGFont.exFromEm(em));
    }

    public void render(@Nullable JComponent component, @NotNull Graphics2D g) {
        render(component, g, null);
    }

    public void render(@Nullable JComponent component, @NotNull Graphics2D g, @Nullable ViewBox bounds) {
        Font f = g.getFont();
        if (f == null && component != null) f = component.getFont();
        float defaultEm = f != null ? f.getSize2D() : SVGFont.defaultFontSize();
        float defaultEx = SVGFont.exFromEm(defaultEm);

        MeasureContext initialMeasure = bounds != null
                ? MeasureContext.createInitial(bounds.size(), defaultEm, defaultEx)
                : MeasureContext.createInitial(root.sizeForTopLevel(defaultEm, defaultEx), defaultEm, defaultEx);
        RenderContext context = RenderContext.createInitial(component, initialMeasure);

        if (bounds == null) bounds = new ViewBox(root.size(context));

        // A transform on an <svg> element should behave as if it were applied on the parent.
        // It seems like this implies that any rotations etc. should behave as if they were centered
        // on the viewport.
        AffineTransform transform = root.transform();
        if (transform != null) {
            g.translate(bounds.width / 2, bounds.height / 2);
            g.transform(transform);
            g.translate(-bounds.width / 2, -bounds.height / 2);
        }

        if (DEBUG) {
            g.setColor(Color.MAGENTA);
            g.draw(bounds);
        }

        g.clip(bounds);
        g.translate(bounds.x, bounds.y);

        root.renderWithSize(bounds.size(), context, g);
    }
}
