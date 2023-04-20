/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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

import com.github.weisj.jsvg.nodes.filter.FilterContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public enum EdgeMode {
    Duplicate {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount) {
            return convolveDuplicate(context, filterContext, producer, kernels, kernelCount);
        }
    },
    Wrap {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount) {
            return convolveWrap(context, filterContext, producer, kernels, kernelCount);
        }
    },
    None {
        @Override
        public ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
                @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount) {
            return applyConvolutions(filterContext.renderingHints(), producer, kernels, kernelCount,
                    ConvolveOp.EDGE_ZERO_FILL);
        }
    };

    public abstract ImageProducer convolve(@NotNull RenderContext context, @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount);


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
            @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount) {
        Image img = context.createImage(producer);
        int width = img.getWidth(null);
        int height = img.getHeight(null);

        int xSize = 0;
        int ySize = 0;
        for (int i = 0; i < kernelCount; i++) {
            Kernel kernel = kernels[i];
            xSize = Math.max(xSize, kernel.getWidth());
            ySize = Math.max(ySize, kernel.getHeight());
        }

        BufferedImage bufferedImage = new BufferedImage(
                width + xSize, height + ySize,
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = bufferedImage.createGraphics();

        int xOff = xSize / 2;
        int yOff = ySize / 2;

        g.translate(xOff, yOff);
        g.drawImage(img, null, null);
        g.dispose();

        return new EdgeModeImage(bufferedImage, xOff, yOff, width, height);
    }

    private static @NotNull ImageProducer convolveDuplicate(@NotNull RenderContext context,
            @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount) {
        if (kernelCount <= 0) return producer;

        EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, kernels, kernelCount);
        int xOff = edgeModeImage.xOff;
        int yOff = edgeModeImage.yOff;
        int width = edgeModeImage.width;
        int height = edgeModeImage.height;

        Graphics2D g = edgeModeImage.img.createGraphics();

        BufferedImage top = edgeModeImage.img.getSubimage(xOff, yOff, width, 1);
        BufferedImage left = edgeModeImage.img.getSubimage(xOff, yOff, 1, height);
        BufferedImage right = edgeModeImage.img.getSubimage(xOff + width - 1, yOff, 1, height);
        BufferedImage bottom = edgeModeImage.img.getSubimage(xOff, yOff + height - 1, width, 1);

        g.drawImage(top, xOff, 0, width, yOff, null);
        g.drawImage(bottom, xOff, yOff + height, width, yOff, null);
        g.drawImage(left, 0, yOff, xOff, height, null);
        g.drawImage(right, xOff + height, yOff, xOff, height, null);

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

        ImageProducer output =
                applyConvolutions(filterContext.renderingHints(), edgeModeImage.img.getSource(), kernels, kernelCount,
                        ConvolveOp.EDGE_NO_OP);
        CropImageFilter cropImageFilter = new CropImageFilter(xOff, yOff, width, height);
        return new FilteredImageSource(output, cropImageFilter);
    }

    private static ImageProducer convolveWrap(@NotNull RenderContext context, @NotNull FilterContext filterContext,
            @NotNull ImageProducer producer, @NotNull Kernel[] kernels, int kernelCount) {
        if (kernelCount <= 0) return producer;

        EdgeModeImage edgeModeImage = prepareEdgeModeImage(context, producer, kernels, kernelCount);
        int xOff = edgeModeImage.xOff;
        int yOff = edgeModeImage.yOff;
        int width = edgeModeImage.width;
        int height = edgeModeImage.height;

        Graphics2D g = edgeModeImage.img.createGraphics();

        BufferedImage top = edgeModeImage.img.getSubimage(xOff, yOff, width, yOff);
        BufferedImage left = edgeModeImage.img.getSubimage(xOff, yOff, xOff, height);
        BufferedImage right = edgeModeImage.img.getSubimage(width - 1, yOff, xOff, height);
        BufferedImage bottom = edgeModeImage.img.getSubimage(xOff, height - 1, width, yOff);

        BufferedImage topLeft = edgeModeImage.img.getSubimage(xOff, yOff, xOff, yOff);
        BufferedImage topRight = edgeModeImage.img.getSubimage(width - 1, yOff, xOff, yOff);
        BufferedImage bottomLeft = edgeModeImage.img.getSubimage(xOff, height - 1, width, yOff);
        BufferedImage bottomRight = edgeModeImage.img.getSubimage(width - 1, height - 1, xOff, yOff);

        g.drawImage(bottom, xOff, 0, null);
        g.drawImage(top, xOff, yOff + height, null);
        g.drawImage(right, 0, yOff, null);
        g.drawImage(left, xOff + width, yOff, null);

        g.drawImage(bottomRight, 0, 0, null);
        g.drawImage(bottomLeft, xOff + width, 0, null);
        g.drawImage(topRight, 0, yOff + height, null);
        g.drawImage(topLeft, xOff + width, yOff + height, null);

        ImageProducer output =
                applyConvolutions(filterContext.renderingHints(), edgeModeImage.img.getSource(), kernels, kernelCount,
                        ConvolveOp.EDGE_NO_OP);
        CropImageFilter cropImageFilter = new CropImageFilter(xOff, yOff, width, height);
        return new FilteredImageSource(output, cropImageFilter);
    }

    private static ImageProducer applyConvolutions(@NotNull RenderingHints hints, @NotNull ImageProducer producer,
            @NotNull Kernel[] kernels, int kernelCount, int awtEdgeMode) {
        ImageProducer output = producer;
        for (int i = 0; i < kernelCount; i++) {
            BufferedImageFilter filter = new BufferedImageFilter(
                    new ConvolveOp(kernels[i], awtEdgeMode, hints));
            output = new FilteredImageSource(output, filter);
        }
        return output;
    }

}
