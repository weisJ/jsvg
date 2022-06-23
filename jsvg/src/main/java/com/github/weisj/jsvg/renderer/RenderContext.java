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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.image.ImageProducer;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.stroke.StrokeResolver;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.prototype.Mutator;

public class RenderContext {

    private final @Nullable JComponent targetComponent;
    private final @NotNull MeasureContext measureContext;
    private final @NotNull PaintContext paintContext;

    private final @NotNull FontRenderContext fontRenderContext;
    private final @NotNull MeasurableFontSpec fontSpec;

    private final @NotNull FillRule fillRule;

    private final @Nullable ContextElementAttributes contextElementAttributes;


    public static @NotNull RenderContext createInitial(@Nullable JComponent targetComponent,
            @NotNull MeasureContext measureContext) {
        return new RenderContext(targetComponent,
                PaintContext.createDefault(),
                measureContext,
                FontRenderContext.createDefault(),
                MeasurableFontSpec.createDefault(),
                FillRule.Nonzero,
                null);
    }

    RenderContext(@Nullable JComponent targetComponent,
            @NotNull PaintContext paintContext,
            @NotNull MeasureContext measureContext,
            @NotNull FontRenderContext fontRenderContext,
            @NotNull MeasurableFontSpec fontSpec,
            @NotNull FillRule fillRule,
            @Nullable ContextElementAttributes contextElementAttributes) {
        this.targetComponent = targetComponent;
        this.paintContext = paintContext;
        this.measureContext = measureContext;
        this.fontRenderContext = fontRenderContext;
        this.fontSpec = fontSpec;
        this.fillRule = fillRule;
        this.contextElementAttributes = contextElementAttributes;
    }

    @NotNull
    RenderContext derive(@Nullable Mutator<PaintContext> context,
            @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
            @Nullable ViewBox viewBox, @Nullable FontRenderContext frc,
            @Nullable FillRule fillRule, @Nullable ContextElementAttributes contextAttributes) {
        if (context == null && viewBox == null && attributeFontSpec == null && frc == null) return this;
        PaintContext newPaintContext = paintContext;
        MeasurableFontSpec newFontSpec = fontSpec;
        FillRule newFillRule = fillRule != null && fillRule != FillRule.Inherit ? fillRule : this.fillRule;

        if (context != null) newPaintContext = context.mutate(paintContext);
        if (attributeFontSpec != null) newFontSpec = attributeFontSpec.mutate(newFontSpec);

        ContextElementAttributes newContextAttributes = contextElementAttributes;
        if (contextAttributes != null) newContextAttributes = contextAttributes;

        float em = newFontSpec.effectiveSize(measureContext);
        float ex = SVGFont.exFromEm(em);
        MeasureContext newMeasureContext = measureContext.derive(viewBox, em, ex);

        FontRenderContext effectiveFrc = fontRenderContext.derive(frc);

        return new RenderContext(targetComponent, newPaintContext, newMeasureContext, effectiveFrc, newFontSpec,
                newFillRule, newContextAttributes);
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

    public @NotNull FontRenderContext fontRenderContext() {
        return fontRenderContext;
    }

    public @NotNull FillRule fillRule() {
        return fillRule;
    }

    public @NotNull SVGPaint strokePaint() {
        return resolvePaint(paintContext.strokePaint);
    }

    public @NotNull SVGPaint fillPaint() {
        return resolvePaint(paintContext.fillPaint);
    }

    private @NotNull SVGPaint resolvePaint(@Nullable SVGPaint p) {
        if (p == SVGPaint.DEFAULT_PAINT || p == SVGPaint.CURRENT_COLOR) {
            // color can only hold resolved values being declared as literals
            return coerceNonNull(paintContext.color);
        }
        if (p == SVGPaint.CONTEXT_STROKE) {
            // value is already absolute hence needs no special treatment.
            if (contextElementAttributes == null) return SVGPaint.NONE;
            return contextElementAttributes.strokePaint;
        }
        if (p == SVGPaint.CONTEXT_FILL) {
            // value is already absolute hence needs no special treatment.
            if (contextElementAttributes == null) return SVGPaint.NONE;
            return contextElementAttributes.fillPaint;
        }
        return coerceNonNull(p);
    }

    private @NotNull SVGPaint coerceNonNull(@Nullable SVGPaint p) {
        return p != null ? p : SVGPaint.DEFAULT_PAINT;
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
                ",\n fillRule=" + fillRule +
                "\n}";
    }

    public @NotNull Image createImage(@NotNull ImageProducer imageProducer) {
        if (targetComponent != null) {
            return targetComponent.createImage(imageProducer);
        } else {
            return Toolkit.getDefaultToolkit().createImage(imageProducer);
        }
    }
}
