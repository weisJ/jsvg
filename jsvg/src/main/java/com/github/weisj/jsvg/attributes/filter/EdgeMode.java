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
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import com.github.weisj.jsvg.util.ImageUtil;

public enum EdgeMode {
    Duplicate,
    Wrap,
    None;

    public enum Axis {
        Horizontal,
        Vertical;

        private @NotNull Axis other() {
            return this == Horizontal ? Vertical : Horizontal;
        }
    }

    public interface SeparableConvolveOperation {

        int radius(@NotNull Axis axis);

        @NotNull
        BufferedImage convolve(@NotNull BufferedImage image, @NotNull Axis axis, @Nullable RenderingHints hints);

        default @NotNull Axis firstAxis() {
            return Axis.Horizontal;
        }
    }

    public @NotNull BufferedImage convolveSeparable(@NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @Nullable RenderingHints hints,
            @NotNull SeparableConvolveOperation operation) {
        if (!image.getColorModel().isAlphaPremultiplied()) {
            throw new IllegalArgumentException("Image must be premultiplied");
        }
        if (!ImageUtil.is_INT_PACK_Data(image.getSampleModel(), true)) {
            throw new IllegalArgumentException("Image must use packed integer ARGB samples");
        }

        Rectangle resultBounds = resultBounds(image);
        if (resultBounds.isEmpty()) return image;

        int horizontalRadius = operation.radius(Axis.Horizontal);
        int verticalRadius = operation.radius(Axis.Vertical);
        if (horizontalRadius == 0 && verticalRadius == 0) return image;

        int horizontalWidth = paddedDimension(resultBounds.width, horizontalRadius);
        int verticalHeight = paddedDimension(resultBounds.height, verticalRadius);
        int backingBufferSize = checkedBackingBufferSize(
                (long) horizontalWidth * resultBounds.height,
                (long) resultBounds.width * verticalHeight);
        // Shared backing storage for the padded image to avoid allocating a new array for each axis pass.
        int[] backingBuffer = new int[backingBufferSize];

        Axis firstAxis = operation.firstAxis();
        int firstRadius = radiusForAxis(firstAxis, horizontalRadius, verticalRadius);
        int secondRadius = radiusForAxis(firstAxis.other(), horizontalRadius, verticalRadius);
        if (firstRadius == 0 && secondRadius > 0) {
            // A zero-radius pass produces no intermediate result.
            // It is convenient to just run the non-zero pass first, so swap the axes and radii.
            firstAxis = firstAxis.other();
            firstRadius = secondRadius;
            secondRadius = 0;
        }

        // sourceBounds is possibly smaller than resultBounds.
        // We only copy the necessary row/column from the sourceBounds which is enough to process the
        // sourceBounds.
        // After the first axis pass we need to copy the complete resultBounds row/column to process the
        // second axis.
        AxisConvolutionContext axisContext =
                new AxisConvolutionContext(resultBounds, hints, operation, backingBuffer);
        convolveAxis(axisContext, image, sourceBounds, firstAxis, firstRadius);
        if (secondRadius > 0) {
            Rectangle intermediateBounds = sourceBoundsAfterAxis(sourceBounds, resultBounds, firstAxis);
            convolveAxis(axisContext, image, intermediateBounds, firstAxis.other(), secondRadius);
        }
        return image;
    }

    private static @NotNull Rectangle sourceBoundsAfterAxis(@NotNull Rectangle sourceBounds,
            @NotNull Rectangle resultBounds, @NotNull Axis processedAxis) {
        Rectangle bounds = new Rectangle(sourceBounds);
        if (processedAxis == Axis.Horizontal) {
            bounds.x = resultBounds.x;
            bounds.width = resultBounds.width;
        } else {
            bounds.y = resultBounds.y;
            bounds.height = resultBounds.height;
        }
        return bounds;
    }

    private static int radiusForAxis(@NotNull Axis axis, int horizontalRadius, int verticalRadius) {
        return axis == Axis.Horizontal ? horizontalRadius : verticalRadius;
    }

    private static final class AxisConvolutionContext {
        private final @NotNull Rectangle resultBounds;
        private final @Nullable RenderingHints hints;
        private final @NotNull SeparableConvolveOperation operation;
        private final int @NotNull [] backingBuffer;


        private AxisConvolutionContext(@NotNull Rectangle resultBounds, @Nullable RenderingHints hints,
                @NotNull SeparableConvolveOperation operation, int @NotNull [] backingBuffer) {
            this.resultBounds = resultBounds;
            this.hints = hints;
            this.operation = operation;
            this.backingBuffer = backingBuffer;
        }
    }

    private void convolveAxis(@NotNull AxisConvolutionContext axisContext,
            @NotNull BufferedImage unpaddedImage,
            @NotNull Rectangle sourceBounds, @NotNull Axis axis, int radius) {
        if (radius == 0) return;

        Rectangle resultBounds = axisContext.resultBounds;

        int horizontalRadius = axis == Axis.Horizontal ? radius : 0;
        int verticalRadius = axis == Axis.Vertical ? radius : 0;
        EdgeModeImage extended = createExtendedImage(axisContext, unpaddedImage, sourceBounds,
                horizontalRadius, verticalRadius);

        BufferedImage filteredImage = axisContext.operation.convolve(extended.img, axis, axisContext.hints);
        if (filteredImage.getWidth() != extended.img.getWidth()
                || filteredImage.getHeight() != extended.img.getHeight()) {
            throw new IllegalArgumentException("Convolution must preserve the padded image dimensions");
        }
        Raster unpaddedResult = filteredImage.getRaster().createChild(
                extended.xOff, extended.yOff, resultBounds.width, resultBounds.height,
                resultBounds.x, resultBounds.y, null);
        unpaddedImage.getRaster().setRect(unpaddedResult);
    }

    private static @NotNull Rectangle resultBounds(@NotNull BufferedImage source) {
        return new Rectangle(0, 0, source.getWidth(), source.getHeight());
    }

    private @NotNull EdgeModeImage createExtendedImage(@NotNull AxisConvolutionContext axisContext,
            @NotNull BufferedImage source,
            @NotNull Rectangle sourceBounds,
            int horizontalRadius, int verticalRadius) {
        Rectangle resultBounds = axisContext.resultBounds;
        int paddedWidth = paddedDimension(resultBounds.width, horizontalRadius);
        int paddedHeight = paddedDimension(resultBounds.height, verticalRadius);
        int paddedSize = checkedImageSize((long) paddedWidth * paddedHeight);
        int[] buffer = axisContext.backingBuffer;
        if (buffer.length < paddedSize) {
            throw new IllegalArgumentException("Reusable buffer is too small for the padded image");
        }
        Arrays.fill(buffer, 0, paddedSize, 0);

        int[] masks = ((SinglePixelPackedSampleModel) source.getSampleModel()).getBitMasks();
        BufferedImage image = createImageView(source.getColorModel(), buffer, masks, paddedWidth, paddedHeight);

        Rectangle extendedBounds = new Rectangle(sourceBounds);
        extendedBounds.translate(horizontalRadius - resultBounds.x, verticalRadius - resultBounds.y);
        Graphics2D graphics = GraphicsUtil.createGraphics(image);
        try {
            // Align the complete available raster with its global device coordinates. If resultBounds
            // was clipped by the backing image, unavailable samples stay transparent rather than
            // becoming an artificial input edge.
            graphics.drawImage(source,
                    horizontalRadius - resultBounds.x,
                    verticalRadius - resultBounds.y, null);
            if (this != None && !extendedBounds.isEmpty()) {
                if (horizontalRadius > 0) {
                    paintExtendedAxis(graphics, image, extendedBounds, Axis.Horizontal);
                    extendedBounds.x = 0;
                    extendedBounds.width = paddedWidth;
                }
                if (verticalRadius > 0) {
                    paintExtendedAxis(graphics, image, extendedBounds, Axis.Vertical);
                }
            }
        } finally {
            graphics.dispose();
        }
        return new EdgeModeImage(image, horizontalRadius, verticalRadius);
    }

    private void paintExtendedAxis(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @NotNull Axis axis) {
        if (this == Duplicate) {
            paintDuplicateEdges(graphics, image, sourceBounds, axis);
        } else {
            paintRepeatedEdges(graphics, image, sourceBounds, axis);
        }
    }

    private static void paintDuplicateEdges(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @NotNull Axis axis) {
        int left = sourceBounds.x;
        int top = sourceBounds.y;
        int right = left + sourceBounds.width;
        int bottom = top + sourceBounds.height;
        Insets insets = new Insets(top, left, bottom, right);
        if (axis == Axis.Horizontal) {
            paintHorizontalDuplicateEdges(graphics, image, insets);
        } else {
            paintVerticalDuplicateEdges(graphics, image, insets);
        }
    }

    private static void paintHorizontalDuplicateEdges(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Insets insets) {
        int clippedTop = Math.max(0, insets.top);
        int clippedBottom = Math.min(image.getHeight(), insets.bottom);
        if (clippedTop >= clippedBottom) return;

        int left = insets.left;
        int right = insets.right;
        if (left > 0 && left < image.getWidth()) {
            graphics.drawImage(image, 0, clippedTop, left, clippedBottom,
                    left, clippedTop, left + 1, clippedBottom, null);
        }
        if (right >= 1 && right < image.getWidth()) {
            graphics.drawImage(image, right, clippedTop, image.getWidth(), clippedBottom,
                    right - 1, clippedTop, right, clippedBottom, null);
        }
    }

    private static void paintVerticalDuplicateEdges(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Insets insets) {
        int clippedLeft = Math.max(0, insets.left);
        int clippedRight = Math.min(image.getWidth(), insets.right);
        if (clippedLeft >= clippedRight) return;

        int top = insets.top;
        int bottom = insets.bottom;
        if (top > 0 && top < image.getHeight()) {
            graphics.drawImage(image, clippedLeft, 0, clippedRight, top,
                    clippedLeft, top, clippedRight, top + 1, null);
        }
        if (bottom >= 1 && bottom < image.getHeight()) {
            graphics.drawImage(image, clippedLeft, bottom, clippedRight, image.getHeight(),
                    clippedLeft, bottom - 1, clippedRight, bottom, null);
        }
    }

    private static void paintRepeatedEdges(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @NotNull Axis axis) {
        if (axis == Axis.Horizontal) {
            int firstCopy = Math.floorDiv(-sourceBounds.x, sourceBounds.width);
            for (int copy = firstCopy; sourceBounds.x + copy * sourceBounds.width < image.getWidth(); copy++) {
                if (copy == 0) continue;
                graphics.drawImage(image,
                        sourceBounds.x + copy * sourceBounds.width, sourceBounds.y,
                        sourceBounds.x + (copy + 1) * sourceBounds.width, sourceBounds.y + sourceBounds.height,
                        sourceBounds.x, sourceBounds.y,
                        sourceBounds.x + sourceBounds.width, sourceBounds.y + sourceBounds.height, null);
            }
        } else {
            int firstCopy = Math.floorDiv(-sourceBounds.y, sourceBounds.height);
            for (int copy = firstCopy; sourceBounds.y + copy * sourceBounds.height < image.getHeight(); copy++) {
                if (copy == 0) continue;
                graphics.drawImage(image,
                        sourceBounds.x, sourceBounds.y + copy * sourceBounds.height,
                        sourceBounds.x + sourceBounds.width, sourceBounds.y + (copy + 1) * sourceBounds.height,
                        sourceBounds.x, sourceBounds.y,
                        sourceBounds.x + sourceBounds.width, sourceBounds.y + sourceBounds.height, null);
            }
        }
    }

    private static @NotNull BufferedImage createImageView(@NotNull ColorModel colorModel,
            int @NotNull [] workspace, int @NotNull [] masks, int width, int height) {
        DataBufferInt dataBuffer = new DataBufferInt(workspace, workspace.length);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT, width, height, width, masks);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point());
        return new BufferedImage(colorModel, raster, true, null);
    }

    private static int paddedDimension(int size, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Convolution radius must not be negative");
        }
        long paddedSize = size + 2L * radius;
        if (paddedSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Padded image dimension exceeds the supported integer range");
        }
        return (int) paddedSize;
    }

    private static int checkedBackingBufferSize(long horizontalSize, long verticalSize) {
        return checkedImageSize(Math.max(horizontalSize, verticalSize));
    }

    private static int checkedImageSize(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Padded image requires too many pixels");
        }
        return (int) size;
    }

    private static final class EdgeModeImage {
        private final @NotNull BufferedImage img;
        private final int xOff;
        private final int yOff;

        private EdgeModeImage(@NotNull BufferedImage img, int xOff, int yOff) {
            this.img = img;
            this.xOff = xOff;
            this.yOff = yOff;
        }
    }

}
