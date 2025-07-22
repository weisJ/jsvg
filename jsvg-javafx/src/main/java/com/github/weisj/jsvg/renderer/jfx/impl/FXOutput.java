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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.FillRule;
import javafx.scene.transform.Affine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.paint.impl.MaskedPaint;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import com.github.weisj.jsvg.util.ImageUtil;

/**
 * An {@link Output} implementation that uses a {@link GraphicsContext} to draw to.
 */
public class FXOutput implements Output {

    private final GraphicsContext ctx;
    private final GraphicsContextSaveCounter ctxSaveCounter;
    private final ClipStack clipStack;
    private final RenderingHints renderingHints;
    private final boolean isRootOutput;

    private static final Color DEFAULT_PAINT = Color.BLACK;
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
    private static final float DEFAULT_OPACITY = 1F;
    private static final FillRule DEFAULT_FILL_RULE = FillRule.NON_ZERO;

    private float currentOpacity = DEFAULT_OPACITY;
    private Paint currentPaint = DEFAULT_PAINT;
    private Stroke currentStroke = DEFAULT_STROKE;
    private final SafeState originalState;

    private FXOutput(@NotNull GraphicsContext context) {
        ctx = context;
        ctxSaveCounter = new GraphicsContextSaveCounter();
        clipStack = new ClipStack();
        isRootOutput = true;
        renderingHints = new RenderingHints(null);
        setOpacity(DEFAULT_OPACITY);
        setPaint(DEFAULT_PAINT);
        setStroke(DEFAULT_STROKE);
        originalState = new FXOutputState(SaveClipStack.YES);
    }

    private FXOutput(@NotNull FXOutput parent) {
        ctx = parent.ctx;
        ctxSaveCounter = parent.ctxSaveCounter;
        clipStack = parent.clipStack;
        renderingHints = new RenderingHints(null);
        renderingHints.putAll(parent.renderingHints);
        isRootOutput = false;
        currentOpacity = parent.currentOpacity;
        currentPaint = parent.currentPaint;
        currentStroke = parent.currentStroke;
        originalState = new FXOutputState(SaveClipStack.YES);
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
        setupDefaultJFXRenderingHints(output);
        return output;
    }

    // JFX defaults to the highest render quality
    public static void setupDefaultJFXRenderingHints(Output output) {
        output.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        output.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        output.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        output.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
        output.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING,
                SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF);
        output.setRenderingHint(SVGRenderingHints.KEY_MASK_CLIP_RENDERING,
                SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_ACCURACY);
    }

    @Override
    public void fillShape(@NotNull Shape shape) {
        if (FXAWTBridge.supportedPaint(currentPaint)) {
            FXAWTBridge.fillShape(ctx, shape);
        } else {
            // Render incompatible / custom paints with PaintContext fallback
            Rectangle2D userBounds = GeometryUtil.containingBoundsAfterTransform(transform(), shape.getBounds());
            Rectangle deviceBounds = userBounds.getBounds();
            PaintContext context = currentPaint.createContext(ColorModel.getRGBdefault(), deviceBounds, userBounds,
                    transform(), renderingHints);
            Raster raster = context.getRaster(deviceBounds.x, deviceBounds.y, deviceBounds.width, deviceBounds.height);
            BufferedImage image = FXAWTBridge.convertRasterToBufferedImage(context.getColorModel(), raster);
            clipStack.pushClip(shape);
            Affine transform = ctx.getTransform();
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.drawImage(FXAWTBridge.convertImage(image), userBounds.getX(), userBounds.getY(), userBounds.getWidth(),
                    userBounds.getHeight());
            ctx.setTransform(transform);
            clipStack.popClip();
        }
    }

    @Override
    public void drawShape(@NotNull Shape shape) {
        if (FXAWTBridge.supportedPaint(currentPaint)) {
            FXAWTBridge.drawShape(ctx, shape);
        } else {
            fillShape(stroke().createStrokedShape(shape));
        }
    }

    @Override
    public void drawImage(@NotNull BufferedImage image) {
        FXAWTBridge.drawImage(ctx, image, currentOpacity);
    }

    @Override
    public void drawImage(@NotNull Image image, @Nullable ImageObserver observer) {
        if (!FXAWTBridge.isWrappingPaint(currentPaint)) {
            FXAWTBridge.drawImage(ctx, image, currentOpacity);
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
        FXAWTBridge.drawImage(ctx, image, currentOpacity);
        ctx.setTransform(originalTransform);
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        paint = GraphicsUtil.exchangePaint(this, currentPaint, paint, true);
        FXAWTBridge.applyPaint(ctx, paint, currentOpacity);
        currentPaint = paint;
    }

    @Override
    public void setPaint(@NotNull Supplier<Paint> paintProvider) {
        setPaint(paintProvider.get());
    }

    @Override
    public void setStroke(@NotNull Stroke stroke) {
        FXAWTBridge.applyStroke(ctx, stroke);
        currentStroke = stroke;
    }

    @Override
    public @NotNull Stroke stroke() {
        return currentStroke;
    }

    @Override
    public void applyClip(@NotNull Shape clipShape) {
        clipStack.pushClip(clipShape);
    }

    @Override
    public void setClip(@Nullable Shape shape) {
        clipStack.clearClip();
        if (shape != null) {
            clipStack.pushClip(shape);
        }
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

    public Rectangle2D canvasBounds() {
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
        return FXAWTBridge.convertAffine(ctx.getTransform());
    }

    @Override
    public void setTransform(@NotNull AffineTransform awtTransform) {
        FXAWTBridge.setTransform(ctx, awtTransform);
    }

    @Override
    public void applyTransform(@NotNull AffineTransform awtTransform) {
        FXAWTBridge.applyTransform(ctx, awtTransform);
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

    private void setOpacity(float opacity) {
        currentOpacity = opacity;

        // Re-apply paint with correct opacity
        FXAWTBridge.applyPaint(ctx, currentPaint, currentOpacity);
    }

    @Override
    public @NotNull SafeState safeState() {
        return new FXOutputState(SaveClipStack.YES);
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

    private class FXOutputState implements SafeState {

        private final AffineTransform originalTransform;
        private final Paint originalPaint;
        private final Stroke originalStroke;
        private final float originalOpacity;
        private final List<Shape> originalClipStack;

        public FXOutputState(SaveClipStack saveClip) {
            this.originalTransform = transform();
            this.originalPaint = currentPaint;
            this.originalStroke = currentStroke;
            this.originalOpacity = currentOpacity;
            this.originalClipStack = saveClip == SaveClipStack.YES ? clipStack.snapshot() : null;
        }

        public @NotNull GraphicsContext context() {
            return ctx;
        }

        @Override
        public void restore() {
            if (originalClipStack != null) {
                clipStack.restoreClipStack(originalClipStack);
            }
            setOpacity(originalOpacity);
            setTransform(originalTransform);
            setPaint(originalPaint);
            setStroke(originalStroke);
        }
    }

    private static class ClipShape {

        private final Shape shape;
        private Rectangle2D bounds;
        private final int savePoint; // Save point before the clip has been applied

        private ClipShape(Shape shape, int savePoint) {
            this.savePoint = savePoint;
            this.shape = shape;
        }

        public Rectangle2D getBounds() {
            if (bounds == null) {
                // We need to apply the save points inverse transform to this
                bounds = shape.getBounds2D();
            }
            return bounds;
        }
    }

    private class ClipStack {

        private final Deque<ClipShape> clipStack = new ArrayDeque<>();

        private void pushClip(Shape awtClipShape) {
            PathIterator awtIterator = awtClipShape.getPathIterator(null);
            FXAWTBridge.applyPathIterator(ctx, awtIterator);
            FXAWTBridge.applyWindingRule(ctx, awtIterator.getWindingRule());

            int savePoint = ctxSaveCounter.save();
            ctx.clip();

            clipStack.add(new ClipShape(awtClipShape, savePoint));
        }

        private void popClip() {
            if (clipStack.isEmpty()) {
                return;
            }
            FXOutputState currentState = new FXOutputState(SaveClipStack.NO);
            ClipShape clipShape = clipStack.removeLast();
            ctxSaveCounter.restoreTo(clipShape.savePoint);
            currentState.restore();
        }

        private void clearClip() {
            if (clipStack.isEmpty()) {
                return;
            }
            FXOutputState currentState = new FXOutputState(SaveClipStack.NO);
            while (!clipStack.isEmpty()) {
                ClipShape clipShape = clipStack.removeLast();
                ctxSaveCounter.restoreTo(clipShape.savePoint);
            }
            currentState.restore();
        }

        private void restoreClipStack(@NotNull List<Shape> originalClipStack) {
            if (clipStack.isEmpty() && originalClipStack.isEmpty()) {
                return;
            }

            FXOutputState currentState = new FXOutputState(SaveClipStack.NO);

            int validClips = 0;
            int minSize = Math.min(clipStack.size(), originalClipStack.size());

            // Compare clips in both stacks to find the first non-matching clip
            for (ClipShape currentClip : clipStack) {
                if (validClips >= minSize) {
                    break;
                }
                Shape originalClipShape = originalClipStack.get(validClips);

                if (currentClip == null || !currentClip.shape.equals(originalClipShape)) {
                    break;
                }
                validClips++;
            }

            // Remove invalid clips from the current stack
            int clipsToRemove = clipStack.size() - validClips;
            for (int i = 0; i < clipsToRemove; i++) {
                ClipShape clipShape = clipStack.removeLast();
                ctxSaveCounter.restoreTo(clipShape.savePoint);
            }

            currentState.restore();

            // Add missing clips from the original stack
            for (int i = validClips; i < originalClipStack.size(); i++) {
                Shape originalClipShape = originalClipStack.get(i);
                applyClip(originalClipShape);
            }
        }

        private List<Shape> snapshot() {
            List<Shape> snapshot = new ArrayList<>(this.clipStack.size());
            for (ClipShape clipShape : this.clipStack) {
                snapshot.add(clipShape.shape);
            }
            return snapshot;
        }

        private Rectangle2D getClipBounds() {
            if (clipStack.isEmpty()) {
                return new Rectangle2D.Double(0, 0, ctx.getCanvas().getWidth(), ctx.getCanvas().getHeight());
            }
            return clipStack.peekLast().getBounds();
        }
    }

    /**
     * We don't have direct access to set the clip required for the setClip() method, so we must track the number of save/restore calls.
     * Then we can pop the stack back to the correct clip.
     */
    private class GraphicsContextSaveCounter {

        private int saveCount = 0;

        private GraphicsContextSaveCounter() {
            super();
        }

        private int save() {
            ctx.save();
            return saveCount++;
        }

        private void restoreTo(int count) {
            while (saveCount > count) {
                ctx.restore();
                saveCount--;
            }
        }

    }

}
