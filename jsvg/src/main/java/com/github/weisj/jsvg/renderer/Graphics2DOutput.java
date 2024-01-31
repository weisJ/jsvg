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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.util.GraphicsResetHelper;
import com.github.weisj.jsvg.util.Provider;

public class Graphics2DOutput implements Output {
    private final Graphics2D g;

    public Graphics2DOutput(@NotNull Graphics2D g) {
        this.g = g;
    }

    @Override
    public void fillShape(@NotNull Shape shape) {
        g.fill(shape);
    }

    @Override
    public void drawShape(@NotNull Shape shape) {
        g.draw(shape);
    }

    @Override
    public void drawImage(@NotNull BufferedImage image) {
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null, null);
    }

    @Override
    public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
        g.drawImage(image, 0, 0, null);
    }

    @Override
    public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
        g.drawImage(image, at, observer);
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        GraphicsUtil.safelySetPaint(g, paint);
    }

    @Override
    public void setPaint(@NotNull Provider<Paint> paintProvider) {
        setPaint(paintProvider.get());
    }

    @Override
    public void setStroke(@NotNull Stroke stroke) {
        g.setStroke(stroke);
    }

    @Override
    public @NotNull Stroke stroke() {
        return g.getStroke();
    }

    @Override
    public void applyClip(@NotNull Shape clipShape) {
        g.clip(clipShape);
    }

    @Override
    public void setClip(@Nullable Shape shape) {
        g.setClip(shape);
    }

    @Override
    public @NotNull Shape clip() {
        return g.getClip();
    }

    @Override
    public Optional<Float> contextFontSize() {
        Font f = g.getFont();
        if (f != null) return Optional.of(f.getSize2D());
        return Optional.empty();
    }

    @Override
    public @NotNull Output createChild() {
        return new Graphics2DOutput((Graphics2D) g.create());
    }

    @Override
    public void dispose() {
        g.dispose();
    }

    @Override
    public void debugPaint(@NotNull Consumer<Graphics2D> painter) {
        Graphics2D debugGraphics = (Graphics2D) g.create();
        painter.accept(debugGraphics);
        debugGraphics.dispose();
    }

    @Override
    public @NotNull Rectangle2D clipBounds() {
        return g.getClipBounds();
    }

    @Override
    public @NotNull RenderingHints renderingHints() {
        return g.getRenderingHints();
    }

    @Override
    public @Nullable Object renderingHint(RenderingHints.@NotNull Key key) {
        return g.getRenderingHint(key);
    }

    @Override
    public void setRenderingHint(RenderingHints.@NotNull Key key, @Nullable Object value) {
        g.setRenderingHint(key, value);
    }

    @Override
    public @NotNull AffineTransform transform() {
        return g.getTransform();
    }

    @Override
    public void setTransform(@NotNull AffineTransform affineTransform) {
        g.setTransform(affineTransform);
    }

    @Override
    public void applyTransform(@NotNull AffineTransform transform) {
        g.transform(transform);
    }

    @Override
    public void rotate(double angle) {
        g.rotate(angle);
    }

    @Override
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }

    @Override
    public void translate(double dx, double dy) {
        g.translate(dx, dy);
    }

    @Override
    public void applyOpacity(float opacity) {
        g.setComposite(GraphicsUtil.deriveComposite(g, opacity));
    }

    @Override
    public @NotNull SafeState safeState() {
        return new GraphicsResetHelper(g);
    }

    @Override
    public boolean supportsFilters() {
        return true;
    }
}
