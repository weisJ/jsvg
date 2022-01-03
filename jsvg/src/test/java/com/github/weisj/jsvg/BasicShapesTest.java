/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
import static com.github.weisj.jsvg.ReferenceTest.wrapTag;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BasicShapesTest {
    @Test
    void testLine() {
        assertEquals(SUCCESS,
                compareImages("line1", wrapTag(100, 100, "<line x1='05' y1='05' x2='70' y2='51' stroke='blue'/>")));
        assertEquals(SUCCESS,
                compareImages("line2", wrapTag(100, 200, "<line x1='05' y1='05' x2='70' y2='51' stroke='green'/>")));
        assertEquals(SUCCESS,
                compareImages("line3", wrapTag(200, 200, "<line x1='05' y1='05' x2='70' y2='51' stroke='yellow'/>")));
        assertEquals(SUCCESS,
                compareImages("line4", wrapTag(100, 100, "<line x1='10' y1='15' x2='80' y2='51' stroke='red'/>")));
        assertEquals(SUCCESS,
                compareImages("line5", wrapTag(100, 200, "<line x1='10' y1='15' x2='80' y2='51' stroke='#000000'/>")));
        assertEquals(SUCCESS,
                compareImages("line6", wrapTag(200, 200, "<line x1='10' y1='15' x2='80' y2='51' stroke='black'/>")));
        assertEquals(SUCCESS, compareImages("line.svg"));
    }

    @Test
    void testRectangle() {
        assertEquals(SUCCESS,
                compareImages("rect1", wrapTag(100, 100, "<rect x='5' y='5' width='70' height='51' fill='blue'/>")));
        assertEquals(SUCCESS,
                compareImages("rect2", wrapTag(100, 200, "<rect x='5' y='5' width='70' height='51' fill='green'/>")));
        assertEquals(SUCCESS,
                compareImages("rect3", wrapTag(200, 200, "<rect x='5' y='5' width='70' height='51' fill='yellow'/>")));
        assertEquals(SUCCESS,
                compareImages("rect4", wrapTag(100, 100, "<rect x='10' y='15' width='80' height='51' fill='red'/>")));
        assertEquals(SUCCESS, compareImages("rect5",
                wrapTag(100, 200, "<rect x='10' y='15' width='80' height='51' fill='#000000'/>")));
        assertEquals(SUCCESS,
                compareImages("rect6", wrapTag(200, 200, "<rect x='10' y='15' width='80' height='51' fill='black'/>")));
    }

    @Test
    void testRoundRectangle() {
        assertEquals(SUCCESS, compareImages("roundRect1",
                wrapTag(100, 100, "<rect x='5' y='5' width='70' height='51' rx='5' ry='0' fill='blue'/>")));
        assertEquals(SUCCESS, compareImages("roundRect2",
                wrapTag(100, 200, "<rect x='5' y='5' width='70' height='51' rx='10' ry='5' fill='green'/>")));
        assertEquals(SUCCESS, compareImages("roundRect3",
                wrapTag(200, 200, "<rect x='5' y='5' width='70' height='51' rx='20' ry='10' fill='yellow'/>")));
        assertEquals(SUCCESS, compareImages("roundRect4",
                wrapTag(100, 100, "<rect x='10' y='15' width='80' height='51' rx='30' ry='0' fill='red'/>")));
        assertEquals(SUCCESS, compareImages("roundRect5",
                wrapTag(100, 200, "<rect x='10' y='15' width='80' height='51' rx='15' ry='5' fill='#000000'/>")));
        assertEquals(SUCCESS, compareImages("roundRect6",
                wrapTag(200, 200, "<rect x='10' y='15' width='80' height='51' rx='5' ry='10' fill='black'/>")));
    }

    @Test
    void testCircle() {
        assertEquals(SUCCESS,
                compareImages("circle1", wrapTag(100, 100, "<circle cx='40' cy='40' r='35' fill='blue'/>")));
        assertEquals(SUCCESS,
                compareImages("circle2", wrapTag(100, 200, "<circle cx='40' cy='40' r='35' fill='green'/>")));
        assertEquals(SUCCESS,
                compareImages("circle3", wrapTag(200, 200, "<circle cx='40' cy='40' r='35' fill='yellow'/>")));
        assertEquals(SUCCESS,
                compareImages("circle4", wrapTag(100, 100, "<circle cx='50' cy='55' r='40' fill='red'/>")));
        assertEquals(SUCCESS,
                compareImages("circle5", wrapTag(100, 200, "<circle cx='50' cy='55' r='40' fill='#000000'/>")));
        assertEquals(SUCCESS,
                compareImages("circle6", wrapTag(200, 200, "<circle cx='50' cy='55' r='40' fill='black'/>")));
    }

    @Test
    void testEllipse() {
        assertEquals(SUCCESS, compareImages("ellipse1",
                wrapTag(100, 100, "<ellipse cx='40' cy='40' rx='35' ry='5' fill='blue'/>")));
        assertEquals(SUCCESS, compareImages("ellipse2",
                wrapTag(100, 200, "<ellipse cx='40' cy='40' rx='35' ry='50' fill='green'/>")));
        assertEquals(SUCCESS, compareImages("ellipse3",
                wrapTag(200, 200, "<ellipse cx='40' cy='40' rx='35' ry='80' fill='yellow'/>")));
        assertEquals(SUCCESS, compareImages("ellipse4",
                wrapTag(100, 100, "<ellipse cx='50' cy='55' rx='40' ry='5' fill='red'/>")));
        assertEquals(SUCCESS, compareImages("ellipse5",
                wrapTag(100, 200, "<ellipse cx='50' cy='55' rx='40' ry='45' fill='#000000'/>")));
        assertEquals(SUCCESS, compareImages("ellipse6",
                wrapTag(200, 200, "<ellipse cx='50' cy='55' rx='40' ry='30' fill='black'/>")));
    }
}
