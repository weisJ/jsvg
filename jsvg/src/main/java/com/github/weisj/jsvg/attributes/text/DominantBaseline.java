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
package com.github.weisj.jsvg.attributes.text;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.Default;
import com.github.weisj.jsvg.attributes.HasMatchName;

public enum DominantBaseline implements HasMatchName {
    /**
     * If this property occurs on a <text> element, then the computed value depends on the value of the writing-mode
     * attribute.
     *
     * If the writing-mode is horizontal, then the value of the dominant-baseline component is alphabetic.
     * Otherwise, if the writing-mode is vertical, then the value of the dominant-baseline component is central.
     *
     * If this property occurs on a 'tspan', 'tref', 'altGlyph', or 'textPath' element, then the dominant-baseline
     * and the baseline-table components remain the same as those of the parent text content element.
     *
     * If the computed baseline-shift value actually shifts the baseline, then the baseline-table font-size component
     * is set to the value of the font-size attribute on the element on which the dominant-baseline attribute occurs,
     * otherwise the baseline-table font-size remains the same as that of the element.
     *
     * If there is no parent text content element, the scaled-baseline-table value is constructed as above for 'text'
     * elements.
     */
    @Default
    Auto,
    /**
     * The baseline-identifier for the dominant-baseline is set to be ideographic, the derived baseline-table is
     * constructed using the ideographic baseline-table in the font, and the baseline-table font-size is changed to the
     * value of the font-size attribute on this element.
     */
    Ideographic,
    /**
     * The baseline-identifier for the dominant-baseline is set to be alphabetic, the derived baseline-table is
     * constructed using the alphabetic baseline-table in the font, and the baseline-table font-size is changed to the
     * value of the font-size attribute on this element.
     */
    Alphabetic,
    /**
     * The baseline-identifier for the dominant-baseline is set to be hanging, the derived baseline-table is
     * constructed using the hanging baseline-table in the font, and the baseline-table font-size is changed to the
     * value of the font-size attribute on this element.
     */
    Hanging,
    /**
     * The baseline-identifier for the dominant-baseline is set to be mathematical, the derived baseline-table is
     * constructed using the mathematical baseline-table in the font, and the baseline-table font-size is changed to
     * the value of the font-size attribute on this element.
     */
    Mathematical,
    /**
     * The baseline-identifier for the dominant-baseline is set to be central. The derived baseline-table is
     * constructed from the defined baselines in a baseline-table in the font. That font baseline-table is chosen
     * using the following priority order of baseline-table names: ideographic, alphabetic, hanging, mathematical.
     * The baseline-table font-size is changed to the value of the font-size attribute on this element.
     */
    Central,
    /**
     * The baseline-identifier for the dominant-baseline is set to be middle. The derived baseline-table is
     * constructed from the defined baselines in a baseline-table in the font. That font baseline-table is chosen
     * using the following priority order of baseline-table names: alphabetic, ideographic, hanging, mathematical.
     * The baseline-table font-size is changed to the value of the font-size attribute on this element.
     */
    Middle,
    /**
     * The baseline-identifier for the dominant-baseline is set to be text-after-edge. The derived baseline-table is
     * constructed from the defined baselines in a baseline-table in the font. The choice of which font baseline-table
     * to use from the baseline-tables in the font is browser dependent. The baseline-table font-size is changed to
     * the value of the font-size attribute on this element.
     */
    TextAfterEdge("text-after-edge"),
    TextBottom("text-bottom"),
    /**
     * The baseline-identifier for the dominant-baseline is set to be text-before-edge. The derived baseline-table is
     * constructed from the defined baselines in a baseline-table in the font. The choice of which baseline-table to
     * use from the baseline-tables in the font is browser dependent. The baseline-table font-size is changed to the
     * value of the font-size attribute on this element.
     */
    TextBeforeEdge("text-before-edge"),
    /**
     * This value uses the top of the em box as the baseline.
     */
    TextTop("text-top");

    private final @NotNull String matchName;

    DominantBaseline(@NotNull String matchName) {
        this.matchName = matchName;
    }

    DominantBaseline() {
        this.matchName = name();
    }

    @Override
    public @NotNull String matchName() {
        return matchName;
    }
}
