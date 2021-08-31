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
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public class RenderContext {

    private final @Nullable JComponent targetComponent;
    private final @NotNull MeasureContext measureContext;
    private final @NotNull PaintContext style;
    private final @NotNull MeasurableFontSpec fontSpec;

    // Todo: font-size-adjust
    // Todo: letter-spacing

    public RenderContext(@Nullable JComponent targetComponent, @NotNull MeasureContext measureContext) {
        this(targetComponent, PaintContext.createDefault(), measureContext, MeasurableFontSpec.createDefault());
    }

    private RenderContext(@Nullable JComponent targetComponent, @NotNull PaintContext style,
            @NotNull MeasureContext measureContext, @NotNull MeasurableFontSpec fontSpec) {
        this.targetComponent = targetComponent;
        this.style = style;
        this.measureContext = measureContext;
        this.fontSpec = fontSpec;
    }

    @NotNull
    RenderContext deriveWith(@Nullable PaintContext context, @Nullable AttributeFontSpec attributeFontSpec,
            @Nullable ViewBox viewBox) {
        if (context == null && viewBox == null) return this;
        PaintContext newPaintContext = style;
        MeasurableFontSpec newFontSpec = fontSpec;
        if (context != null) {
            newPaintContext = new PaintContext(
                    fillPaint(context.fillPaint),
                    fillOpacity(context.fillOpacity),
                    strokePaint(context.strokePaint),
                    strokeOpacity(context.strokeOpacity),
                    opacity(context.opacity));
        }
        if (attributeFontSpec != null) {
            newFontSpec = fontSpec.derive(attributeFontSpec);
        }
        float em = newFontSpec.currentSize().resolveFontSize(measureContext);
        float ex = SVGFont.exFromEm(em);
        MeasureContext newMeasureContext = measureContext.derive(viewBox, em, ex);
        return new RenderContext(targetComponent, newPaintContext, newMeasureContext, newFontSpec);
    }

    public @Nullable JComponent targetComponent() {
        return targetComponent;
    }

    public @NotNull MeasureContext measureContext() {
        return measureContext;
    }

    public @NotNull SVGPaint strokePaint(@Nullable SVGPaint paint) {
        return paint != null ? paint : style.strokePaint;
    }

    public @NotNull SVGPaint fillPaint(@Nullable SVGPaint paint) {
        return paint != null ? paint : style.fillPaint;
    }

    public @Percentage float fillOpacity(@Percentage float opacity) {
        return style.fillOpacity * opacity;
    }

    public @Percentage float strokeOpacity(@Percentage float opacity) {
        return style.strokeOpacity * opacity;
    }

    public @Percentage float opacity(@Percentage float opacity) {
        return style.opacity * opacity;
    }

    public @NotNull SVGFont font(@Nullable AttributeFontSpec fontSpec) {
        return FontResolver.resolve(this.fontSpec.derive(fontSpec), this.measureContext);
    }
}
