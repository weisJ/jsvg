/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Attributes {
    private Attributes() {}

    /**
     * Presentation attributes that are also CSS properties (SVG 2 §6.1): when declared on an element their value is
     * run through the CSS grammar and shorthands expanded. Non-shorthand properties are parsed on demand, so this list
     * need not be complete.
     */
    static final Set<String> CSS_ATTRIBUTES = new HashSet<>(Arrays.asList(
            "fill", "fill-opacity", "fill-rule",
            "stroke", "stroke-opacity", "stroke-width", "stroke-linecap", "stroke-linejoin",
            "stroke-miterlimit", "stroke-dasharray", "stroke-dashoffset",
            Attributes.Name.COLOR, "opacity",
            "font", "font-family", "font-size", "font-size-adjust", "font-stretch",
            "font-style", "font-variant", "font-weight", "line-height",
            "marker", "marker-start", "marker-mid", "marker-end",
            "text-anchor", "dominant-baseline", "alignment-baseline", "baseline-shift",
            "direction", "letter-spacing", "word-spacing", "text-decoration", "writing-mode",
            "visibility", "display", "overflow",
            "color-interpolation", "color-interpolation-filters", "shape-rendering", "image-rendering",
            "clip", "clip-path", "clip-rule", "mask", "filter",
            "stop-color", "stop-opacity", "flood-color", "flood-opacity", "lighting-color",
            "solid-color", "solid-opacity",
            "paint-order", "vector-effect", "mix-blend-mode", "isolation", "pointer-events",
            "transform", "transform-origin", "transform-box", "transform-style"));

    public static final class Name {
        private Name() {}

        public static final String COLOR = "color";
    }

    public static final class Keyword {
        private Keyword() {}

        public static final String INHERIT = "inherit";
        public static final String CURRENT_COLOR = "currentColor";
    }
}
