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
package com.github.weisj.jsvg.attribute;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.AffineTransform;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.paint.impl.DefaultPaintParser;
import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

class PreserveAspectRatioTest {

    private static final AttributeParser PARSER = new AttributeParser(new DefaultPaintParser());

    private static PreserveAspectRatio parse(String value) {
        return PreserveAspectRatio.parse(value, PARSER);
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    @Test
    void testParseDefault() {
        // null → xMidYMid Meet
        PreserveAspectRatio par = PreserveAspectRatio.parse(null, PARSER);
        assertEquals(PreserveAspectRatio.Align.xMidYMid, par.align);
        assertEquals(PreserveAspectRatio.MeetOrSlice.Meet, par.meetOrSlice);
    }

    @Test
    void testParseNone() {
        PreserveAspectRatio par = parse("none");
        assertEquals(PreserveAspectRatio.Align.None, par.align);
        assertEquals(PreserveAspectRatio.MeetOrSlice.Meet, par.meetOrSlice);
    }

    @Test
    void testParseXMinYMin() {
        PreserveAspectRatio par = parse("xMinYMin");
        assertEquals(PreserveAspectRatio.Align.xMinYMin, par.align);
        assertEquals(PreserveAspectRatio.MeetOrSlice.Meet, par.meetOrSlice);
    }

    @Test
    void testParseXMidYMidSlice() {
        PreserveAspectRatio par = parse("xMidYMid slice");
        assertEquals(PreserveAspectRatio.Align.xMidYMid, par.align);
        assertEquals(PreserveAspectRatio.MeetOrSlice.Slice, par.meetOrSlice);
    }

    @Test
    void testParseXMaxYMaxMeet() {
        PreserveAspectRatio par = parse("xMaxYMax meet");
        assertEquals(PreserveAspectRatio.Align.xMaxYMax, par.align);
        assertEquals(PreserveAspectRatio.MeetOrSlice.Meet, par.meetOrSlice);
    }

    @Test
    void testParseAllAlignValues() {
        for (PreserveAspectRatio.Align align : PreserveAspectRatio.Align.values()) {
            if (align == PreserveAspectRatio.Align.None) continue;
            PreserveAspectRatio par = parse(align.name());
            assertEquals(align, par.align);
        }
    }

    @Test
    void testParseFallback() {
        PreserveAspectRatio fallback = PreserveAspectRatio.parse("xMinYMin", PARSER);
        PreserveAspectRatio par = PreserveAspectRatio.parse(null, fallback, PARSER);
        assertEquals(fallback, par);
    }

    @Test
    void testParseTooManyArgumentsThrows() {
        assertThrows(IllegalArgumentException.class, () -> parse("xMidYMid meet extra"));
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    void testEqualsAndHashCode() {
        PreserveAspectRatio a = parse("xMidYMid meet");
        PreserveAspectRatio b = parse("xMidYMid meet");
        PreserveAspectRatio c = parse("xMinYMin slice");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void testEqualsWithNull() {
        PreserveAspectRatio par = parse("xMidYMid");
        assertNotEquals(null, par);
    }

    // -------------------------------------------------------------------------
    // factory methods
    // -------------------------------------------------------------------------

    @Test
    void testNoneFactory() {
        PreserveAspectRatio par = PreserveAspectRatio.none();
        assertEquals(PreserveAspectRatio.Align.None, par.align);
    }

    @Test
    void testForDisplayFactory() {
        PreserveAspectRatio par = PreserveAspectRatio.forDisplay();
        assertEquals(PreserveAspectRatio.Align.xMidYMid, par.align);
        assertEquals(PreserveAspectRatio.MeetOrSlice.Meet, par.meetOrSlice);
    }

    // -------------------------------------------------------------------------
    // computeViewportTransform – none (non-uniform scaling)
    // -------------------------------------------------------------------------

    @Test
    void testComputeViewportTransformNone() {
        // none → scale to fit exactly, no uniform scaling
        PreserveAspectRatio par = PreserveAspectRatio.none();
        FloatSize size = new FloatSize(200, 100);
        ViewBox viewBox = new ViewBox(0, 0, 100, 50);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        // Both axes should scale independently: scaleX=2, scaleY=2
        double[] m = new double[6];
        tx.getMatrix(m);
        assertEquals(2.0, m[0], 0.001); // scaleX
        assertEquals(2.0, m[3], 0.001); // scaleY
    }

    @Test
    void testComputeViewportTransformNoneNonUniform() {
        // none with different aspect ratios → non-uniform scaling
        PreserveAspectRatio par = PreserveAspectRatio.none();
        FloatSize size = new FloatSize(300, 100);
        ViewBox viewBox = new ViewBox(0, 0, 100, 50);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        double[] m = new double[6];
        tx.getMatrix(m);
        assertEquals(3.0, m[0], 0.001); // scaleX
        assertEquals(2.0, m[3], 0.001); // scaleY
    }

    // -------------------------------------------------------------------------
    // computeViewportTransform – meet (fit inside, uniform scale)
    // -------------------------------------------------------------------------

    @Test
    void testComputeViewportTransformMeetXMidYMid() {
        // viewport: 200×100, viewBox: 100×100 → meet: uniform scale = min(2, 1) = 1
        PreserveAspectRatio par = parse("xMidYMid meet");
        FloatSize size = new FloatSize(200, 100);
        ViewBox viewBox = new ViewBox(0, 0, 100, 100);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        double[] m = new double[6];
        tx.getMatrix(m);
        // uniform scale = 1 (limited by height)
        assertEquals(1.0, m[0], 0.001);
        assertEquals(1.0, m[3], 0.001);
        // xMid: translateX = (200 - 100*1) / 2 = 50
        assertEquals(50.0, m[4], 0.001);
        // yMid: translateY = (100 - 100*1) / 2 = 0
        assertEquals(0.0, m[5], 0.001);
    }

    @Test
    void testComputeViewportTransformMeetXMinYMin() {
        PreserveAspectRatio par = parse("xMinYMin meet");
        FloatSize size = new FloatSize(200, 100);
        ViewBox viewBox = new ViewBox(0, 0, 100, 100);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        double[] m = new double[6];
        tx.getMatrix(m);
        assertEquals(1.0, m[0], 0.001);
        assertEquals(1.0, m[3], 0.001);
        // xMin: translateX = 0
        assertEquals(0.0, m[4], 0.001);
        // yMin: translateY = 0
        assertEquals(0.0, m[5], 0.001);
    }

    @Test
    void testComputeViewportTransformMeetXMaxYMax() {
        PreserveAspectRatio par = parse("xMaxYMax meet");
        FloatSize size = new FloatSize(200, 100);
        ViewBox viewBox = new ViewBox(0, 0, 100, 100);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        double[] m = new double[6];
        tx.getMatrix(m);
        // xMax: translateX = 200 - 100 = 100
        assertEquals(100.0, m[4], 0.001);
        // yMax: translateY = 100 - 100 = 0
        assertEquals(0.0, m[5], 0.001);
    }

    // -------------------------------------------------------------------------
    // computeViewportTransform – slice (fill outside, uniform scale)
    // -------------------------------------------------------------------------

    @Test
    void testComputeViewportTransformSliceXMidYMid() {
        // viewport: 200×100, viewBox: 100×100 → slice: uniform scale = max(2, 1) = 2
        PreserveAspectRatio par = parse("xMidYMid slice");
        FloatSize size = new FloatSize(200, 100);
        ViewBox viewBox = new ViewBox(0, 0, 100, 100);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        double[] m = new double[6];
        tx.getMatrix(m);
        assertEquals(2.0, m[0], 0.001);
        assertEquals(2.0, m[3], 0.001);
        // xMid: (200 - 100*2) / 2 = 0
        assertEquals(0.0, m[4], 0.001);
        // yMid: (100 - 100*2) / 2 = -50
        assertEquals(-50.0, m[5], 0.001);
    }

    // -------------------------------------------------------------------------
    // computeViewportTransform – viewBox with non-zero origin
    // -------------------------------------------------------------------------

    @Test
    void testComputeViewportTransformWithViewBoxOffset() {
        PreserveAspectRatio par = parse("xMinYMin meet");
        FloatSize size = new FloatSize(100, 100);
        ViewBox viewBox = new ViewBox(10, 20, 100, 100);
        AffineTransform tx = par.computeViewportTransform(size, viewBox);
        // Should translate by -x, -y of viewBox
        double[] m = new double[6];
        tx.getMatrix(m);
        // scale = 1, translateX = -10, translateY = -20
        assertEquals(1.0, m[0], 0.001);
        assertEquals(-10.0, m[4], 0.001);
        assertEquals(-20.0, m[5], 0.001);
    }
}
