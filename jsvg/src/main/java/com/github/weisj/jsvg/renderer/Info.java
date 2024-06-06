/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;

class Info implements AutoCloseable {
    protected final @NotNull RenderContext context;
    protected final @NotNull Output output;
    private final @NotNull Renderable renderable;

    Info(@NotNull Renderable renderable, @NotNull RenderContext context, @NotNull Output output) {
        this.renderable = renderable;
        this.context = context;
        this.output = output;
    }

    public @NotNull Renderable renderable() {
        return renderable;
    }

    public @NotNull Output output() {
        return output;
    }

    public @NotNull RenderContext context() {
        return context;
    }

    @Override
    public void close() {
        output.dispose();
    }

    static final class InfoWithIsolation extends Info {

        private final @NotNull BlittableImage blittableImage;
        private final @NotNull Output imageOutput;
        private final @NotNull ElementBounds elementBounds;
        private final @NotNull IsolationEffects isolationEffects;
        private final @Nullable Filter.FilterInfo filterInfo;

        static @Nullable InfoWithIsolation create(@NotNull Renderable renderable,
                @NotNull RenderContext context, @NotNull Output output,
                @NotNull ElementBounds elementBounds, @NotNull IsolationEffects effects) {

            Rectangle2D clipBounds = null;
            Rectangle2D bounds = null;
            Filter.FilterBounds filterBounds = null;
            Filter.FilterInfo filterInfo = null;

            Filter filter = effects.filter;
            Mask mask = effects.mask;
            ClipPath clipPath = effects.clipPath;

            if (filter != null) {
                filterBounds = filter.createFilterBounds(output, context, elementBounds);
                if (filterBounds != null) {
                    bounds = filterBounds.filterRegion();
                    clipBounds = filterBounds.effectiveFilterArea();
                }
            }
            if (mask != null || clipPath != null) {
                bounds = elementBounds.geometryBox();
            }

            if (bounds == null) {
                return null;
            }

            RenderContext imageContext = context.deriveForSurface();

            BlittableImage blitImage = BlittableImage.create(
                    ImageUtil::createCompatibleTransparentImage, context, clipBounds,
                    bounds, elementBounds.boundingBox(), UnitType.UserSpaceOnUse, imageContext);
            if (blitImage == null) return null;

            Graphics2D g = blitImage.createGraphics();
            g.setRenderingHints(output.renderingHints());
            Output imageOutput = new Graphics2DOutput(g);

            if (filter != null && filterBounds != null) {
                filterInfo = new Filter.FilterInfo(blitImage, imageOutput, filterBounds);
            }

            return new InfoWithIsolation(
                    renderable, context, output, imageOutput, blitImage, elementBounds, effects, filterInfo);
        }

        private InfoWithIsolation(@NotNull Renderable renderable, @NotNull RenderContext context,
                @NotNull Output output, @NotNull Output imageOutput,
                @NotNull BlittableImage blittableImage,
                @NotNull ElementBounds elementBounds,
                @NotNull IsolationEffects isolationEffects, @Nullable Filter.FilterInfo filterInfo) {
            super(renderable, context, output);
            this.blittableImage = blittableImage;
            this.imageOutput = imageOutput;
            this.elementBounds = elementBounds;
            this.isolationEffects = isolationEffects;
            this.filterInfo = filterInfo;
        }

        @Override
        public @NotNull Output output() {
            return imageOutput;
        }

        @Override
        public @NotNull RenderContext context() {
            return blittableImage.context();
        }

        @Override
        public void close() {
            Output previousOutput = this.output;
            BufferedImage result = this.blittableImage.image();

            if (isolationEffects.filter != null) {
                assert filterInfo != null;
                result = isolationEffects.filter.applyFilter(previousOutput, context, filterInfo);
                previousOutput.applyClip(filterInfo.filterRegion());
            }
            if (isolationEffects.mask != null) {
                previousOutput.setPaint(() -> isolationEffects.mask.createMaskPaint(
                        previousOutput, context, elementBounds));
            }
            if (isolationEffects.clipPath != null) {
                isolationEffects.clipPath.applyClip(previousOutput, context, elementBounds);
            }

            blittableImage.prepareForBlitting(previousOutput);
            previousOutput.drawImage(result, context.platformSupport().imageObserver());
            imageOutput.dispose();
            super.close();
        }
    }
}
