/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;

/**
 * A filter surface whose pixel axes follow user coordinates. Source artwork is rendered directly
 * into this surface; the remaining rotation or shear is applied when the result is blitted.
 */
public final class TransformedBlittableImage implements OffscreenImage {
    private final @NotNull BufferedImage image;
    private final @NotNull RenderContext context;
    private final @NotNull Rectangle2D clippedUserBounds;
    private final @NotNull AffineTransform imageToDevice;
    private final @NotNull AffineTransform userToImage;

    private TransformedBlittableImage(@NotNull BufferedImage image, @NotNull RenderContext context,
            @NotNull Rectangle2D clippedUserBounds, @NotNull AffineTransform imageToDevice,
            @NotNull AffineTransform userToImage) {
        this.image = image;
        this.context = context;
        this.clippedUserBounds = clippedUserBounds;
        this.imageToDevice = imageToDevice;
        this.userToImage = userToImage;
    }

    public static @Nullable TransformedBlittableImage create(
            @NotNull BlittableImage.BufferSurfaceSupplier bufferSurfaceSupplier,
            @NotNull RenderContext context, @NotNull RenderContext imageContext,
            @NotNull Rectangle2D bounds, @NotNull Rectangle2D clipBounds,
            @NotNull AffineTransform bufferTransform) {
        Rectangle2D imageBounds = GeometryUtil.containingBoundsAfterTransform(
                bufferTransform, bounds.createIntersection(clipBounds));
        if (ShapeUtil.isInvalidArea(imageBounds)) return null;
        // Anchor the sampling grid to user coordinates, independently of the current graphics clip.
        GeometryUtil.adjustForAliasing(imageBounds);
        AffineTransform userToImage = AffineTransform.getTranslateInstance(-imageBounds.getX(), -imageBounds.getY());
        userToImage.concatenate(bufferTransform);
        Rectangle2D clippedUserBounds = GeometryUtil.containingBoundsAfterTransform(
                GeometryUtil.createInverse(bufferTransform), imageBounds);

        AffineTransform userToDevice = new AffineTransform(context.rootTransform());
        userToDevice.concatenate(context.userSpaceTransform());
        AffineTransform imageToDevice = new AffineTransform(userToDevice);
        imageToDevice.concatenate(GeometryUtil.createInverse(userToImage));

        AffineTransform deviceToImage = new AffineTransform(userToImage);
        deviceToImage.concatenate(GeometryUtil.createInverse(userToDevice));
        AffineTransform rootTransform = new AffineTransform(deviceToImage);
        rootTransform.concatenate(context.rootTransform());
        AffineTransform hostTransform = new AffineTransform(deviceToImage);
        hostTransform.concatenate(context.hostTransform());
        RenderContextAccessor.instance().setTransforms(imageContext, hostTransform, rootTransform,
                context.userSpaceTransform());

        BufferedImage image = bufferSurfaceSupplier.createBufferSurface(null,
                imageBounds.getWidth(), imageBounds.getHeight());
        return new TransformedBlittableImage(image, imageContext, clippedUserBounds, imageToDevice, userToImage);
    }

    @Override
    public @NotNull BufferedImage image() {
        return image;
    }

    @Override
    public @NotNull RenderContext context() {
        return context;
    }

    @Override
    public @NotNull Rectangle2D clippedUserBounds() {
        return clippedUserBounds;
    }

    @Override
    public @NotNull Graphics2D createGraphics() {
        Graphics2D graphics = GraphicsUtil.createGraphics(image);
        // Keep the exact axis-aligned mapping rather than cancelling context transforms numerically.
        graphics.setTransform(userToImage);
        return graphics;
    }

    @Override
    public void prepareForBlitting(@NotNull Output output) {
        output.setTransform(imageToDevice);
    }
}
