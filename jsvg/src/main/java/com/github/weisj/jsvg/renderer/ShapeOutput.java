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
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ShapeOutput implements Output {

    private final @NotNull Path2D accumulatorShape;
    private @NotNull AffineTransform currentTransform;
    private @NotNull Stroke currentStroke;
    private @Nullable Area currentClip;

    public ShapeOutput(@NotNull Path2D shape) {
        accumulatorShape = shape;
        currentStroke = new BasicStroke();
        currentTransform = new AffineTransform();
        currentClip = null;
    }

    private ShapeOutput(@NotNull ShapeOutput parent) {
        accumulatorShape = parent.accumulatorShape;
        currentStroke = parent.currentStroke;
        currentTransform = new AffineTransform(parent.currentTransform);
        currentClip = parent.currentClip != null ? new Area(parent.currentClip) : null;
    }

    private @NotNull Shape transformShape(@NotNull Shape shape, @NotNull AffineTransform transform) {
        if (transform.isIdentity())
            return shape;
        return transform.createTransformedShape(shape);
    }

    private void append(@NotNull Shape shape, @NotNull AffineTransform transform) {
        AffineTransform at = new AffineTransform(currentTransform);
        at.concatenate(transform);
        accumulatorShape.append(transformShape(shape, at), false);
    }

    private void append(@NotNull Shape shape) {
        accumulatorShape.append(transformShape(shape, currentTransform), false);
    }

    @Override
    public void fillShape(@NotNull Shape shape) {
        append(shape);
    }

    @Override
    public void drawShape(@NotNull Shape shape) {
        append(currentStroke.createStrokedShape(shape));
    }

    @Override
    public void drawImage(@NotNull BufferedImage image) {
        append(new Rectangle2D.Float(0, 0, image.getWidth(), image.getHeight()));
    }

    @Override
    public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
        append(new Rectangle2D.Float(0, 0, image.getWidth(null), image.getHeight(null)));
    }

    @Override
    public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
        append(new Rectangle2D.Float(0, 0, image.getWidth(null), image.getHeight(null)), at);
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        // DO NOTHING
    }

    @Override
    public void setStroke(@NotNull Stroke stroke) {
        currentStroke = stroke;
    }

    @Override
    public @NotNull Stroke stroke() {
        return currentStroke;
    }

    @Override
    public void applyClip(@NotNull Shape clipShape) {
        if (currentClip == null) {
            currentClip = new Area(clipShape);
        } else {
            currentClip.intersect(new Area(clipShape));
        }
    }

    @Override
    public void setClip(@Nullable Shape shape) {
        currentClip = shape != null ? new Area(shape) : null;
    }

    @Override
    public @Nullable Shape clip() {
        return currentClip;
    }

    @Override
    public Optional<Float> contextFontSize() {
        return Optional.empty();
    }

    @Override
    public @NotNull Output createChild() {
        return new ShapeOutput(this);
    }

    @Override
    public void dispose() {
        // No action needed
    }

    @Override
    public void debugPaint(@NotNull Consumer<Graphics2D> painter) {
        // Not supported. Do nothing
    }

    @Override
    public @NotNull Rectangle2D clipBounds() {
        float veryLargeNumber = Float.MAX_VALUE / 4;
        return currentClip != null ? currentClip.getBounds2D()
                : new Rectangle2D.Float(-veryLargeNumber, -veryLargeNumber, 2 * veryLargeNumber, 2 * veryLargeNumber);
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
        // Not supported. Do nothing
    }

    @Override
    public @NotNull AffineTransform transform() {
        return new AffineTransform(currentTransform);
    }

    @Override
    public void setTransform(@NotNull AffineTransform affineTransform) {
        currentTransform = new AffineTransform(affineTransform);
    }

    @Override
    public void applyTransform(@NotNull AffineTransform transform) {
        currentTransform.concatenate(transform);
    }

    @Override
    public void rotate(double angle) {
        currentTransform.rotate(angle);
    }

    @Override
    public void scale(double sx, double sy) {
        currentTransform.scale(sx, sy);
    }

    @Override
    public void translate(double dx, double dy) {
        currentTransform.translate(dx, dy);
    }

    @Override
    public void applyOpacity(float opacity) {
        // Not supported. Do nothing
    }

    @Override
    public @NotNull SafeState safeState() {
        return new ShapeOutputSafeState(this);
    }

    private static class ShapeOutputSafeState implements SafeState {
        private final @NotNull ShapeOutput shapeOutput;
        private final @NotNull Stroke oldStroke;
        private final @NotNull AffineTransform oldTransform;
        private final @Nullable Area oldClip;

        private ShapeOutputSafeState(@NotNull ShapeOutput shapeOutput) {
            this.shapeOutput = shapeOutput;
            this.oldStroke = shapeOutput.stroke();
            this.oldTransform = shapeOutput.transform();
            this.oldClip = shapeOutput.currentClip != null ? new Area(shapeOutput.currentClip) : null;
        }

        @Override
        public void restore() {
            shapeOutput.currentStroke = oldStroke;
            shapeOutput.currentTransform = oldTransform;
            shapeOutput.currentClip = oldClip;
        }
    }
}
