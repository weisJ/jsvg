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
package com.github.weisj.jsvg;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

import javax.swing.*;

import com.github.weisj.jsvg.nodes.View;
import com.github.weisj.jsvg.renderer.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.awt.JComponentPlatformSupport;
import com.github.weisj.jsvg.renderer.awt.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.awt.PlatformSupport;

public final class SVGDocument {
    private static final boolean DEBUG = false;
    private final @NotNull SVG root;
    private final @NotNull FloatSize size;

    public SVGDocument(@NotNull SVG root) {
        this.root = root;
        float em = SVGFont.defaultFontSize();
        this.size = root.sizeForTopLevel(em, SVGFont.exFromEm(em));
    }

    public @NotNull FloatSize size() {
        return size;
    }

    public @NotNull Shape computeShape() {
        return computeShape(null);
    }

    public @NotNull Shape computeShape(@Nullable ViewBox viewBox) {
        Output output = null;
        RenderContext context = prepareRenderContext(new NullPlatformSupport(), output, viewBox);
        Shape shape = root.elementShape(context);
        if (viewBox != null) {
            System.out.println(context.userSpaceTransform());
            shape = context.userSpaceTransform().createTransformedShape(shape);
        }
        return shape;
    }

    public void render(@Nullable JComponent component, @NotNull Graphics2D g) {
        render(component, g, null);
    }

    public void render(@Nullable JComponent component, @NotNull Graphics2D graphics2D, @Nullable ViewBox bounds) {
        PlatformSupport platformSupport = component != null
                ? new JComponentPlatformSupport(component)
                : new NullPlatformSupport();
        Graphics2D g = (Graphics2D) graphics2D.create();
        setupSVGRenderingHints(g);
        Output output = new Graphics2DOutput(g);
        renderWithPlatform(platformSupport, output, bounds);
        output.dispose();
    }

    private float computePlatformFontSize(@NotNull PlatformSupport platformSupport, @NotNull Output output) {
        return output.contextFontSize().orElseGet(platformSupport::fontSize);
    }

    public void renderWithPlatform(@NotNull PlatformSupport platformSupport, @NotNull Output output,
            @Nullable ViewBox bounds) {
        RenderContext context = prepareRenderContext(platformSupport, output, bounds);

        if (bounds == null) bounds = new ViewBox(root.size(context));


        if (DEBUG) {
            final ViewBox finalBounds = bounds;
            output.debugPaint(g -> {
                g.setColor(Color.RED);
                g.draw(finalBounds);
            });
        }

        output.applyClip(bounds);
        output.translate(bounds.x, bounds.y);

        try (NodeRenderer.Info info = NodeRenderer.createRenderInfo(root, context, output, null)) {
            Objects.requireNonNull(info);
            root.renderWithSize(bounds.size(), root.viewBox(context), info.context, info.output);
        }
    }

    private @NotNull RenderContext prepareRenderContext(
            @NotNull PlatformSupport platformSupport,
            @NotNull Output output,
            @Nullable ViewBox bounds) {
        float defaultEm = computePlatformFontSize(platformSupport, output);
        float defaultEx = SVGFont.exFromEm(defaultEm);
        // TODO: Abstract away Graphics2D to allow a "ShapeCollector" context.
        MeasureContext initialMeasure = bounds != null
                ? MeasureContext.createInitial(bounds.size(), defaultEm, defaultEx)
                : MeasureContext.createInitial(root.sizeForTopLevel(defaultEm, defaultEx), defaultEm, defaultEx);
        RenderContext context = RenderContext.createInitial(platformSupport, initialMeasure);

        root.applyTransform(output, context);
        return context;
    }

    private void setupSVGRenderingHints(@NotNull Graphics2D g) {
        Object aaHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (aaHint != RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
            setSVGRenderingHint(g,
                    SVGRenderingHints.KEY_IMAGE_ANTIALIASING,
                    aaHint == RenderingHints.VALUE_ANTIALIAS_ON
                            ? SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON
                            : SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if (g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL) == RenderingHints.VALUE_STROKE_DEFAULT) {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
    }

    private void setSVGRenderingHint(@NotNull Graphics2D g, @NotNull RenderingHints.Key key, @NotNull Object o) {
        if (g.getRenderingHint(key) == null) {
            g.setRenderingHint(key, o);
        }
    }
}
