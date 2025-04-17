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
package com.github.weisj.jsvg.renderer.impl.output;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.paint.impl.MaskedPaint;
import com.github.weisj.jsvg.renderer.Output;

public class Graphics2DOutput implements Output {
    private final Graphics2D g;

    public @NotNull Graphics2D graphics() {
        return g;
    }

    public Graphics2DOutput(@NotNull Graphics2D g) {
        this.g = g;
        GraphicsUtil.preparePaint(g.getPaint());
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
        GraphicsUtil.safelyDrawImage(this, g, image, observer);
    }

    @Override
    public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
        g.drawImage(image, at, observer);
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        GraphicsUtil.safelySetPaint(this, g, paint);
    }

    @Override
    public void setPaint(@NotNull Supplier<Paint> paintProvider) {
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
        GraphicsUtil.cleanupPaint(this, g.getPaint());
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
    public float currentOpacity() {
        Composite composite = g.getComposite();
        if (composite instanceof AlphaComposite) {
            return ((AlphaComposite) composite).getAlpha();
        }
        return 1;
    }

    @Override
    public void applyOpacity(float opacity) {
        if (GeometryUtil.approximatelyEqual(opacity, 1)) return;
        g.setComposite(GraphicsUtil.deriveComposite(g, opacity));
    }

    @Override
    public boolean hasMaskedPaint() {
        return g.getPaint() instanceof MaskedPaint;
    }

    @Override
    public @NotNull SafeState safeState() {
        return new GraphicsResetHelper(g);
    }

    @Override
    public boolean supportsFilters() {
        return true;
    }

    @Override
    public boolean supportsColors() {
        return true;
    }
}
