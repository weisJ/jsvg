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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.attributes.stroke.StrokeResolver;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.impl.PaintResolver;
import com.github.weisj.jsvg.renderer.impl.context.*;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.ViewBox;

public final class RenderContext {

    private final @NotNull PlatformSupport platformSupport;
    private final @NotNull MeasureContext measureContext;
    private final @NotNull PaintContext paintContext;

    private final @NotNull FontRenderContext fontRenderContext;
    private final @NotNull MeasurableFontSpec fontSpec;


    private final @Nullable ContextElementAttributes contextElementAttributes;

    private final @NotNull AffineTransform rootTransform;
    private final @NotNull AffineTransform userSpaceTransform;

    static {
        RenderContextAccessor.setInstance(new RenderContextAccessor.Accessor() {
            public @NotNull RenderContext createInitial(@NotNull PlatformSupport awtSupport,
                    @NotNull MeasureContext measureContext) {
                return RenderContext.createInitial(awtSupport, measureContext);
            }

            public @NotNull RenderContext deriveForSurface(@NotNull RenderContext context) {
                return context.deriveForSurface();
            }

            public @NotNull RenderContext deriveForChildGraphics(@NotNull RenderContext context) {
                return context.deriveForChildGraphics();
            }

            @Override
            public @NotNull RenderContext deriveForNode(
                    @NotNull RenderContext context,
                    @Nullable Mutator<PaintContext> paintContextMutator,
                    @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
                    @Nullable FontRenderContext frc,
                    @Nullable ContextElementAttributes contextAttributes,
                    @NotNull Object node) {
                return context.deriveForNode(paintContextMutator, attributeFontSpec, frc, contextAttributes, node);
            }

            @Override
            public @NotNull RenderContext setupInnerViewRenderContext(@NotNull ViewBox viewBox,
                    @NotNull RenderContext context, boolean inheritAttributes) {
                if (inheritAttributes) {
                    return context.derive(null, null, viewBox, null, null,
                            RenderContext.EstablishRootMeasure.NO);
                } else {
                    MeasureContext newMeasure = context.measureContext().derive(viewBox,
                            Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW);
                    return new RenderContext(
                            context.platformSupport(),
                            new AffineTransform(),
                            new AffineTransform(),
                            PaintContext.createDefault(),
                            newMeasure,
                            FontRenderContext.createDefault(),
                            MeasurableFontSpec.createDefault(),
                            context.contextElementAttributes());
                }
            }


            @Override
            public @NotNull StrokeContext strokeContext(@NotNull RenderContext context) {
                return context.strokeContext();
            }

            @Override
            public @NotNull FontRenderContext fontRenderContext(@NotNull RenderContext context) {
                return context.fontRenderContext();
            }

            @Override
            public @NotNull FillRule fillRule(@NotNull RenderContext context) {
                return context.fillRule();
            }

            @Override
            public @NotNull PaintOrder paintOrder(@NotNull RenderContext context) {
                return context.paintOrder();
            }

            @Override
            public @NotNull SVGFont font(@NotNull RenderContext context) {
                return context.font();
            }

            @Override
            public void setRootTransform(@NotNull RenderContext context, @NotNull AffineTransform rootTransform) {
                context.setRootTransform(rootTransform);
            }

            @Override
            public void setRootTransform(@NotNull RenderContext context, @NotNull AffineTransform rootTransform,
                    @NotNull AffineTransform userSpaceTransform) {
                context.setRootTransform(rootTransform, userSpaceTransform);
            }
        });
    }


    private static @NotNull RenderContext createInitial(@NotNull PlatformSupport awtSupport,
            @NotNull MeasureContext measureContext) {
        return new RenderContext(awtSupport,
                new AffineTransform(),
                new AffineTransform(),
                com.github.weisj.jsvg.renderer.impl.context.PaintContext.createDefault(),
                measureContext,
                FontRenderContext.createDefault(),
                MeasurableFontSpec.createDefault(),
                null);
    }

    private RenderContext(@NotNull PlatformSupport platformSupport,
            @NotNull AffineTransform rootTransform,
            @NotNull AffineTransform userSpaceTransform,
            @NotNull com.github.weisj.jsvg.renderer.impl.context.PaintContext paintContext,
            @NotNull MeasureContext measureContext,
            @NotNull FontRenderContext fontRenderContext,
            @NotNull MeasurableFontSpec fontSpec,
            @Nullable ContextElementAttributes contextElementAttributes) {
        this.platformSupport = platformSupport;
        this.rootTransform = rootTransform;
        this.userSpaceTransform = userSpaceTransform;
        this.paintContext = paintContext;
        this.measureContext = measureContext;
        this.fontRenderContext = fontRenderContext;
        this.fontSpec = fontSpec;
        this.contextElementAttributes = contextElementAttributes;
    }

    private enum EstablishRootMeasure {
        YES,
        NO
    }

    @NotNull
    private RenderContext derive(
            @Nullable Mutator<PaintContext> context,
            @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
            @Nullable ViewBox viewBox, @Nullable FontRenderContext frc,
            @Nullable ContextElementAttributes contextAttributes,
            EstablishRootMeasure establishRootMeasure) {
        return deriveImpl(context, attributeFontSpec, viewBox, frc, contextAttributes, null,
                establishRootMeasure);
    }

    @NotNull
    private RenderContext deriveImpl(
            @Nullable Mutator<com.github.weisj.jsvg.renderer.impl.context.PaintContext> context,
            @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
            @Nullable ViewBox viewBox, @Nullable FontRenderContext frc,
            @Nullable ContextElementAttributes contextAttributes,
            @Nullable AffineTransform rootTransform,
            EstablishRootMeasure establishRootMeasure) {
        if (context == null && viewBox == null && attributeFontSpec == null && frc == null) return this;
        PaintContext newPaintContext = paintContext;
        MeasurableFontSpec newFontSpec = fontSpec;

        if (context != null) newPaintContext = context.mutate(paintContext);
        if (attributeFontSpec != null) newFontSpec = attributeFontSpec.mutate(newFontSpec);

        ContextElementAttributes newContextAttributes = contextElementAttributes;
        if (contextAttributes != null) newContextAttributes = contextAttributes;

        float em = newFontSpec.emSize(measureContext);
        float ex = SVGFont.exFromEm(em);
        MeasureContext newMeasureContext = measureContext.derive(viewBox, em, ex);

        if (establishRootMeasure == EstablishRootMeasure.YES) {
            newMeasureContext = newMeasureContext.deriveRoot(em);
        }

        FontRenderContext effectiveFrc = fontRenderContext.derive(frc);
        AffineTransform newRootTransform = rootTransform != null ? rootTransform : this.rootTransform;

        return new RenderContext(platformSupport, newRootTransform, new AffineTransform(userSpaceTransform),
                newPaintContext, newMeasureContext, effectiveFrc, newFontSpec, newContextAttributes);
    }

    private @NotNull RenderContext deriveForChildGraphics() {
        // Pass non-trivial context mutator to ensure userSpaceTransform gets created a different copy.
        return derive(t -> t, null, null, null, null, EstablishRootMeasure.NO);
    }

    private @NotNull RenderContext deriveForSurface() {
        return deriveImpl(t -> t, null, null, null, null,
                new AffineTransform(rootTransform), EstablishRootMeasure.NO);
    }

    private @NotNull RenderContext deriveForNode(
            @Nullable Mutator<PaintContext> context,
            @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
            @Nullable FontRenderContext frc,
            @Nullable ContextElementAttributes contextAttributes,
            @NotNull Object node) {
        EstablishRootMeasure establishRootMeasure = node instanceof SVG && ((SVG) node).isTopLevel()
                ? EstablishRootMeasure.YES
                : EstablishRootMeasure.NO;
        return derive(context, attributeFontSpec, null, frc, contextAttributes,
                establishRootMeasure);
    }

    public @NotNull AffineTransform rootTransform() {
        return rootTransform;
    }

    public @NotNull AffineTransform userSpaceTransform() {
        return userSpaceTransform;
    }

    public void translate(@NotNull Output output, @NotNull Point2D dp) {
        translate(output, dp.getX(), dp.getY());
    }

    public void translate(@NotNull Output output, double dx, double dy) {
        output.translate(dx, dy);
        userSpaceTransform.translate(dx, dy);
    }

    public void scale(@NotNull Output output, double sx, double sy) {
        output.scale(sx, sy);
        userSpaceTransform.scale(sx, sy);
    }

    public void rotate(@NotNull Output output, double angle) {
        output.rotate(angle);
        userSpaceTransform.rotate(angle);
    }

    public void transform(@NotNull Output output, @NotNull AffineTransform at) {
        output.applyTransform(at);
        userSpaceTransform.concatenate(at);
    }

    public @NotNull PlatformSupport platformSupport() {
        return platformSupport;
    }

    public @NotNull MeasureContext measureContext() {
        return measureContext;
    }

    public @NotNull SVGPaint strokePaint() {
        return PaintResolver.resolvePaint(paintContext.strokePaint, paintContext, contextElementAttributes);
    }

    public @NotNull SVGPaint fillPaint() {
        return PaintResolver.resolvePaint(paintContext.fillPaint, paintContext, contextElementAttributes);
    }

    public float rawOpacity() {
        return paintContext.opacity.get(measureContext);
    }

    public float fillOpacity() {
        assert paintContext.fillOpacity != null;
        return paintContext.fillOpacity.get(measureContext) * paintContext.opacity.get(measureContext);
    }

    public float strokeOpacity() {
        assert paintContext.strokeOpacity != null;
        return paintContext.strokeOpacity.get(measureContext) * paintContext.opacity.get(measureContext);
    }

    public @NotNull Stroke stroke(float pathLengthFactor) {
        return StrokeResolver.resolve(pathLengthFactor, measureContext, strokeContext());
    }

    // Internal accessor methods

    private @NotNull StrokeContext strokeContext() {
        // This will never be null for a RenderContext.
        // Our deriving mechanism together with non-null initial values prohibits this.
        assert paintContext.strokeContext != null;
        return paintContext.strokeContext;
    }

    private @NotNull FontRenderContext fontRenderContext() {
        return fontRenderContext;
    }

    private @Nullable ContextElementAttributes contextElementAttributes() {
        return contextElementAttributes;
    }

    private @NotNull FillRule fillRule() {
        FillRule fillRule = paintContext.fillRule;
        return fillRule != null ? fillRule : FillRule.Nonzero;
    }

    private @NotNull PaintOrder paintOrder() {
        PaintOrder paintOrder = paintContext.paintOrder;
        return paintOrder != null ? paintOrder : PaintOrder.NORMAL;
    }

    private @NotNull SVGFont font() {
        return FontResolver.resolve(this.fontSpec, this.measureContext, platformSupport.fontFamily());
    }

    private void setRootTransform(@NotNull AffineTransform rootTransform) {
        this.rootTransform.setTransform(rootTransform);
        this.userSpaceTransform.setToIdentity();
    }

    private void setRootTransform(@NotNull AffineTransform rootTransform, @NotNull AffineTransform userSpaceTransform) {
        this.rootTransform.setTransform(rootTransform);
        this.userSpaceTransform.setTransform(userSpaceTransform);
    }

    @Override
    public String toString() {
        return "RenderContext{" +
                "platformSupport=" + platformSupport +
                ", measureContext=" + measureContext +
                ", paintContext=" + paintContext +
                ", fontRenderContext=" + fontRenderContext +
                ", fontSpec=" + fontSpec +
                ", contextElementAttributes=" + contextElementAttributes +
                ", rootTransform=" + rootTransform +
                ", userSpaceTransform=" + userSpaceTransform +
                '}';
    }
}
