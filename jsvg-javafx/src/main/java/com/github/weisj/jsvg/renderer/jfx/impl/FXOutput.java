/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.impl;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.paint.impl.MaskedPaint;
import com.github.weisj.jsvg.renderer.jfx.impl.bridge.*;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import com.github.weisj.jsvg.util.ImageUtil;

/**
 * An {@link Output} implementation that uses a {@link GraphicsContext} to draw to.
 */
public final class FXOutput implements Output {

    private final GraphicsContext ctx;
    private final RenderingHints renderingHints;

    private static final Color DEFAULT_PAINT = Color.BLACK;
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
    private static final float DEFAULT_OPACITY = 1F;

    private float currentOpacity = DEFAULT_OPACITY;
    private Paint currentPaint = DEFAULT_PAINT;
    private Stroke currentStroke = DEFAULT_STROKE;
    private final SafeState originalState;

    private FXOutput(@NotNull GraphicsContext context) {
        ctx = context;
        renderingHints = new RenderingHints(null);
        setOpacity(DEFAULT_OPACITY);
        setPaint(DEFAULT_PAINT);
        setStroke(DEFAULT_STROKE);
        originalState = new FXOutputState(this, SaveClipStack.YES);
    }

    private FXOutput(@NotNull FXOutput parent) {
        ctx = parent.ctx;
        renderingHints = new RenderingHints(null);
        renderingHints.putAll(parent.renderingHints);
        currentOpacity = parent.currentOpacity;
        currentPaint = parent.currentPaint;
        currentStroke = parent.currentStroke;
        originalState = new FXOutputState(this, SaveClipStack.YES);
    }

    /**
     * Example usage:
     * <pre><code>
     *     Output output = FXOutput.createForGraphicsContext(graphics);
     *     svgDocument.renderWithPlatform(NullPlatformSupport.INSTANCE, output, null, null);
     *     output.dispose();
     * </code></pre>
     */
    public static @NotNull FXOutput createForGraphicsContext(@NotNull GraphicsContext context) {
        FXOutput output = new FXOutput(context);
        FXRenderingHintsUtil.setupDefaultJFXRenderingHints(output);
        return output;
    }

    @Override
    public void fillShape(@NotNull Shape shape) {
        if (FXPaintBridge.supportedPaint(currentPaint)) {
            FXShapeBridge.fillShape(ctx, shape);
        } else {
            // Render incompatible / custom paints with PaintContext fallback
            Rectangle2D userBounds = GeometryUtil.containingBoundsAfterTransform(transform(), shape.getBounds());
            Rectangle deviceBounds = userBounds.getBounds();
            PaintContext context = currentPaint.createContext(ColorModel.getRGBdefault(), deviceBounds, userBounds,
                    transform(), renderingHints);
            Raster raster = context.getRaster(deviceBounds.x, deviceBounds.y, deviceBounds.width, deviceBounds.height);
            BufferedImage image = FXImageBridge.convertRasterToBufferedImage(context.getColorModel(), raster);
            ctx.save();
            applyClip(shape);
            Affine transform = ctx.getTransform();
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.drawImage(FXImageBridge.convertImage(image), userBounds.getX(), userBounds.getY(),
                    userBounds.getWidth(),
                    userBounds.getHeight());
            ctx.setTransform(transform);
            ctx.restore();
        }
    }

    @Override
    public void drawShape(@NotNull Shape shape) {
        if (FXPaintBridge.supportedPaint(currentPaint)) {
            FXShapeBridge.strokeShape(ctx, shape);
        } else {
            fillShape(stroke().createStrokedShape(shape));
        }
    }

    @Override
    public void drawImage(@NotNull BufferedImage image) {
        FXImageBridge.drawImage(ctx, image, currentOpacity);
    }

    @Override
    public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
        if (!FXPaintBridge.isWrappingPaint(currentPaint)) {
            FXImageBridge.drawImage(ctx, image, currentOpacity);
        } else {
            // Handle rendering of wrapping paints
            GraphicsUtil.WrappingPaint wrappingPaint = (GraphicsUtil.WrappingPaint) currentPaint;
            Paint inner = wrappingPaint.innerPaint();

            Rectangle r = new Rectangle(0, 0, image.getWidth(observer), image.getHeight(observer));
            BufferedImage img = image instanceof BufferedImage
                    ? (BufferedImage) image
                    : ImageUtil.toBufferedImage(image);
            TexturePaint texturePaint = new TexturePaint(img, r);

            wrappingPaint.setPaint(GraphicsUtil.exchangePaint(this, wrappingPaint.paint(), texturePaint, false));
            fillShape(r);
            wrappingPaint.setPaint(GraphicsUtil.exchangePaint(this, texturePaint, inner, false));
        }
    }


    @Override
    public void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer) {
        Affine originalTransform = ctx.getTransform();
        ctx.transform(at.getScaleX(), at.getShearY(), at.getShearX(), at.getScaleY(), at.getTranslateX(),
                at.getTranslateY());
        FXImageBridge.drawImage(ctx, image, currentOpacity);
        ctx.setTransform(originalTransform);
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        paint = GraphicsUtil.exchangePaint(this, currentPaint, paint, true);
        FXPaintBridge.applyPaint(ctx, paint, currentOpacity);
        currentPaint = paint;
    }

    @Override
    public void setPaint(@NotNull Supplier<Paint> paintProvider) {
        setPaint(paintProvider.get());
    }

    @Override
    public void setStroke(@NotNull Stroke stroke) {
        FXStrokeBridge.applyStroke(ctx, stroke);
        currentStroke = stroke;
    }

    @Override
    public @NotNull Stroke stroke() {
        return currentStroke;
    }

    @Override
    public void applyClip(@NotNull Shape clipShape) {
        PathIterator awtIterator = clipShape.getPathIterator(null);
        FXShapeBridge.appendPathIterator(ctx, awtIterator);
        FXShapeBridge.applyWindingRule(ctx, awtIterator.getWindingRule());
        ctx.clip();
    }

    @Override
    public Optional<Float> contextFontSize() {
        // TODO check this actually returns what we're after
        return Optional.of((float) ctx.getFont().getSize());
    }

    @Override
    public @NotNull Output createChild() {
        return new FXOutput(this);
    }

    @Override
    public void dispose() {
        GraphicsUtil.cleanupPaint(this, currentPaint);
        originalState.restore();
    }

    @Override
    public void debugPaint(@NotNull Consumer<Graphics2D> painter) {
        int width = (int) ctx.getCanvas().getWidth();
        int height = (int) ctx.getCanvas().getHeight();

        BufferedImage debugImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D debugGraphics = debugImage.createGraphics();
        debugGraphics.setRenderingHints(renderingHints);
        debugGraphics.setPaint(currentPaint);
        debugGraphics.setTransform(transform());
        debugGraphics.setStroke(stroke());
        debugGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentOpacity));
        painter.accept(debugGraphics);
        debugGraphics.dispose();

        Affine originalTransform = ctx.getTransform();
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        drawImage(debugImage);
        ctx.setTransform(originalTransform);
    }

    private Rectangle2D canvasBounds() {
        return new Rectangle2D.Double(0, 0, ctx.getCanvas().getWidth(), ctx.getCanvas().getHeight());
    }

    @Override
    public @NotNull Rectangle2D clipBounds() {
        Rectangle2D bounds = canvasBounds();// clipStack.getClipBounds();
        return GeometryUtil.createInverse(transform()).createTransformedShape(bounds).getBounds2D();
    }

    @Override
    public @Nullable RenderingHints renderingHints() {
        return renderingHints;
    }

    @Override
    public @Nullable Object renderingHint(RenderingHints.@NotNull Key key) {
        return renderingHints.get(key);
    }

    @Override
    public void setRenderingHint(RenderingHints.@NotNull Key key, @Nullable Object value) {
        renderingHints.put(key, value);
    }

    @Override
    public @NotNull AffineTransform transform() {
        return FXTransformBridge.convertAffine(ctx.getTransform());
    }

    @Override
    public void setTransform(@NotNull AffineTransform awtTransform) {
        FXTransformBridge.setTransform(ctx, awtTransform);
    }

    @Override
    public void applyTransform(@NotNull AffineTransform awtTransform) {
        FXTransformBridge.applyTransform(ctx, awtTransform);
    }

    @Override
    public void rotate(double angle) {
        ctx.rotate(Math.toDegrees(angle));
    }

    @Override
    public void scale(double sx, double sy) {
        ctx.scale(sx, sy);
    }

    @Override
    public void translate(double dx, double dy) {
        ctx.translate(dx, dy);
    }

    @Override
    public float currentOpacity() {
        return currentOpacity;
    }

    @Override
    public void applyOpacity(float opacity) {
        if (GeometryUtil.approximatelyEqual(opacity, 1)) return;
        setOpacity(opacity * currentOpacity);
    }

    void setOpacity(float opacity) {
        currentOpacity = opacity;

        // Re-apply paint with correct opacity
        FXPaintBridge.applyPaint(ctx, currentPaint, currentOpacity);
    }

    @Override
    public @NotNull SafeState safeState() {
        return new FXOutputState(this, SaveClipStack.YES);
    }

    @Override
    public boolean supportsFilters() {
        return true;
    }

    @Override
    public boolean supportsColors() {
        return true;
    }

    @Override
    public boolean isSoftClippingEnabled() {
        // JavaFX performs soft clips by default
        return false;
    }

    @Override
    public boolean hasMaskedPaint() {
        return currentPaint instanceof MaskedPaint;
    }

    private enum SaveClipStack {
        YES,
        NO
    }

    private static final class FXOutputState implements Output.SafeState {

        private final FXOutput fxOutput;
        private final AffineTransform originalTransform;
        private final Paint originalPaint;
        private final Stroke originalStroke;
        private final float originalOpacity;
        private final SaveClipStack saveClip;

        FXOutputState(@NotNull FXOutput fxOutput, SaveClipStack saveClip) {
            this.fxOutput = fxOutput;
            this.originalTransform = fxOutput.transform();
            this.originalPaint = fxOutput.currentPaint;
            this.originalStroke = fxOutput.currentStroke;
            this.originalOpacity = fxOutput.currentOpacity;
            this.saveClip = saveClip;
            if (saveClip == SaveClipStack.YES) {
                fxOutput.ctx.save();
            }
        }

        public @NotNull GraphicsContext context() {
            return fxOutput.ctx;
        }

        @Override
        public void restore() {
            if (saveClip == SaveClipStack.YES) {
                fxOutput.ctx.restore();
            }
            fxOutput.setOpacity(originalOpacity);
            fxOutput.setTransform(originalTransform);
            fxOutput.setPaint(originalPaint);
            fxOutput.setStroke(originalStroke);
        }
    }
}
