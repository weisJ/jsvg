/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.PaintContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.util.ReferenceCounter;

public final class MaskedPaint implements Paint, GraphicsUtil.WrappingPaint, GraphicsUtil.ReferenceCountedPaint {
    private @NotNull Paint paint;
    private final @NotNull Raster maskRaster;
    private final @NotNull Point maskOffset;
    private final @Nullable ReferenceCounter referenceCounter;

    public MaskedPaint(@NotNull Paint paint, @NotNull Raster maskRaster, @NotNull Point2D maskOffset,
            @Nullable ReferenceCounter referenceCounter) {
        this.paint = paint;
        this.maskRaster = maskRaster;
        this.maskOffset = new Point((int) Math.floor(maskOffset.getX()), (int) Math.floor(maskOffset.getY()));
        this.referenceCounter = referenceCounter;
    }

    @Override
    public void setPaint(@NotNull Paint paint) {
        this.paint = GraphicsUtil.setupPaint(this.paint, paint);
    }

    @Override
    public @NotNull Paint paint() {
        return paint;
    }

    @Override
    public @Nullable ReferenceCounter referenceCounter() {
        return referenceCounter;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
            AffineTransform xform, RenderingHints hints) {
        PaintContext parentContext = paint.createContext(null, deviceBounds, userBounds, xform, hints);
        return new MaskPaintContext(parentContext, maskRaster, maskOffset);
    }

    @Override
    public int getTransparency() {
        return Transparency.TRANSLUCENT;
    }

    private static final class MaskPaintContext implements PaintContext {
        private final @NotNull PaintContext parentContext;
        private final @NotNull ColorModel colorModel;
        private final int numColorComponents;
        private final @NotNull ColorModel parentColorModel;
        private final @NotNull Raster maskRaster;
        private final @NotNull Point offset;

        MaskPaintContext(@NotNull PaintContext parentContext, @NotNull Raster maskRaster,
                @NotNull Point offset) {
            this.parentContext = parentContext;
            parentColorModel = parentContext.getColorModel();
            this.maskRaster = maskRaster;
            this.offset = offset;
            if (parentContext.getColorModel().hasAlpha()) {
                colorModel = parentColorModel;
            } else {
                colorModel = new ComponentColorModel(parentContext.getColorModel()
                        .getColorSpace(), true, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            }
            numColorComponents = colorModel.getNumColorComponents();
        }

        @Override
        public @NotNull ColorModel getColorModel() {
            return colorModel;
        }

        @Override
        public void dispose() {
            parentContext.dispose();
        }

        @Override
        public Raster getRaster(int x, int y, int w, int h) {
            Raster parentRaster = parentContext.getRaster(x, y, w, h);

            int parentMinX = parentRaster.getMinX();
            int parentMinY = parentRaster.getMinY();

            WritableRaster result;
            if (parentRaster instanceof WritableRaster) {
                if (parentColorModel.equals(colorModel)) {
                    result = parentRaster.createCompatibleWritableRaster();
                    result.setDataElements(-parentMinX, -parentMinY, parentRaster);
                } else {
                    BufferedImage parentImage = new BufferedImage(parentColorModel,
                            (WritableRaster) parentRaster,
                            parentColorModel.isAlphaPremultiplied(), null);
                    result = Raster.createWritableRaster(
                            colorModel.createCompatibleSampleModel(w, h), new Point(0, 0));
                    BufferedImage resultImage = new BufferedImage(colorModel, result, false, null);
                    Graphics graphics = resultImage.getGraphics();
                    graphics.drawImage(parentImage, 0, 0, null);
                    graphics.dispose();
                }
            } else {
                result = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h,
                        getColorModel().getNumComponents(), new Point(0, 0));
                ColorConvertOp colorConvertOp = new ColorConvertOp(
                        parentColorModel.getColorSpace(), colorModel.getColorSpace(), null);
                colorConvertOp.filter(parentRaster, result);
            }

            int softMaskMinX = maskRaster.getMinX();
            int softMaskMinY = maskRaster.getMinY();
            int softMaskMaxX = softMaskMinX + maskRaster.getWidth();
            int softMaskMaxY = softMaskMinY + maskRaster.getHeight();

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int rx = x + i - offset.x;
                    int ry = y + j - offset.y;

                    int alpha;
                    if ((rx >= softMaskMinX) && (rx < softMaskMaxX) && (ry >= softMaskMinY) && (ry < softMaskMaxY)) {
                        alpha = maskRaster.getSample(rx, ry, 0);
                    } else {
                        alpha = 0;
                    }
                    alpha = alpha * result.getSample(i, j, numColorComponents) / 255;
                    result.setSample(i, j, numColorComponents, alpha);
                }
            }

            return result;
        }
    }
}
