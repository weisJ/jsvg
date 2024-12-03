/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jannis Weis
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
package com.github.weisj.jsvg.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.paint.AwtSVGPaint;
import com.github.weisj.jsvg.attributes.paint.PredefinedPaints;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;

class PaintParserTest {

    private static final @NotNull AttributeNode attributeNode = dummyAttributeNode();

    private static @NotNull AttributeNode dummyAttributeNode() {
        return ParserTestUtil.createDummyAttributeNode(Collections.emptyMap());
    }

    private static SVGPaint parsePaint(@NotNull String value) {
        return attributeNode.parser().parsePaint(value, attributeNode);
    }

    private static @NotNull SVGPaint colorFromFloat(float r, float g, float b, float a) {
        return color(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    private static int clamp(float p) {
        return Math.min(255, Math.max(0, (int) (p * 255)));
    }

    private static @NotNull SVGPaint color(int r, int g, int b, float a) {
        return color(r, g, b, clamp(a));
    }

    private static @NotNull SVGPaint color(int r, int g, int b, int a) {
        return new AwtSVGPaint(new Color(r, g, b, a));
    }

    @Test
    void testRgbLiteral() {
        assertEquals(color(111, 203, 255, 1f), parsePaint("rgb(111,203,255)"));
        assertEquals(color(111, 203, 255, 1f), parsePaint("Rgb(111 203 255)"));
        assertEquals(color(10, 20, 30, 1f), parsePaint("rGb(10, 20, 30, 0.4)"));
        assertEquals(color(10, 20, 30, 1f), parsePaint("rgB(10 20 30 0.4)"));

        assertEquals(colorFromFloat(0.1f, 0.2f, 0.3f, 1f), parsePaint("rgb(10% 20% 30%)"));
        assertEquals(colorFromFloat(0.4f, 0.5f, 0.6f, 1f), parsePaint("rGb(40%,50%,60%)"));
        assertEquals(colorFromFloat(0.7f, 0.8f, 0.9f, 1f), parsePaint("rgB(70%,80% 90%)"));
    }

    @Test
    void testRgbaLiteral() {
        assertEquals(color(111, 203, 255, 1f), parsePaint("rgba(111,203,255)"));
        assertEquals(color(111, 203, 255, 1f), parsePaint("Rgba(111,203,255)"));
        assertEquals(color(111, 203, 255, 1f), parsePaint("rGba(111 203 255)"));
        assertEquals(color(10, 20, 30, 0.4f), parsePaint("rgBa(10, 20, 30, 0.4)"));
        assertEquals(color(10, 20, 30, 0.4f), parsePaint("rgbA(10 20 30 0.4)"));

        assertEquals(colorFromFloat(0.1f, 0.2f, 0.3f, 0.12f), parsePaint("rgba(10% 20% 30% 12%)"));
        assertEquals(colorFromFloat(0.4f, 0.5f, 0.6f, 0.34f), parsePaint("rGba(40%,50%,60% 34%)"));
        assertEquals(colorFromFloat(0.7f, 0.8f, 0.9f, 0.56f), parsePaint("rgBa(70%,80% 90% 56%)"));
    }

    @Test
    void testHexLiteral() {
        assertEquals(color(255, 0, 0, 255), parsePaint("#F00"));
        assertEquals(color(255, 0, 0, 255), parsePaint("#f00"));
        assertEquals(color(255, 0, 0, 255), parsePaint("#F00F"));
        assertEquals(color(255, 0, 0, 255), parsePaint("#F00f"));
        assertEquals(color(255, 0, 0, 17), parsePaint("#F001"));
        assertEquals(color(255, 0, 0, 17), parsePaint("#f001"));
        assertEquals(color(0, 241, 0, 18), parsePaint("#00F10012"));
        assertEquals(color(0, 241, 0, 18), parsePaint("#00f10012"));
    }

    @Test
    void testSpecialColors() {
        assertEquals(PredefinedPaints.NONE, parsePaint("none"));
        assertEquals(PredefinedPaints.NONE, parsePaint("transparent"));
        assertEquals(PredefinedPaints.CURRENT_COLOR, parsePaint("currentcolor"));
        assertEquals(PredefinedPaints.CONTEXT_FILL, parsePaint("context-fill"));
        assertEquals(PredefinedPaints.CONTEXT_STROKE, parsePaint("context-stroke"));
    }

    @Test
    void testNamedColors() {
        assertEquals(color(0, 0, 0, 1f), parsePaint("black"));
        assertEquals(color(255, 0, 0, 1f), parsePaint("red"));
        assertEquals(color(0, 128, 0, 1f), parsePaint("green"));
        assertEquals(color(0, 0, 255, 1f), parsePaint("blue"));
    }
}
