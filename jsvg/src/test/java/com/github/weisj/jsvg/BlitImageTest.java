/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg;

import static com.github.weisj.jsvg.ReferenceTest.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.awt.NullPlatformSupport;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;

class BlitImageTest {

    private static class ImageSurface {
        private final @NotNull BufferedImage image;
        private final int scaleW;
        private final int scaleH;

        private ImageSurface(int vw, int vh, int scaleW, int scaleH) {
            this.image = ImageUtil.createCompatibleTransparentImage(scaleW * vw, scaleH * vh);;
            this.scaleW = scaleW;
            this.scaleH = scaleH;
        }

        @NotNull
        Output output() {
            Output output = new Graphics2DOutput(image.createGraphics());
            output.scale(scaleW, scaleH);
            output.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            output.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            return output;
        }

        public @NotNull BufferedImage image() {
            return image;
        }
    }

    @NotNull
    RenderContext createTestContext(int vw, int vh) {
        return RenderContext.createInitial(
                new NullPlatformSupport(),
                new MeasureContext(vw, vh,
                        SVGFont.defaultFontSize(),
                        SVGFont.exFromEm(SVGFont.defaultFontSize())));
    }

    void renderThroughBlitImage(@NotNull Output output, @NotNull RenderContext context,
            @NotNull Rectangle2D bounds, @NotNull Rectangle2D objectBounds,
            UnitType unitType, BiConsumer<Output, RenderContext> renderRoutine) {
        BlittableImage blitImage = BlittableImage.create(
                ImageUtil::createCompatibleTransparentImage, context, null,
                bounds, objectBounds, unitType);
        assertNotNull(blitImage);
        blitImage.render(output, renderRoutine);
        blitImage.blitTo(output);
    }

    void checkTransformAndShape(
            @NotNull AffineTransform transform,
            @NotNull Shape shape,
            @NotNull UnitType contentUnits) {
        int size = 100;
        int scale = 5;
        RenderContext context = createTestContext(size, size);

        Consumer<Output> renderShape = o -> {
            o.setPaint(Color.RED);
            o.fillShape(shape);
        };
        Rectangle2D objectBounds = new Rectangle2D.Double(25, 30, 50, 40);

        ImageSurface reference = new ImageSurface(size, size, scale, scale);
        Output referenceOutput = reference.output();
        referenceOutput.applyTransform(transform);
        referenceOutput.debugPaint(g -> {
            g.setColor(Color.GREEN);
            g.fill(objectBounds);
        });
        if (contentUnits == UnitType.ObjectBoundingBox) {
            referenceOutput.translate(objectBounds.getX(), objectBounds.getY());
            referenceOutput.scale(objectBounds.getWidth(), objectBounds.getHeight());
        }
        referenceOutput.debugPaint(g -> renderShape.accept(new Graphics2DOutput(g)));

        ImageSurface actual = new ImageSurface(size, size, scale, scale);
        Output actualOutput = actual.output();

        context.setRootTransform(actualOutput.transform());
        context.transform(actualOutput, transform);
        actualOutput.debugPaint(g -> {
            g.setColor(Color.GREEN);
            g.fill(objectBounds);
        });

        Rectangle2D shapeBounds = shape.getBounds2D();
        if (contentUnits == UnitType.ObjectBoundingBox) {
            shapeBounds = GeometryUtil.containingBoundsAfterTransform(
                    UnitType.ObjectBoundingBox.viewTransform(objectBounds), shapeBounds);
        }
        renderThroughBlitImage(actualOutput, context, shapeBounds, objectBounds, contentUnits,
                (o, ctx) -> renderShape.accept(o));

        assertEquals(SUCCESS, ReferenceTest.compareImageRasterization(reference.image(), actual.image(),
                "BlitImage", 0f));
    }

    @Test
    void testUserSpaceOnUse() {
        Rectangle rect = new Rectangle(25, 25, 25, 25);
        AffineTransform a = new AffineTransform();
        checkTransformAndShape(a, rect, UnitType.UserSpaceOnUse);
        a.translate(5, 7);
        checkTransformAndShape(a, rect, UnitType.UserSpaceOnUse);
        a.rotate(30);
        checkTransformAndShape(a, rect, UnitType.UserSpaceOnUse);
        a.shear(3, 0.5);
        checkTransformAndShape(a, rect, UnitType.UserSpaceOnUse);
    }

    @Test
    void testUserObjectBoundingBox() {
        Rectangle2D rect = new Rectangle2D.Double(0.1, 0.1, 0.5, 0.5);
        AffineTransform a = new AffineTransform();
        checkTransformAndShape(a, rect, UnitType.ObjectBoundingBox);
        a.translate(0.1, -0.05);
        checkTransformAndShape(a, rect, UnitType.ObjectBoundingBox);
        a.rotate(-15);
        checkTransformAndShape(a, rect, UnitType.ObjectBoundingBox);
        a.shear(0.75, 2.3);
        checkTransformAndShape(a, rect, UnitType.ObjectBoundingBox);
    }
}
