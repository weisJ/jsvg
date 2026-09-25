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
    Mirror,
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

    public interface ConvolveOperation {

        /**
         * Convolves {@code image} and returns the pixels in {@code resultRegion}. The source image
         * already contains the edge-mode extension required around that region.
         */
        @NotNull
        BufferedImage convolve(@NotNull BufferedImage image, @NotNull Rectangle resultRegion,
                @Nullable RenderingHints hints);
    }

    public @NotNull BufferedImage convolve(@NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @NotNull Insets samplingInsets,
            @Nullable RenderingHints hints, @NotNull ConvolveOperation operation) {
        validateInputImage(image);
        validateInsets(samplingInsets);

        Rectangle resultBounds = resultBounds(image);
        if (resultBounds.isEmpty()) return image;

        int paddedWidth = paddedDimension(resultBounds.width, samplingInsets.left, samplingInsets.right);
        int paddedHeight = paddedDimension(resultBounds.height, samplingInsets.top, samplingInsets.bottom);
        int[] buffer = new int[checkedImageSize((long) paddedWidth * paddedHeight)];
        EdgeModeImage extended = createExtendedImage(image, sourceBounds, resultBounds, samplingInsets, buffer);
        Rectangle resultRegion = new Rectangle(
                extended.xOff, extended.yOff, resultBounds.width, resultBounds.height);
        BufferedImage result = operation.convolve(extended.img, resultRegion, hints);
        if (result.getWidth() != resultBounds.width || result.getHeight() != resultBounds.height) {
            throw new IllegalArgumentException("Convolution result must have the unpadded image dimensions");
        }
        return result;
    }

    public @NotNull BufferedImage convolveSeparable(@NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @Nullable RenderingHints hints,
            @NotNull SeparableConvolveOperation operation) {
        validateInputImage(image);

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
        Insets samplingInsets = new Insets(verticalRadius, horizontalRadius, verticalRadius, horizontalRadius);
        EdgeModeImage extended = createExtendedImage(unpaddedImage, sourceBounds, resultBounds,
                samplingInsets, axisContext.backingBuffer);

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

    private @NotNull EdgeModeImage createExtendedImage(@NotNull BufferedImage source,
            @NotNull Rectangle sourceBounds, @NotNull Rectangle resultBounds,
            @NotNull Insets samplingInsets, int @NotNull [] buffer) {
        int paddedWidth = paddedDimension(resultBounds.width, samplingInsets.left, samplingInsets.right);
        int paddedHeight = paddedDimension(resultBounds.height, samplingInsets.top, samplingInsets.bottom);
        int paddedSize = checkedImageSize((long) paddedWidth * paddedHeight);
        if (buffer.length < paddedSize) {
            throw new IllegalArgumentException("Reusable buffer is too small for the padded image");
        }
        Arrays.fill(buffer, 0, paddedSize, 0);

        int[] masks = ((SinglePixelPackedSampleModel) source.getSampleModel()).getBitMasks();
        BufferedImage image = createImageView(source.getColorModel(), buffer, masks, paddedWidth, paddedHeight);

        Rectangle extendedBounds = new Rectangle(sourceBounds);
        extendedBounds.translate(samplingInsets.left - resultBounds.x,
                samplingInsets.top - resultBounds.y);
        Graphics2D graphics = GraphicsUtil.createGraphics(image);
        try {
            // Align the complete available raster with its global device coordinates. If resultBounds
            // was clipped by the backing image, unavailable samples stay transparent rather than
            // becoming an artificial input edge.
            graphics.drawImage(source,
                    samplingInsets.left - resultBounds.x,
                    samplingInsets.top - resultBounds.y, null);
            if (this != None && !extendedBounds.isEmpty()) {
                if (samplingInsets.left > 0 || samplingInsets.right > 0) {
                    paintExtendedAxis(graphics, image, extendedBounds, Axis.Horizontal);
                    extendedBounds.x = 0;
                    extendedBounds.width = paddedWidth;
                }
                if (samplingInsets.top > 0 || samplingInsets.bottom > 0) {
                    paintExtendedAxis(graphics, image, extendedBounds, Axis.Vertical);
                }
            }
        } finally {
            graphics.dispose();
        }
        return new EdgeModeImage(image, samplingInsets.left, samplingInsets.top);
    }

    private void paintExtendedAxis(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @NotNull Axis axis) {
        if (this == Duplicate) {
            paintDuplicateEdges(graphics, image, sourceBounds, axis);
        } else if (this == Wrap) {
            paintRepeatedEdges(graphics, image, sourceBounds, axis);
        } else if (this == Mirror) {
            paintMirroredEdges(graphics, image, sourceBounds, axis);
        } else {
            throw new IllegalStateException("Cannot extend edges for mode " + this);
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

    private static void paintMirroredEdges(@NotNull Graphics2D graphics, @NotNull BufferedImage image,
            @NotNull Rectangle sourceBounds, @NotNull Axis axis) {
        if (axis == Axis.Horizontal) {
            int top = Math.max(0, sourceBounds.y);
            int bottom = Math.min(image.getHeight(), sourceBounds.y + sourceBounds.height);
            if (top >= bottom) return;
            for (int x = 0; x < image.getWidth(); x++) {
                if (x >= sourceBounds.x && x < sourceBounds.x + sourceBounds.width) continue;
                int sourceX = mirroredCoordinate(x, sourceBounds.x, sourceBounds.width);
                graphics.drawImage(image, x, top, x + 1, bottom,
                        sourceX, top, sourceX + 1, bottom, null);
            }
        } else {
            int left = Math.max(0, sourceBounds.x);
            int right = Math.min(image.getWidth(), sourceBounds.x + sourceBounds.width);
            if (left >= right) return;
            for (int y = 0; y < image.getHeight(); y++) {
                if (y >= sourceBounds.y && y < sourceBounds.y + sourceBounds.height) continue;
                int sourceY = mirroredCoordinate(y, sourceBounds.y, sourceBounds.height);
                graphics.drawImage(image, left, y, right, y + 1,
                        left, sourceY, right, sourceY + 1, null);
            }
        }
    }

    private static int mirroredCoordinate(int coordinate, int start, int length) {
        long period = 2L * length;
        long offset = Math.floorMod((long) coordinate - start, period);
        if (offset >= length) offset = period - 1 - offset;
        return start + (int) offset;
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
        return paddedDimension(size, radius, radius);
    }

    private static int paddedDimension(int size, int before, int after) {
        if (before < 0 || after < 0) {
            throw new IllegalArgumentException("Convolution padding must not be negative");
        }
        long paddedSize = size + (long) before + after;
        if (paddedSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Padded image dimension exceeds the supported integer range");
        }
        return (int) paddedSize;
    }

    private static void validateInsets(@NotNull Insets insets) {
        if (insets.top < 0 || insets.left < 0 || insets.bottom < 0 || insets.right < 0) {
            throw new IllegalArgumentException("Convolution padding must not be negative");
        }
    }

    private static void validateInputImage(@NotNull BufferedImage image) {
        if (!image.getColorModel().isAlphaPremultiplied()) {
            throw new IllegalArgumentException("Image must be premultiplied");
        }
        if (!ImageUtil.is_INT_PACK_Data(image.getSampleModel(), true)) {
            throw new IllegalArgumentException("Image must use packed integer ARGB samples");
        }
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
