/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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
import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static com.github.weisj.jsvg.ReferenceTest.render;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.paint.AwtSVGPaint;

class PaintsTest {

    @Test
    void testCurrentColor() {
        assertEquals(SUCCESS, compareImages("paint/currentColor.svg"));
    }

    @Test
    void testContextColors() {
        BufferedImage img = render("paint/context.svg");
        assertEquals(new Color(0, 255, 0), new Color(img.getRGB(25, 25)));
        assertEquals(new Color(255, 0, 0), new Color(img.getRGB(125, 25)));
        assertEquals(new Color(0, 0, 255), new Color(img.getRGB(225, 25)));
        assertEquals(new Color(255, 255, 0), new Color(img.getRGB(325, 25)));
    }

    @Test
    void testStringRepresentation() {
        assertEquals("AwtSVGPaint{paint=Color{r=0,g=0,b=0,a=255}}", new AwtSVGPaint(Color.BLACK).toString());
    }
}
