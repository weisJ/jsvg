/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.Graphics2DOutput;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

class ToShapeTest {

    @Test
    void testStroke() {
        assertEquals(SUCCESS, compareShape("stroke/stroke1.svg"));
        assertEquals(SUCCESS, compareShape("stroke/stroke2.svg"));
        assertEquals(SUCCESS, compareShape("stroke/stroke3.svg"));
    }

    @Test
    void testGradient() {
        assertEquals(SUCCESS, compareShape("gradient/linearGradient.svg"));
        assertEquals(SUCCESS, compareShape("gradient/radialGradient.svg"));
    }

    @Test
    void testAspectRatio() {
        assertEquals(SUCCESS, compareShape("aspect/aspect.svg"));
    }

    @Test
    void testMarker() {
        assertEquals(SUCCESS, compareShape("marker/marker1.svg"));
        assertEquals(SUCCESS, compareShape("marker/marker2.svg"));
    }

    @Test
    void testMeshGradient() {
        assertEquals(SUCCESS, compareShape("mesh/mesh.svg", 0.6f));
    }

    @Test
    void testPath() {
        assertEquals(SUCCESS, compareShape("path/closePath.svg"));
        assertEquals(SUCCESS, compareShape("path/cubicBezier.svg"));
        assertEquals(SUCCESS, compareShape("path/ellipticalArc.svg"));
        assertEquals(SUCCESS, compareShape("path/lineTo.svg"));
        assertEquals(SUCCESS, compareShape("path/moveTo.svg"));
        assertEquals(SUCCESS, compareShape("path/quadraticBezier.svg"));
    }

    @Test
    void testSymbol() {
        assertEquals(SUCCESS, compareShape("symbol/symbol1.svg"));
        assertEquals(SUCCESS, compareShape("symbol/symbol2.svg"));
    }

    @Test
    void testText() {
        assertEquals(SUCCESS, compareShape("text/text0.svg", 1f));
        assertEquals(SUCCESS, compareShape("text/text1.svg", 1f));
        assertEquals(SUCCESS, compareShape("text/textAnchor.svg", 1f));
        assertEquals(SUCCESS, compareShape("text/textLength.svg", 1f));
    }

    @Test
    void testTransform() {
        assertEquals(SUCCESS, compareShape("transform/matrix.svg"));
        assertEquals(SUCCESS, compareShape("transform/rotate.svg"));
        assertEquals(SUCCESS, compareShape("transform/scale.svg"));
        assertEquals(SUCCESS, compareShape("transform/skewX.svg"));
        assertEquals(SUCCESS, compareShape("transform/skewY.svg"));
        assertEquals(SUCCESS, compareShape("transform/SVGinSVG.svg"));
        assertEquals(SUCCESS, compareShape("transform/transform.svg"));
        assertEquals(SUCCESS, compareShape("transform/translate.svg"));
    }

    @Test
    void testUnits() {
        assertEquals(SUCCESS, compareShape("units/relativeUnits.svg"));
        assertEquals(SUCCESS, compareShape("units/relativeUnits2.svg"));
        assertEquals(SUCCESS, compareShape("units/relativeUnits3.svg"));
    }

    @Test
    void testVectorEffect() {
        assertEquals(SUCCESS, compareShape("vectorEffect/before.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/fixedPosition.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/none.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonRotation.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonRotationFixedPosition.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonScalingSize.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonScalingSizeFixedPosition.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonScalingSizeNonRotation.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonScalingSizeNonRotationFixedPosition.svg", 0.7f));
        assertEquals(SUCCESS, compareShape("vectorEffect/nonScalingStroke.svg", 0.7f));
    }

    @Test
    void testViewBox() {
        assertEquals(SUCCESS, compareShape("viewBox/viewBox.svg"));
        assertEquals(SUCCESS, compareShape("viewBox/viewBox2.svg"));
        assertEquals(SUCCESS, compareShape("viewBox/viewBox3.svg"));
    }

    @Test
    void testOther() {
        assertEquals(SUCCESS, compareShape("clipPath/clipPathUnits.svg"));
        assertEquals(SUCCESS, compareShape("fillRule.svg"));
        assertEquals(SUCCESS, compareShape("paintOrder/paintOrder.svg"));
    }

    private static @NotNull BufferedImage prepareImage(@NotNull SVGDocument document) {
        FloatSize size = document.size();
        int w = 2000;
        int h = Math.round(w * (size.height / size.width));
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return image;
    }

    private static @NotNull BufferedImage renderReference(@NotNull SVGDocument document) {
        BufferedImage img = prepareImage(document);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        document.renderWithPlatform(
                NullPlatformSupport.INSTANCE,
                new BlackAndWhiteOutput(g),
                new ViewBox(0, 0, img.getWidth(), img.getHeight()));
        g.dispose();
        return img;
    }

    private static @NotNull BufferedImage renderShape(@NotNull SVGDocument document) {
        BufferedImage img = prepareImage(document);
        Shape shape = document.computeShape(new ViewBox(0, 0, img.getWidth(), img.getHeight()));
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fill(shape);
        g.dispose();
        return img;
    }

    private static ReferenceTest.ReferenceTestResult compareShape(@NotNull String path) {
        return compareShape(path, 0.5f);
    }

    private static ReferenceTest.ReferenceTestResult compareShape(@NotNull String path, float tolerance) {
        try {
            URL url = Objects.requireNonNull(ReferenceTest.class.getResource(path), path);
            SVGDocument document = Objects.requireNonNull(new SVGLoader().load(url));
            BufferedImage expected = renderReference(document);
            BufferedImage actual = renderShape(document);
            return ReferenceTest.compareImageRasterization(expected, actual, path, tolerance, 0);
        } catch (Exception e) {
            Assertions.fail(e);
            return ReferenceTest.ReferenceTestResult.FAILURE;
        }
    }

    private static class BlackAndWhiteOutput extends Graphics2DOutput {

        public BlackAndWhiteOutput(@NotNull Graphics2D g) {
            super(g);
        }

        @Override
        public void setPaint(@NotNull Paint paint) {
            // Only black and white allowed
        }

        @Override
        public void setPaint(@NotNull Supplier<Paint> paintProvider) {
            // Only black and white allowed
        }

        @Override
        public void applyOpacity(float opacity) {
            if (opacity != 0) {
                opacity = 1;
            }
            super.applyOpacity(opacity);
        }

        @Override
        public @NotNull Output createChild() {
            return new BlackAndWhiteOutput((Graphics2D) graphics().create());
        }
    }
}
