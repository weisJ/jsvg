/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
import java.awt.geom.Area;
import java.awt.geom.Path2D;

import javax.swing.*;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.renderer.*;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.awt.AwtComponentPlatformSupport;
import com.github.weisj.jsvg.renderer.impl.*;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.impl.output.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.impl.output.ShapeOutput;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

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

    public @NotNull ViewBox viewBox() {
        return root.staticViewBox(size());
    }

    public @NotNull Shape computeShape() {
        return computeShape(null);
    }

    public @NotNull Shape computeShape(@Nullable ViewBox viewBox) {
        Area accumulator = new Area(new Path2D.Float());
        renderWithPlatform(NullPlatformSupport.INSTANCE, new ShapeOutput(accumulator), viewBox);
        return accumulator;
    }

    public boolean isAnimated() {
        return root.animationPeriod().duration() > 0;
    }

    public @NotNull AnimationPeriod animationPeriod() {
        return root.animationPeriod();
    }

    public void render(@Nullable JComponent component, @NotNull Graphics2D g) {
        render(component, g, null);
    }

    @Deprecated
    public void render(@Nullable JComponent component, @NotNull Graphics2D graphics2D, @Nullable ViewBox bounds) {
        render((Component) component, graphics2D, bounds);
    }

    public void render(@Nullable Component component, @NotNull Graphics2D graphics2D, @Nullable ViewBox bounds) {
        PlatformSupport platformSupport = component != null
                ? new AwtComponentPlatformSupport(component)
                : NullPlatformSupport.INSTANCE;
        renderWithPlatform(platformSupport, graphics2D, bounds);
    }

    private float computePlatformFontSize(@NotNull PlatformSupport platformSupport, @NotNull Output output) {
        return output.contextFontSize().orElseGet(platformSupport::fontSize);
    }

    public void renderWithPlatform(@NotNull PlatformSupport platformSupport, @NotNull Graphics2D graphics2D,
            @Nullable ViewBox bounds) {
        Graphics2D g = (Graphics2D) graphics2D.create();
        setupSVGRenderingHints(g);
        Output output = new Graphics2DOutput(g);
        renderWithPlatform(platformSupport, output, bounds);
        output.dispose();
    }

    public void renderWithPlatform(@NotNull PlatformSupport platformSupport, @NotNull Output output,
            @Nullable ViewBox bounds) {
        renderWithPlatform(platformSupport, output, bounds, null);
    }

    @ApiStatus.Experimental
    public void renderWithPlatform(@NotNull PlatformSupport platformSupport, @NotNull Output output,
            @Nullable ViewBox bounds, @Nullable AnimationState animationState) {
        RenderContext context = prepareRenderContext(platformSupport, output, bounds, animationState);

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

        NodeRenderer.renderWithSize(root, bounds.size(), context, output, null);
    }

    private @NotNull RenderContext prepareRenderContext(
            @NotNull PlatformSupport platformSupport,
            @NotNull Output output,
            @Nullable ViewBox bounds,
            @Nullable AnimationState animationState) {
        float defaultEm = computePlatformFontSize(platformSupport, output);
        float defaultEx = SVGFont.exFromEm(defaultEm);
        AnimationState animState = animationState != null ? animationState : AnimationState.NO_ANIMATION;
        MeasureContext initialMeasure = bounds != null
                ? MeasureContext.createInitial(bounds.size(), defaultEm, defaultEx, animState)
                : MeasureContext.createInitial(root.sizeForTopLevel(defaultEm, defaultEx),
                        defaultEm, defaultEx, animState);
        RenderContext context = RenderContextAccessor.instance().createInitial(platformSupport, initialMeasure);

        root.applyTransform(output, context, new ElementBounds(root, context));
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
        setSVGRenderingHint(g,
                SVGRenderingHints.KEY_MASK_CLIP_RENDERING,
                SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_DEFAULT);
    }

    private void setSVGRenderingHint(@NotNull Graphics2D g, @NotNull RenderingHints.Key key, @NotNull Object o) {
        if (g.getRenderingHint(key) == null) {
            g.setRenderingHint(key, o);
        }
    }
}
