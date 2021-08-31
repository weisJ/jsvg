/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
package com.github.weisj.jsvg.attributes.text;

import com.github.weisj.jsvg.attributes.Default;
import com.github.weisj.jsvg.attributes.Mapped;
import com.github.weisj.jsvg.attributes.SVGDeprecated;

public enum BaselineAlignment {
    /**
     * The value is the dominant-baseline of the script to which the character belongs
     * i.e., use the dominant-baseline of the parent.
     */
    @Default
    Auto,
    /**
     * Uses the dominant baseline choice of the parent.
     * Matches the box’s corresponding baseline to that of its parent.
     */
    Baseline,
    /**
     * The alignment-point of the object being aligned is aligned with the "before-edge" baseline
     * of the parent text content element.
     */
    @SVGDeprecated
    BeforeEdge,
    /**
     * Matches the bottom of the box to the top of the parent’s content area.
     */
    TextBottom,
    /**
     * The alignment-point of the object being aligned is aligned with the "text-before-edge"
     * baseline of the parent text content element.
     */
    @Mapped(to = "text-top")
    TextBeforeEdge,
    /**
     * Aligns the vertical midpoint of the box with the baseline of the parent box plus half the
     * x-height of the parent.
     */
    Middle,
    /**
     * Matches the box’s central baseline to the central baseline of its parent.
     */
    Central,
    /**
     * The alignment-point of the object being aligned is aligned with
     * the "after-edge" baseline of the parent text content element.
     */
    @SVGDeprecated
    AfterEdge,
    /**
     * Matches the top of the box to the top of the parent’s content area.
     */
    TextTop,
    /**
     * The alignment-point of the object being aligned is aligned with the "text-after-edge" baseline of the parent
     * text content element.
     */
    @Mapped(to = "text-bottom")
    TextAfterEdge,
    /**
     * Matches the box’s ideographic character face under-side baseline to that of its parent.
     */
    Ideographic,
    /**
     * Matches the box’s alphabetic baseline to that of its parent.
     */
    Alphabetic,
    /**
     * The alignment-point of the object being aligned is aligned with the "hanging" baseline of
     * the parent text content element.
     */
    Hanging,
    /**
     * Matches the box’s mathematical baseline to that of its parent.
     */
    Mathematical,
    /**
     * Aligns the top of the aligned subtree with the top of the line box.
     */
    Top,
    /**
     * Aligns the center of the aligned subtree with the center of the line box.
     */
    Center,
    /**
     * Aligns the bottom of the aligned subtree with the bottom of the line box.
     */
    Bottom
}
