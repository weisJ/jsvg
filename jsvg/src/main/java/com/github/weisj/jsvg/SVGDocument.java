/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;

import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.impl.DocumentConstructorAccessor;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import com.github.weisj.jsvg.renderer.RenderConfig;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.animation.Animation;
import com.github.weisj.jsvg.renderer.awt.AwtComponentPlatformSupport;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.CurrentColorProvider;
import com.github.weisj.jsvg.renderer.output.impl.ShapeOutput;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

public final class SVGDocument {
    private static final boolean DEBUG = false;
    private final @NotNull SVG root;
    private final @NotNull FloatSize size;

    static {
        DocumentConstructorAccessor.setDocumentConstructor(SVGDocument::new);
    }

    private SVGDocument(@NotNull SVG root) {
        this.root = root;
        this.size = sizeForViewport(null);
    }

    public @NotNull FloatSize size() {
        return size;
    }

    public @NotNull FloatSize sizeForViewport(@Nullable ViewBox viewport) {
        float em = SVGFont.defaultFontSize();
        return root.sizeForTopLevel(viewport, em, SVGFont.exFromEm(em));
    }

    public @NotNull ViewBox viewBox() {
        return root.staticViewBox(size());
    }

    public @NotNull Shape computeShape() {
        return computeShape(null);
    }

    public @NotNull Shape computeShape(@Nullable ViewBox viewBox) {
        Area accumulator = new Area(new Path2D.Float());
        render(new ShapeOutput(accumulator), RenderConfig.builder().viewBox(viewBox).build());
        return accumulator;
    }

    public boolean isAnimated() {
        return animation().duration() > 0;
    }

    public @NotNull Animation animation() {
        return root.animationPeriod();
    }

    public void render(@Nullable JComponent component, @NotNull Graphics2D g) {
        render(component, g, null);
    }

    public void render(@Nullable Component component, @NotNull Graphics2D graphics2D, @Nullable ViewBox bounds) {
        PlatformSupport platformSupport = component != null
                ? new AwtComponentPlatformSupport(component)
                : NullPlatformSupport.INSTANCE;
        RenderConfig.Builder config = RenderConfig.builder()
                .platformSupport(platformSupport)
                .viewBox(bounds);
        Font componentFont = component != null ? component.getFont() : null;
        if (componentFont != null) {
            config.fontSize(componentFont.getSize2D())
                    .defaultFontFamily(componentFont.getFamily());
        }
        render(graphics2D, config.build());
    }

    public void render(@NotNull Graphics2D graphics2D, @NotNull RenderConfig config) {
        Output output = Output.createForGraphics(graphics2D);
        try {
            render(output, config);
        } finally {
            output.dispose();
        }
    }

    public void render(@NotNull Output output, @NotNull RenderConfig config) {
        ViewBox viewportBounds = config.viewBox();

        if (viewportBounds != null) {
            output.translate(viewportBounds.x, viewportBounds.y);
        }

        RenderContext context = prepareRenderContext(config, output);

        // Needed for the non-scaling-stroke vector-effect to work properly.
        RenderContextAccessor.Accessor accessor = RenderContextAccessor.instance();
        accessor.setHostTransform(context, output.transform());

        ViewBox fallbackViewbox = new ViewBox(root.size(context));
        if (viewportBounds == null) viewportBounds = fallbackViewbox;

        if (DEBUG) {
            final ViewBox finalBounds = viewportBounds;
            output.debugPaint(g -> {
                g.setColor(Color.RED);
                g.draw(finalBounds);
            });
        }

        ViewBox rootViewBox = root.viewBox(context);
        RenderContext viewContext = root.createInnerContextForViewBox(
                viewportBounds.size(), rootViewBox, context, output);

        // Needed for other vector-effects to work properly.
        accessor.setTransforms(viewContext, output.transform());

        ViewBox clipViewbox = rootViewBox != null ? rootViewBox : fallbackViewbox;
        output.applyClip(clipViewbox);

        NodeRenderer.renderNode(root, viewContext, output);
    }

    private @NotNull RenderContext prepareRenderContext(
            @NotNull RenderConfig config,
            @NotNull Output output) {
        PlatformSupport platformSupport = config.platformSupport();
        ViewBox viewportBounds = config.viewBox();
        float defaultEm = config.fontSize();
        float defaultEx = SVGFont.exFromEm(defaultEm);
        FloatSize initialViewportSize = viewportBounds != null
                ? viewportBounds.size()
                : root.sizeForTopLevel(null, defaultEm, defaultEx);
        MeasureContext initialMeasure =
                MeasureContext.createInitial(initialViewportSize, defaultEm, defaultEx, config.animationState());
        SVGPaint currentColor = null;
        if (output instanceof CurrentColorProvider) {
            currentColor = ((CurrentColorProvider) output).currentColor();
        }
        return RenderContextAccessor.instance().createInitial(
                currentColor, platformSupport, config.fontLoader(), config.defaultFontFamily(), initialMeasure);
    }

}
