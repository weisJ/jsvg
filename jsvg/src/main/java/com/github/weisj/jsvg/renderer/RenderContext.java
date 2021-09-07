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
package com.github.weisj.jsvg.renderer;

import java.awt.*;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.stroke.StrokeResolver;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public class RenderContext {

    private final @Nullable JComponent targetComponent;
    private final @NotNull MeasureContext measureContext;
    private final @NotNull PaintContext paintContext;
    private final @NotNull MeasurableFontSpec fontSpec;

    private final @Nullable ContextElementAttributes contextElementAttributes;

    public static @NotNull RenderContext createInitial(@Nullable JComponent targetComponent,
            @NotNull MeasureContext measureContext) {
        return new RenderContext(targetComponent,
                PaintContext.createDefault(),
                measureContext,
                MeasurableFontSpec.createDefault(),
                null);
    }

    RenderContext(@Nullable JComponent targetComponent,
            @NotNull PaintContext paintContext,
            @NotNull MeasureContext measureContext,
            @NotNull MeasurableFontSpec fontSpec,
            @Nullable ContextElementAttributes contextElementAttributes) {
        this.targetComponent = targetComponent;
        this.paintContext = paintContext;
        this.measureContext = measureContext;
        this.fontSpec = fontSpec;
        this.contextElementAttributes = contextElementAttributes;
    }

    @NotNull
    RenderContext derive(@Nullable PaintContext context, @Nullable AttributeFontSpec attributeFontSpec,
            @Nullable ViewBox viewBox, @Nullable FontRenderContext frc,
            @Nullable ContextElementAttributes contextAttributes) {
        if (context == null && viewBox == null && attributeFontSpec == null && frc == null) return this;
        PaintContext newPaintContext = paintContext;
        MeasurableFontSpec newFontSpec = fontSpec;

        if (context != null) {
            newPaintContext = new PaintContext(
                    context.color != null ? context.color : paintContext.color,
                    context.fillPaint != null ? context.fillPaint : paintContext.fillPaint,
                    paintContext.fillOpacity * context.fillOpacity,
                    context.strokePaint != null ? context.strokePaint : paintContext.strokePaint,
                    paintContext.strokeOpacity * context.strokeOpacity,
                    paintContext.opacity * context.opacity,
                    strokeContext().derive(context.strokeContext));
        }
        if (attributeFontSpec != null) {
            newFontSpec = newFontSpec.derive(attributeFontSpec);
        }

        ContextElementAttributes newContextAttributes = contextElementAttributes;
        if (contextAttributes != null) newContextAttributes = contextAttributes;

        float em = newFontSpec.effectiveSize(measureContext);
        float ex = SVGFont.exFromEm(em);
        MeasureContext newMeasureContext = measureContext.derive(viewBox, em, ex, frc);
        return new RenderContext(targetComponent, newPaintContext, newMeasureContext, newFontSpec,
                newContextAttributes);
    }

    public @NotNull StrokeContext strokeContext() {
        // This will never be null for a RenderContext.
        // Our deriving mechanism together with non-null initial values prohibits this.
        assert paintContext.strokeContext != null;
        return paintContext.strokeContext;
    }

    @Nullable
    ContextElementAttributes contextElementAttributes() {
        return contextElementAttributes;
    }

    public @Nullable JComponent targetComponent() {
        return targetComponent;
    }

    public @NotNull MeasureContext measureContext() {
        return measureContext;
    }

    public @NotNull SVGPaint strokePaint() {
        return resolvePaint(paintContext.strokePaint);
    }

    public @NotNull SVGPaint fillPaint() {
        return resolvePaint(paintContext.fillPaint);
    }

    private @NotNull SVGPaint resolvePaint(@NotNull SVGPaint p) {
        if (p == SVGPaint.DEFAULT_PAINT) return paintContext.color;
        if (p == SVGPaint.CONTEXT_STROKE) {
            if (contextElementAttributes == null) return SVGPaint.NONE;
            return contextElementAttributes.strokePaint;
        }
        if (p == SVGPaint.CONTEXT_FILL) {
            if (contextElementAttributes == null) return SVGPaint.NONE;
            return contextElementAttributes.fillPaint;
        }
        return p;
    }

    public @Percentage float rawOpacity() {
        return paintContext.opacity;
    }

    public @Percentage float fillOpacity() {
        return paintContext.fillOpacity * paintContext.opacity;
    }

    public @Percentage float strokeOpacity() {
        return paintContext.strokeOpacity * paintContext.opacity;
    }

    public @NotNull Stroke stroke(float pathLengthFactor) {
        return StrokeResolver.resolve(pathLengthFactor, measureContext, strokeContext());
    }

    public @NotNull SVGFont font() {
        return FontResolver.resolve(this.fontSpec, this.measureContext);
    }

    @Override
    public String toString() {
        return "RenderContext{" +
                "\n  measureContext=" + measureContext +
                ",\n  paintContext=" + paintContext +
                ",\n  fontSpec=" + fontSpec +
                ",\n  targetComponent=" + targetComponent +
                ",\n  contextElementAttributes=" + contextElementAttributes +
                "\n}";
    }
}
