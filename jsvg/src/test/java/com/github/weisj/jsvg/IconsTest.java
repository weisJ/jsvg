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
package com.github.weisj.jsvg;

import static com.github.weisj.jsvg.ReferenceTest.compareImages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IconsTest {

    @Test
    void testIcons() {
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/desktop.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/drive.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/folder.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/general.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/homeFolder.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/image.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/missingImage.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/newFolder.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/pendingImage.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/text.svg"));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/unknown.svg", 0.4));
        assertEquals(ReferenceTest.ReferenceTestResult.SUCCESS, compareImages("icons/upFolder.svg"));
    }
}
