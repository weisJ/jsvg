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
package com.github.weisj.jsvg.renderer.jfx.impl.bridge;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;

import org.jetbrains.annotations.NotNull;

public final class FXImageBridge {

    private FXImageBridge() {}

    public static void drawImage(@NotNull GraphicsContext ctx, @NotNull BufferedImage awtImage, double currentOpacity) {
        ctx.drawImage(convertImageWithOpacity(awtImage, currentOpacity), 0, 0);
    }

    public static void drawImage(@NotNull GraphicsContext ctx, @NotNull Image awtImage, double currentOpacity) {
        ctx.drawImage(convertImageWithOpacity(awtImage, currentOpacity), 0, 0);
    }

    public static @NotNull WritableImage convertImage(@NotNull BufferedImage image) {
        return SwingFXUtils.toFXImage(image, null);
    }

    public static @NotNull WritableImage convertImageWithOpacity(@NotNull Image image, double globalOpacity) {
        boolean hasOpacity = globalOpacity < 1.0;
        if (image instanceof BufferedImage && !hasOpacity) {
            return convertImage((BufferedImage) image);
        }
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dst.createGraphics();
        if (hasOpacity) {
            Composite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) globalOpacity);
            graphics.setComposite(alphaComposite);
        }
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return convertImage(dst);
    }

    public static @NotNull BufferedImage convertRasterToBufferedImage(@NotNull ColorModel colorModel,
            @NotNull Raster raster) {
        BufferedImage image = new BufferedImage(colorModel, raster.createCompatibleWritableRaster(),
                colorModel.isAlphaPremultiplied(), null);
        image.setData(raster);
        return image;
    }
}
