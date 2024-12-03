/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.attributes.paint;

public final class PredefinedPaints {
    public static final AwtSVGPaint DEFAULT_PAINT = new AwtSVGPaint(PaintParser.DEFAULT_COLOR);
    public static final SVGPaint NONE = new NonePaint();
    public static final SVGPaint CURRENT_COLOR = new SentinelPaint("currentColor");
    public static final SVGPaint CONTEXT_FILL = new SentinelPaint("contextFill");
    public static final SVGPaint CONTEXT_STROKE = new SentinelPaint("contextStroke");
    public static final SVGPaint INHERITED = new SentinelPaint("inherited");


    private PredefinedPaints() {}
}
