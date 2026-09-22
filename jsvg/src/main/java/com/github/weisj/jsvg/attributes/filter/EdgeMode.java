/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
package com.github.weisj.jsvg.attributes.filter;

import java.awt.*;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.filter.FilterContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import com.github.weisj.jsvg.util.ImageUtil;

public enum EdgeMode {
    Duplicate {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull Rectangle sourceBounds,
                @NotNull ConvolveOperation convolveOperation) {
            return convolveDuplicate(context, filterContext, producer, sourceBounds, convolveOperation);
        }
    },
    Wrap {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull Rectangle sourceBounds,
                @NotNull ConvolveOperation convolveOperation) {
            return convolveWrap(context, filterContext, producer, sourceBounds, convolveOperation);
        }
    },
    None {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull Rectangle sourceBounds,
                @NotNull ConvolveOperation convolveOperation) {
            EdgeModeImage image = prepareEdgeModeImage(context, producer, sourceBounds, convolveOperation);
            return applyConvolutions(filterContext.renderingHints(), image, convolveOperation);
        }
    };

    public abstract ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull Rectangle sourceBounds,
            @NotNull ConvolveOperation convolveOperation);

    public interface ConvolveOperation {

        @NotNull
        Dimension kernelRadius();

        @NotNull
        ImageProducer convolve(@NotNull BufferedImage image, @Nullable RenderingHints hints, int awtEdgeMode);
    }

    private static final class EdgeModeImage {
        private final @NotNull BufferedImage img;
        private final int xOff;
        private final int yOff;
        private final int width;
        private final int height;
        private final @NotNull Rectangle sourceBounds;

        private EdgeModeImage(@NotNull BufferedImage img, int xOff, int yOff, int width, int height,
                @NotNull Rectangle sourceBounds) {
            this.img = img;
            this.xOff = xOff;
            this.yOff = yOff;
            this.width = width;
            this.height = height;
            this.sourceBounds = sourceBounds;
        }
    }

    private static EdgeModeImage prepareEdgeModeImage(@NotNull RenderContext context,
            @NotNull ImageProducer producer, @NotNull Rectangle sourceBounds,
            @NotNull ConvolveOperation convolveOperation) {
        Image img = context.platformSupport().createImage(producer);
        int width = img.getWidth(null);
        int height = img.getHeight(null);

        Dimension kernelRadius = convolveOperation.kernelRadius();
        int xOff = kernelRadius.width;
        int yOff = kernelRadius.height;

        BufferedImage bufferedImage = ImageUtil.createCompatibleTransparentImage(width + 2 * xOff, height + 2 * yOff,
                ImageUtil.Premultiplied.Yes);
        Graphics2D g = GraphicsUtil.createGraphics(bufferedImage);

        g.translate(xOff, yOff);
        g.drawImage(img, null, null);
        g.dispose();

        sourceBounds.translate(xOff, yOff);
        return new EdgeModeImage(bufferedImage, xOff, yOff, width, height, sourceBounds);
    }

    private static @NotNull ImageProducer convolveDuplicate(@NotNull RenderContext context,
            @NotNull FilterContext filterContext, @NotNull ImageProducer producer,
            @NotNull Rectangle sourceBounds, @NotNull ConvolveOperation convolveOperation) {
        EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, sourceBounds, convolveOperation);
        int xOff = edgeModeImage.sourceBounds.x;
        int yOff = edgeModeImage.sourceBounds.y;
        int width = edgeModeImage.sourceBounds.width;
        int height = edgeModeImage.sourceBounds.height;
        if (width <= 0 || height <= 0) {
            return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
        }

        Graphics2D g = GraphicsUtil.createGraphics(edgeModeImage.img);

        BufferedImage img = edgeModeImage.img;
        // Source coordinates may lie outside the allocated image. drawImage clips those reads,
        // without treating the allocation boundary as an edge of the input region.
        g.drawImage(img, xOff, 0, xOff + width, yOff,
                xOff, yOff, xOff + width, yOff + 1, null);
        g.drawImage(img, xOff, yOff + height, xOff + width, img.getHeight(),
                xOff, yOff + height - 1, xOff + width, yOff + height, null);
        g.drawImage(img, 0, yOff, xOff, yOff + height,
                xOff, yOff, xOff + 1, yOff + height, null);
        g.drawImage(img, xOff + width, yOff, img.getWidth(), yOff + height,
                xOff + width - 1, yOff, xOff + width, yOff + height, null);

        g.drawImage(img, 0, 0, xOff, yOff,
                xOff, yOff, xOff + 1, yOff + 1, null);
        g.drawImage(img, xOff + width, 0, img.getWidth(), yOff,
                xOff + width - 1, yOff, xOff + width, yOff + 1, null);
        g.drawImage(img, 0, yOff + height, xOff, img.getHeight(),
                xOff, yOff + height - 1, xOff + 1, yOff + height, null);
        g.drawImage(img, xOff + width, yOff + height, img.getWidth(), img.getHeight(),
                xOff + width - 1, yOff + height - 1, xOff + width, yOff + height, null);

        g.dispose();

        return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
    }

    private static ImageProducer convolveWrap(@NotNull RenderContext context, @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull Rectangle sourceBounds,
            @NotNull ConvolveOperation convolveOperation) {
        EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, sourceBounds, convolveOperation);
        int xOff = edgeModeImage.sourceBounds.x;
        int yOff = edgeModeImage.sourceBounds.y;
        int width = edgeModeImage.sourceBounds.width;
        int height = edgeModeImage.sourceBounds.height;
        int rightPadding = edgeModeImage.img.getWidth() - xOff - width;
        int bottomPadding = edgeModeImage.img.getHeight() - yOff - height;
        if (width <= 0 || height <= 0) {
            return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
        }

        Graphics2D g = GraphicsUtil.createGraphics(edgeModeImage.img);

        if (xOff > width || yOff > height || rightPadding > width || bottomPadding > height) {
            // A border wider than the input needs more than one copy of its opposite edge.
            paintRepeatedEdges(g, edgeModeImage.img, edgeModeImage.sourceBounds);
        } else {
            paintWrappedEdges(g, edgeModeImage.img, edgeModeImage.sourceBounds);
        }
        g.dispose();

        return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
    }

    private static void paintRepeatedEdges(@NotNull Graphics2D g, @NotNull BufferedImage img,
            @NotNull Rectangle sourceBounds) {
        int xOff = sourceBounds.x;
        int yOff = sourceBounds.y;
        int width = sourceBounds.width;
        int height = sourceBounds.height;
        int rightPadding = img.getWidth() - xOff - width;
        int bottomPadding = img.getHeight() - yOff - height;
        if (xOff >= 0 && yOff >= 0 && rightPadding >= 0 && bottomPadding >= 0) {
            BufferedImage tile = img.getSubimage(xOff, yOff, width, height);
            g.setPaint(new TexturePaint(tile, sourceBounds));
            g.fillRect(0, 0, img.getWidth(), yOff);
            g.fillRect(0, yOff + height, img.getWidth(), bottomPadding);
            g.fillRect(0, yOff, xOff, height);
            g.fillRect(xOff + width, yOff, rightPadding, height);
            return;
        }
        // Only part of the tile is allocated. Keep the full region's repetition period,
        // and let drawImage clip source pixels outside the backing image.
        for (int y = Math.floorDiv(-yOff, height); y * height < img.getHeight() - yOff; y++) {
            for (int x = Math.floorDiv(-xOff, width); x * width < img.getWidth() - xOff; x++) {
                if (x == 0 && y == 0) continue;
                g.drawImage(img,
                        xOff + x * width, yOff + y * height, xOff + (x + 1) * width, yOff + (y + 1) * height,
                        xOff, yOff, xOff + width, yOff + height, null);
            }
        }
    }

    private static void paintWrappedEdges(@NotNull Graphics2D g, @NotNull BufferedImage img,
            @NotNull Rectangle sourceBounds) {
        int xOff = sourceBounds.x;
        int yOff = sourceBounds.y;
        int width = sourceBounds.width;
        int height = sourceBounds.height;
        int rightPadding = img.getWidth() - xOff - width;
        int bottomPadding = img.getHeight() - yOff - height;
        if (yOff > 0) {
            g.drawImage(img, xOff, 0, xOff + width, yOff,
                    xOff, height, xOff + width, yOff + height, null);
        }
        if (bottomPadding > 0) {
            g.drawImage(img, xOff, yOff + height, xOff + width, img.getHeight(),
                    xOff, yOff, xOff + width, yOff + bottomPadding, null);
        }
        if (xOff > 0) {
            g.drawImage(img, 0, yOff, xOff, yOff + height,
                    width, yOff, xOff + width, yOff + height, null);
        }
        if (rightPadding > 0) {
            g.drawImage(img, xOff + width, yOff, img.getWidth(), yOff + height,
                    xOff, yOff, xOff + rightPadding, yOff + height, null);
        }
        if (xOff > 0 && yOff > 0) {
            g.drawImage(img, 0, 0, xOff, yOff,
                    width, height, xOff + width, yOff + height, null);
        }
        if (rightPadding > 0 && yOff > 0) {
            g.drawImage(img, xOff + width, 0, img.getWidth(), yOff,
                    xOff, height, xOff + rightPadding, yOff + height, null);
        }
        if (xOff > 0 && bottomPadding > 0) {
            g.drawImage(img, 0, yOff + height, xOff, img.getHeight(),
                    width, yOff, xOff + width, yOff + bottomPadding, null);
        }
        if (rightPadding > 0 && bottomPadding > 0) {
            g.drawImage(img, xOff + width, yOff + height, img.getWidth(), img.getHeight(),
                    xOff, yOff, xOff + rightPadding, yOff + bottomPadding, null);
        }
    }

    private static ImageProducer applyConvolutions(@Nullable RenderingHints hints, @NotNull EdgeModeImage image,
            @NotNull ConvolveOperation convolveOperation) {
        ImageProducer output = convolveOperation.convolve(image.img, hints, ConvolveOp.EDGE_NO_OP);
        return new FilteredImageSource(output, new CropImageFilter(image.xOff, image.yOff, image.width, image.height));
    }

}
