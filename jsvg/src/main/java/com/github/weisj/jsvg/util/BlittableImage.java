/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
package com.github.weisj.jsvg.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.renderer.*;

/**
 * Class that encapsulates rendering to an offscreen image.
 * The image is aligned to the pixel boundary of the root image surface. Rendering to the
 * image behaves and the blitting it behaves as if it was rendered directly to the root surface.
 */
public final class BlittableImage {


    @FunctionalInterface
    public interface BufferSurfaceSupplier {
        @NotNull
        BufferedImage createBufferSurface(@Nullable AffineTransform at, double width, double height);
    }

    private final @NotNull BufferedImage image;
    public final @NotNull RenderContext context;
    private final @NotNull Rectangle2D boundsInDeviceSpace;
    private final @NotNull Rectangle2D userBoundsInRootSpace;

    private BlittableImage(@NotNull BufferedImage image, @NotNull RenderContext context,
            @NotNull Rectangle2D boundsInDeviceSpace, @NotNull Rectangle2D userBoundsInRootSpace) {
        this.image = image;
        this.context = context;
        this.boundsInDeviceSpace = boundsInDeviceSpace;
        this.userBoundsInRootSpace = userBoundsInRootSpace;
    }

    public static @Nullable BlittableImage create(@NotNull BufferSurfaceSupplier bufferSurfaceSupplier,
            @NotNull RenderContext context, @Nullable Rectangle2D clipBounds,
            @NotNull Rectangle2D bounds, @NotNull Rectangle2D objectBounds, @NotNull UnitType contentUnits) {
        Rectangle2D boundsInDeviceSpace = GeometryUtil.userBoundsToDeviceBounds(context, bounds);

        if (clipBounds != null) {
            Rectangle2D clipBoundsInDeviceSpace = GeometryUtil.userBoundsToDeviceBounds(context, clipBounds);
            Rectangle2D.intersect(clipBoundsInDeviceSpace, boundsInDeviceSpace, boundsInDeviceSpace);
        }

        if (ShapeUtil.isInvalidArea(boundsInDeviceSpace)) return null;

        Rectangle2D adjustedBoundsInRootSpace = GeometryUtil.convertBounds(context, boundsInDeviceSpace,
                GeometryUtil.Space.Device, GeometryUtil.Space.Root);

        // Convert to integer coordinates to ensure we don't cut off any pixels due to rounding errors.
        // Increase size by 1 to ensure we don't cut off any pixels used for anti-aliasing.
        boundsInDeviceSpace = GeometryUtil.adjustForAliasing(GeometryUtil.grow(boundsInDeviceSpace, 1));

        BufferedImage img = bufferSurfaceSupplier.createBufferSurface(null,
                boundsInDeviceSpace.getWidth(),
                boundsInDeviceSpace.getHeight());

        AffineTransform rootTransform = new AffineTransform();
        rootTransform.translate(-boundsInDeviceSpace.getX(), -boundsInDeviceSpace.getY());
        rootTransform.concatenate(context.rootTransform());

        AffineTransform userSpaceTransform = new AffineTransform(context.userSpaceTransform());
        if (contentUnits == UnitType.ObjectBoundingBox) {
            userSpaceTransform = new AffineTransform(userSpaceTransform);
            userSpaceTransform.translate(objectBounds.getX(), objectBounds.getY());
            userSpaceTransform.scale(objectBounds.getWidth(), objectBounds.getHeight());
        }

        // Note: This should actually be the render context from the declaration site of the mask/clipPath
        // etc.
        RenderContext imageContext = RenderContext.createInitial(context.platformSupport(),
                contentUnits.deriveMeasure(context.measureContext()));
        imageContext.setRootTransform(rootTransform, userSpaceTransform);

        return new BlittableImage(img, imageContext, boundsInDeviceSpace, adjustedBoundsInRootSpace);
    }

    public @NotNull Rectangle2D imageBoundsInDeviceSpace() {
        return boundsInDeviceSpace;
    }

    public @NotNull Rectangle2D userBoundsInRootSpace() {
        return userBoundsInRootSpace;
    }

    public @NotNull BufferedImage image() {
        return image;
    }

    public @NotNull Graphics2D createGraphics() {
        Graphics2D g = GraphicsUtil.createGraphics(image);
        g.transform(context.rootTransform());
        g.transform(context.userSpaceTransform());
        return g;
    }

    public void renderNode(@NotNull Output parentOutput, @NotNull SVGNode node,
            @NotNull Instantiator instantiator) {
        render(parentOutput, (out, ctx) -> NodeRenderer.renderNode(node, ctx, out, instantiator));
    }

    public void clearBackground(@NotNull Color color) {
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }

    public void render(@NotNull Output output, @NotNull Consumer<Graphics2D> painter) {
        Graphics2D imgGraphics = createGraphics();
        imgGraphics.setRenderingHints(output.renderingHints());
        painter.accept(imgGraphics);
        imgGraphics.dispose();
    }

    public void render(@NotNull Output output, @NotNull BiConsumer<Output, RenderContext> painter) {
        Graphics2D imgGraphics = createGraphics();
        imgGraphics.setRenderingHints(output.renderingHints());
        painter.accept(new Graphics2DOutput(imgGraphics), context);
        imgGraphics.dispose();
    }

    public void prepareForBlitting(@NotNull Output output) {
        output.setTransform(AffineTransform.getTranslateInstance(
                boundsInDeviceSpace.getX(), boundsInDeviceSpace.getY()));
    }

    public void blitTo(@NotNull Output output) {
        Output out = output.createChild();
        out.setTransform(AffineTransform.getTranslateInstance(
                boundsInDeviceSpace.getX(), boundsInDeviceSpace.getY()));
        out.drawImage(image);
        out.dispose();
    }

    public void debug(@NotNull Output output) {
        debug(output, true);
    }

    public void debug(@NotNull Output output, boolean drawImage) {
        output.debugPaint(g -> {
            g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            g.setTransform(AffineTransform.getTranslateInstance(
                    boundsInDeviceSpace.getX(), boundsInDeviceSpace.getY()));
            if (drawImage) {
                g.drawImage(image, 0, 0, null);
            }
            g.setColor(Color.MAGENTA);
            g.drawRect(0, 0, image.getWidth(), image.getHeight());
        });
    }
}
