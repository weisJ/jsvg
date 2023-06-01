/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
package com.github.weisj.jsvg.attributes.filter;

import org.jetbrains.annotations.NotNull;

public enum DefaultFilterChannel implements FilterChannelKey {
    /**
     * This keyword represents the graphics elements that were the original input into the <filter> element.
     */
    SourceGraphic,
    /**
     * This keyword represents the graphics elements that were the original input into the <filter> element.
     * SourceAlpha has all the same rules as SourceGraphic except that only the alpha channel is used.
     */
    SourceAlpha,
    /**
     * This keyword represents an image snapshot of the SVG document under the filter region at the time that the
     * <filter> element was invoked.
     */
    BackgroundImage,
    /**
     * Same as BackgroundImage except only the alpha channel is used.
     */
    BackgroundAlpha,
    /**
     * This keyword represents the value of the fill property on the target element for the filter effect.
     * In many cases, the FillPaint is opaque everywhere, but that might not be the case if a shape is painted with a
     * gradient or pattern which itself includes transparent or semi-transparent parts.
     */
    FillPaint,
    /**
     * This keyword represents the value of the stroke property on the target element for the filter effect.
     * In many cases, the StrokePaint is opaque everywhere, but that might not be the case if a shape is painted with a
     * gradient or pattern which itself includes transparent or semi-transparent parts.
     */
    StrokePaint,
    /**
     * Uses the result of the preceding filter or SourceGraphic if this is the first filter.
     */
    LastResult;

    @Override
    public @NotNull Object key() {
        if (this == LastResult) return this;
        return toString();
    }
}
