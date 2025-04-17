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
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.util.ShapeUtil;


public class ShapeOutput implements Output {

    private final @NotNull Area accumulatorShape;
    private @NotNull AffineTransform currentTransform;
    private @NotNull Stroke currentStroke;
    private @Nullable Shape currentClip;

    public ShapeOutput(@NotNull Area area) {
        accumulatorShape = area;
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

    private void addShape(@NotNull Shape shape) {
        // NOTE: ShapeUtil.transformShape always returns a new shape hence we can safely modify shape.
        Shape s = currentClip != null
                ? ShapeUtil.intersect(currentClip, shape, true, false)
                : shape;
        accumulatorShape.add(new Area(s));
    }

    private void append(@NotNull Shape shape, @NotNull AffineTransform transform) {
        AffineTransform at = new AffineTransform(currentTransform);
        at.concatenate(transform);
        addShape(ShapeUtil.transformShape(shape, at));
    }

    private void append(@NotNull Shape shape) {
        addShape(ShapeUtil.transformShape(shape, currentTransform));
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
        // Not supported. Do nothing
    }

    @Override
    public void setPaint(@NotNull Supplier<Paint> paintProvider) {
        // Not supported. Do nothing
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
        Shape transformedShape = ShapeUtil.transformShape(clipShape, currentTransform);
        if (currentClip != null) {
            currentClip = ShapeUtil.intersect(currentClip, transformedShape, true, false);
        } else {
            currentClip = transformedShape;
        }
    }

    @Override
    public void setClip(@Nullable Shape shape) {
        currentClip = shape != null
                ? ShapeUtil.transformShape(shape, currentTransform)
                : null;
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
    public float currentOpacity() {
        return 1;
    }

    @Override
    public void applyOpacity(float opacity) {
        // Not supported. Do nothing
    }

    @Override
    public @NotNull SafeState safeState() {
        return new ShapeOutputSafeState(this);
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
    public boolean isSoftClippingEnabled() {
        // Not needed here. Always return false
        return false;
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
