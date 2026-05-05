/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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
package com.github.weisj.jsvg.geometry.path;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


class PathParserTest {

    @Test
    void testAdjacentFlags() {
        PathCommand[] cmds = new PathParser("M0 0 a1 2 3 10 6 7").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[0] instanceof MoveTo);
        assertEquals(6, cmds[1].nodeCount());
    }

    @Test
    void testDotInExponent() {
        PathCommand[] cmds = new PathParser("M0 0 L 1e5.5").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[0] instanceof MoveTo);
        assertTrue(cmds[1] instanceof LineTo);
        LineTo cmd = (LineTo) cmds[1];
        assertEquals(cmd.x(), 1e5);
        assertEquals(cmd.y(), .5);
    }

    @Test
    void testRepeatedArcs() {
        PathCommand[] cmds =
                new PathParser("M0 0 a1 2 3 10 6 7 1 2 3 10 6 7 1 2 3 10 6 7 1 2 3 10 6 7").parsePathCommand();
        assertEquals(5, cmds.length);
        assertEquals(6, cmds[1].nodeCount());
    }

    // -------------------------------------------------------------------------
    // "none" special value
    // -------------------------------------------------------------------------

    @Test
    void testNoneProducesEmptyCommands() {
        PathCommand[] cmds = new PathParser("none").parsePathCommand();
        assertEquals(0, cmds.length);
    }

    // -------------------------------------------------------------------------
    // Move + Line commands
    // -------------------------------------------------------------------------

    @Test
    void testSimpleMoveAndLine() {
        PathCommand[] cmds = new PathParser("M10 20 L30 40").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[0] instanceof MoveTo);
        assertTrue(cmds[1] instanceof LineTo);
    }

    @Test
    void testRelativeMoveAndLine() {
        PathCommand[] cmds = new PathParser("m10 20 l30 40").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[0] instanceof MoveTo);
        assertTrue(cmds[1] instanceof LineTo);
        assertTrue(cmds[0].isRelative());
        assertTrue(cmds[1].isRelative());
    }

    @Test
    void testImplicitLinesAfterMove() {
        // After M, subsequent coordinate pairs produce implicit L commands
        PathCommand[] cmds = new PathParser("M0 0 10 20 30 40").parsePathCommand();
        assertEquals(3, cmds.length);
        assertTrue(cmds[0] instanceof MoveTo);
        assertTrue(cmds[1] instanceof LineTo);
        assertTrue(cmds[2] instanceof LineTo);
    }

    // -------------------------------------------------------------------------
    // Horizontal / Vertical line commands
    // -------------------------------------------------------------------------

    @Test
    void testHorizontalLine() {
        PathCommand[] cmds = new PathParser("M0 0 H50").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Horizontal);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeHorizontalLine() {
        PathCommand[] cmds = new PathParser("M0 0 h25").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Horizontal);
        assertTrue(cmds[1].isRelative());
    }

    @Test
    void testVerticalLine() {
        PathCommand[] cmds = new PathParser("M0 0 V50").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Vertical);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeVerticalLine() {
        PathCommand[] cmds = new PathParser("M0 0 v25").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Vertical);
        assertTrue(cmds[1].isRelative());
    }

    // -------------------------------------------------------------------------
    // Cubic Bezier commands
    // -------------------------------------------------------------------------

    @Test
    void testCubicBezier() {
        PathCommand[] cmds = new PathParser("M0 0 C10 20 30 40 50 60").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Cubic);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeCubicBezier() {
        PathCommand[] cmds = new PathParser("M0 0 c10 20 30 40 50 60").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Cubic);
        assertTrue(cmds[1].isRelative());
    }

    @Test
    void testCubicSmoothBezier() {
        PathCommand[] cmds = new PathParser("M0 0 S10 20 30 40").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof CubicSmooth);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeCubicSmoothBezier() {
        PathCommand[] cmds = new PathParser("M0 0 s10 20 30 40").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof CubicSmooth);
        assertTrue(cmds[1].isRelative());
    }

    // -------------------------------------------------------------------------
    // Quadratic Bezier commands
    // -------------------------------------------------------------------------

    @Test
    void testQuadraticBezier() {
        PathCommand[] cmds = new PathParser("M0 0 Q10 20 30 40").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Quadratic);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeQuadraticBezier() {
        PathCommand[] cmds = new PathParser("M0 0 q10 20 30 40").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Quadratic);
        assertTrue(cmds[1].isRelative());
    }

    @Test
    void testQuadraticSmoothBezier() {
        PathCommand[] cmds = new PathParser("M0 0 T10 20").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof QuadraticSmooth);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeQuadraticSmoothBezier() {
        PathCommand[] cmds = new PathParser("M0 0 t10 20").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof QuadraticSmooth);
        assertTrue(cmds[1].isRelative());
    }

    // -------------------------------------------------------------------------
    // Arc command
    // -------------------------------------------------------------------------

    @Test
    void testAbsoluteArc() {
        PathCommand[] cmds = new PathParser("M0 0 A10 10 0 0 1 50 50").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Arc);
        assertFalse(cmds[1].isRelative());
    }

    @Test
    void testRelativeArc() {
        PathCommand[] cmds = new PathParser("M0 0 a10 10 0 0 1 50 50").parsePathCommand();
        assertEquals(2, cmds.length);
        assertTrue(cmds[1] instanceof Arc);
        assertTrue(cmds[1].isRelative());
    }

    // -------------------------------------------------------------------------
    // Close path (Z/z)
    // -------------------------------------------------------------------------

    @Test
    void testClosePath() {
        PathCommand[] cmds = new PathParser("M0 0 L10 10 Z").parsePathCommand();
        assertEquals(3, cmds.length);
        assertTrue(cmds[2] instanceof Terminal);
    }

    @Test
    void testLowercaseClosePath() {
        PathCommand[] cmds = new PathParser("M0 0 L10 10 z").parsePathCommand();
        assertEquals(3, cmds.length);
        assertTrue(cmds[2] instanceof Terminal);
    }

    // -------------------------------------------------------------------------
    // Complex paths
    // -------------------------------------------------------------------------

    @Test
    void testComplexPath() {
        String d = "M10 80 C 40 10 65 10 95 80 S 150 150 180 80";
        PathCommand[] cmds = new PathParser(d).parsePathCommand();
        assertEquals(3, cmds.length);
        assertTrue(cmds[0] instanceof MoveTo);
        assertTrue(cmds[1] instanceof Cubic);
        assertTrue(cmds[2] instanceof CubicSmooth);
    }

    @Test
    void testMultipleMovesAndClose() {
        String d = "M0 0 L10 0 L10 10 L0 10 Z M20 20 L30 20 Z";
        PathCommand[] cmds = new PathParser(d).parsePathCommand();
        assertTrue(cmds.length >= 6);
        assertTrue(cmds[0] instanceof MoveTo);
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Test
    void testInvalidPathProducesEmptyOrPartialResult() {
        // An invalid path (starting with non-M) should not throw, but return empty or partial
        PathCommand[] cmds = new PathParser("L10 20").parsePathCommand();
        assertEquals(0, cmds.length);
    }

    @Test
    void testEmptyStringProducesEmptyResult() {
        PathCommand[] cmds = new PathParser("").parsePathCommand();
        assertEquals(0, cmds.length);
    }

    @Test
    void testWhitespaceOnlyProducesEmptyResult() {
        PathCommand[] cmds = new PathParser("   ").parsePathCommand();
        assertEquals(0, cmds.length);
    }
}

