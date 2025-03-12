/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.impl;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.util.Provider;

public class NullOutput implements Output, Output.SafeState {
    @Override
    public void fillShape(@NotNull Shape shape) {
        /* do nothing */
    }

    @Override
    public void drawShape(@NotNull Shape shape) {
        /* do nothing */
    }

    @Override
    public void drawImage(@NotNull BufferedImage image) {
        /* do nothing */
    }

    @Override
    public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
        /* do nothing */
    }

    @Override
    public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
        /* do nothing */
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        /* do nothing */
    }

    @Override
    public void setPaint(@NotNull Provider<Paint> paintProvider) {
        /* do nothing */
    }

    @Override
    public void setStroke(@NotNull Stroke stroke) {
        /* do nothing */
    }

    @Override
    public @NotNull Stroke stroke() {
        return new BasicStroke();
    }

    @Override
    public void applyClip(@NotNull Shape clipShape) {
        /* do nothing */
    }

    @Override
    public void setClip(@Nullable Shape shape) {
        /* do nothing */
    }

    @Override
    public Optional<Float> contextFontSize() {
        return Optional.empty();
    }

    @Override
    public @NotNull Output createChild() {
        return this;
    }

    @Override
    public void dispose() {
        /* do nothing */
    }

    @Override
    public void debugPaint(@NotNull Consumer<Graphics2D> painter) {
        /* do nothing */
    }

    @Override
    public @NotNull Rectangle2D clipBounds() {
        return new Rectangle2D.Double();
    }

    @Override
    public @Nullable RenderingHints renderingHints() {
        return null;
    }

    @Override
    public @Nullable Object renderingHint(RenderingHints.@NotNull Key key) {
        return null;
    }

    @Override
    public void setRenderingHint(RenderingHints.@NotNull Key key, @Nullable Object value) {
        /* do nothing */
    }

    @Override
    public @NotNull AffineTransform transform() {
        return new AffineTransform();
    }

    @Override
    public void setTransform(@NotNull AffineTransform affineTransform) {
        /* do nothing */
    }

    @Override
    public void applyTransform(@NotNull AffineTransform transform) {
        /* do nothing */
    }

    @Override
    public void rotate(double angle) {
        /* do nothing */
    }

    @Override
    public void scale(double sx, double sy) {
        /* do nothing */
    }

    @Override
    public void translate(double dx, double dy) {
        /* do nothing */
    }

    @Override
    public float currentOpacity() {
        return 1;
    }

    @Override
    public void applyOpacity(float opacity) {
        /* do nothing */
    }

    @Override
    public @NotNull SafeState safeState() {
        return this;
    }

    @Override
    public boolean supportsFilters() {
        return false;
    }

    @Override
    public boolean supportsColors() {
        return false;
    }


    @Override
    public void restore() {
        /* do nothing */
    }
}
