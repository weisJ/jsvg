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
package com.github.weisj.jsvg.geometry.path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class PathParserTest {

    @Test
    void testAdjacentFlags() {
        PathCommand[] cmds = new PathParser("M0 0 a1 2 3 10 6 7").parsePathCommand();
        Assertions.assertEquals(2, cmds.length);
        Assertions.assertTrue(cmds[0] instanceof MoveTo);
        Assertions.assertEquals(6, cmds[1].nodeCount());
    }

    @Test
    void testDotInExponent() {
        PathCommand[] cmds = new PathParser("M0 0 L 1e5.5").parsePathCommand();
        Assertions.assertEquals(2, cmds.length);
        Assertions.assertTrue(cmds[0] instanceof MoveTo);
        Assertions.assertTrue(cmds[1] instanceof LineTo);
        LineTo cmd = (LineTo) cmds[1];
        Assertions.assertEquals(cmd.x(), 1e5);
        Assertions.assertEquals(cmd.y(), .5);
    }

    @Test
    void testRepeatedArcs() {
        PathCommand[] cmds =
                new PathParser("M0 0 a1 2 3 10 6 7 1 2 3 10 6 7 1 2 3 10 6 7 1 2 3 10 6 7").parsePathCommand();
        Assertions.assertEquals(5, cmds.length);
        Assertions.assertEquals(6, cmds[1].nodeCount());
    }
}
