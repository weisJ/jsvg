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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.renderer.*;

public final class BlittableImage {

    @FunctionalInterface
    public interface BufferSurfaceSupplier {
        @NotNull
        BufferedImage createBufferSurface(@Nullable AffineTransform at, double width, double height);
    }

    private final @NotNull BufferedImage image;
    private final @NotNull RenderContext context;
    private final @NotNull Rectangle2D boundsInUserSpace;
    private final @NotNull UnitType contentUnits;

    private BlittableImage(@NotNull BufferedImage image, @NotNull RenderContext context,
            @NotNull Rectangle2D boundsInUserSpace, @NotNull UnitType contentUnits) {
        this.image = image;
        this.context = context;
        this.boundsInUserSpace = boundsInUserSpace;
        this.contentUnits = contentUnits;
    }

    public static @NotNull BlittableImage create(@NotNull BufferSurfaceSupplier bufferSurfaceSupplier,
            @NotNull RenderContext context, @Nullable Rectangle2D clipBounds,
            @NotNull Rectangle2D bounds, @NotNull Rectangle2D objectBounds, @NotNull UnitType contentUnits) {
        Rectangle2D boundsInUserSpace =
                GeometryUtil.containingBoundsAfterTransform(context.userSpaceTransform(), bounds);
        Rectangle2D boundsInRootSpace =
                GeometryUtil.containingBoundsAfterTransform(context.rootTransform(), boundsInUserSpace);

        if (clipBounds != null) {
            Rectangle2D clipBoundsInUserSpace =
                    GeometryUtil.containingBoundsAfterTransform(context.userSpaceTransform(), clipBounds);
            Rectangle2D clipBoundsInRootSpace =
                    GeometryUtil.containingBoundsAfterTransform(context.rootTransform(), clipBoundsInUserSpace);
            Rectangle2D.intersect(clipBoundsInRootSpace, boundsInRootSpace, boundsInRootSpace);
        }

        int imgX = (int) Math.floor(boundsInRootSpace.getX());
        int imgY = (int) Math.floor(boundsInRootSpace.getY());
        int imgWidth = (int) Math.ceil(boundsInRootSpace.getX() + boundsInRootSpace.getWidth()) - imgX;
        int imgHeight = (int) Math.ceil(boundsInRootSpace.getY() + boundsInRootSpace.getHeight()) - imgY;

        Rectangle2D adjustedUserSpaceBounds = new Rectangle2D.Double(imgX, imgY, imgWidth, imgHeight);
        try {
            adjustedUserSpaceBounds = GeometryUtil
                    .containingBoundsAfterTransform(context.rootTransform().createInverse(), adjustedUserSpaceBounds);
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }

        BufferedImage img = bufferSurfaceSupplier.createBufferSurface(null, imgWidth, imgHeight);

        RenderContext imageContext = RenderContext.createInitial(context.platformSupport(),
                contentUnits.deriveMeasure(context.measureContext()));

        AffineTransform rootTransform = new AffineTransform();

        Rectangle2D ub = adjustedUserSpaceBounds;
        // Adjust for subpixel alignment
        rootTransform.translate(boundsInRootSpace.getX() - imgX, boundsInRootSpace.getY() - imgY);

        if (contentUnits == UnitType.ObjectBoundingBox) {
            rootTransform.scale(
                    objectBounds.getWidth() * img.getWidth() / ub.getWidth(),
                    objectBounds.getWidth() * img.getHeight() / ub.getHeight());
        } else {
            rootTransform.scale(
                    img.getWidth() / ub.getWidth(),
                    img.getHeight() / ub.getHeight());
            rootTransform.translate(-ub.getX(), -ub.getY());
        }

        imageContext.setRootTransform(rootTransform, context.userSpaceTransform());

        return new BlittableImage(img, imageContext, ub, contentUnits);
    }

    public @NotNull Rectangle2D boundsInUserSpace() {
        return boundsInUserSpace;
    }

    public @NotNull BufferedImage image() {
        return image;
    }

    public @NotNull Graphics2D createGraphics() {
        Graphics2D g = GraphicsUtil.createGraphics(image);
        g.transform(context.rootTransform());

        if (contentUnits == UnitType.UserSpaceOnUse) {
            g.transform(context.userSpaceTransform());
        } else {
            // Reset the view transform.
            context.setRootTransform(context.rootTransform(), new AffineTransform());
        }

        return g;
    }

    public void renderNode(@NotNull Output parentOutput, @NotNull SVGNode node,
            @NotNull Instantiator instantiator) {
        Graphics2D imgGraphics = createGraphics();
        Output imgOutput = new Graphics2DOutput(imgGraphics);
        imgGraphics.setRenderingHints(parentOutput.renderingHints());
        try (NodeRenderer.Info info = NodeRenderer.createRenderInfo(node, context, imgOutput, instantiator)) {
            if (info != null) info.renderable.render(info.context, info.output());
        }
        imgGraphics.dispose();
    }

    public void prepareForBlitting(@NotNull Output output, @NotNull RenderContext parentContext) {
        output.setTransform(parentContext.rootTransform());
        output.translate(boundsInUserSpace.getX(), boundsInUserSpace.getY());
        output.scale(
                boundsInUserSpace.getWidth() / image.getWidth(),
                boundsInUserSpace.getHeight() / image.getHeight());
    }

    public void blitTo(@NotNull Output output, @NotNull RenderContext parentContext) {
        Output out = output.createChild();
        prepareForBlitting(out, parentContext);
        out.drawImage(image);
        out.dispose();
    }
}
