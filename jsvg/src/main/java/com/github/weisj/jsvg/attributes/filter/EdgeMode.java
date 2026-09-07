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
                @NotNull ImageProducer producer, @NotNull ConvolveOperation convolveOperation) {
            return convolveDuplicate(context, filterContext, producer, convolveOperation);
        }
    },
    Wrap {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull ConvolveOperation convolveOperation) {
            return convolveWrap(context, filterContext, producer, convolveOperation);
        }
    },
    None {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull ConvolveOperation convolveOperation) {
            EdgeModeImage image = prepareEdgeModeImage(context, producer, convolveOperation);
            return applyConvolutions(filterContext.renderingHints(), image, convolveOperation);
        }
    };

    public abstract ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull ConvolveOperation convolveOperation);

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

        private EdgeModeImage(@NotNull BufferedImage img, int xOff, int yOff, int width, int height) {
            this.img = img;
            this.xOff = xOff;
            this.yOff = yOff;
            this.width = width;
            this.height = height;
        }
    }

    private static EdgeModeImage prepareEdgeModeImage(@NotNull RenderContext context,
            @NotNull ImageProducer producer, @NotNull ConvolveOperation convolveOperation) {
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

        return new EdgeModeImage(bufferedImage, xOff, yOff, width, height);
    }

    private static @NotNull ImageProducer convolveDuplicate(@NotNull RenderContext context,
            @NotNull FilterContext filterContext, @NotNull ImageProducer producer,
            @NotNull ConvolveOperation convolveOperation) {
        EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, convolveOperation);
        int xOff = edgeModeImage.xOff;
        int yOff = edgeModeImage.yOff;
        int width = edgeModeImage.width;
        int height = edgeModeImage.height;

        Graphics2D g = GraphicsUtil.createGraphics(edgeModeImage.img);

        BufferedImage top = edgeModeImage.img.getSubimage(xOff, yOff, width, 1);
        BufferedImage left = edgeModeImage.img.getSubimage(xOff, yOff, 1, height);
        BufferedImage right = edgeModeImage.img.getSubimage(xOff + width - 1, yOff, 1, height);
        BufferedImage bottom = edgeModeImage.img.getSubimage(xOff, yOff + height - 1, width, 1);

        g.drawImage(top, xOff, 0, width, yOff, null);
        g.drawImage(bottom, xOff, yOff + height, width, yOff, null);
        g.drawImage(left, 0, yOff, xOff, height, null);
        g.drawImage(right, xOff + width, yOff, xOff, height, null);

        Color topLeft = new Color(top.getRGB(0, 0), true);
        Color topRight = new Color(top.getRGB(top.getWidth() - 1, 0), true);
        Color bottomLeft = new Color(bottom.getRGB(0, 0), true);
        Color bottomRight = new Color(bottom.getRGB(bottom.getWidth() - 1, 0), true);

        g.setColor(topLeft);
        g.fillRect(0, 0, xOff, yOff);

        g.setColor(topRight);
        g.fillRect(xOff + width, 0, xOff, yOff);

        g.setColor(bottomLeft);
        g.fillRect(0, yOff + height, xOff, yOff);

        g.setColor(bottomRight);
        g.fillRect(xOff + width, yOff + height, xOff, yOff);

        g.dispose();

        return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
    }

    private static ImageProducer convolveWrap(@NotNull RenderContext context, @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull ConvolveOperation convolveOperation) {
        EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, convolveOperation);
        int xOff = edgeModeImage.xOff;
        int yOff = edgeModeImage.yOff;
        int width = edgeModeImage.width;
        int height = edgeModeImage.height;

        Graphics2D g = GraphicsUtil.createGraphics(edgeModeImage.img);

        if (xOff > width || yOff > height) {
            // A border wider than the input needs more than one copy of its opposite edge.
            BufferedImage tile = edgeModeImage.img.getSubimage(xOff, yOff, width, height);
            g.setPaint(new TexturePaint(tile, new Rectangle(xOff, yOff, width, height)));
            g.fillRect(0, 0, edgeModeImage.img.getWidth(), yOff);
            g.fillRect(0, yOff + height, edgeModeImage.img.getWidth(), yOff);
            g.fillRect(0, yOff, xOff, height);
            g.fillRect(xOff + width, yOff, xOff, height);
            g.dispose();
            return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
        }

        if (yOff > 0) {
            BufferedImage top = edgeModeImage.img.getSubimage(xOff, yOff, width, yOff);
            BufferedImage bottom = edgeModeImage.img.getSubimage(xOff, height, width, yOff);
            g.drawImage(bottom, xOff, 0, null);
            g.drawImage(top, xOff, yOff + height, null);
        }
        if (xOff > 0) {
            BufferedImage left = edgeModeImage.img.getSubimage(xOff, yOff, xOff, height);
            BufferedImage right = edgeModeImage.img.getSubimage(width, yOff, xOff, height);
            g.drawImage(right, 0, yOff, null);
            g.drawImage(left, xOff + width, yOff, null);
        }
        if (xOff > 0 && yOff > 0) {
            BufferedImage topLeft = edgeModeImage.img.getSubimage(xOff, yOff, xOff, yOff);
            BufferedImage topRight = edgeModeImage.img.getSubimage(width, yOff, xOff, yOff);
            BufferedImage bottomLeft = edgeModeImage.img.getSubimage(xOff, height, xOff, yOff);
            BufferedImage bottomRight = edgeModeImage.img.getSubimage(width, height, xOff, yOff);
            g.drawImage(bottomRight, 0, 0, null);
            g.drawImage(bottomLeft, xOff + width, 0, null);
            g.drawImage(topRight, 0, yOff + height, null);
            g.drawImage(topLeft, xOff + width, yOff + height, null);
        }
        g.dispose();

        return applyConvolutions(filterContext.renderingHints(), edgeModeImage, convolveOperation);
    }

    private static ImageProducer applyConvolutions(@Nullable RenderingHints hints, @NotNull EdgeModeImage image,
            @NotNull ConvolveOperation convolveOperation) {
        ImageProducer output = convolveOperation.convolve(image.img, hints, ConvolveOp.EDGE_NO_OP);
        return new FilteredImageSource(output, new CropImageFilter(image.xOff, image.yOff, image.width, image.height));
    }

}
