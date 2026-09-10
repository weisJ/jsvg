/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.css;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.renderer.CssHints;

public interface CssParser {
    /**
     * Parse a stylesheet, evaluating {@code @media} at-rules against the given hints.
     * CSS shorthand attributes like font are replaced with multiple declarations.
     */
    @NotNull
    StyleRuleList parseStyleSheet(@NotNull List<char[]> input, @NotNull CssHints hints);

    /**
     * Parse an SVG style attribute. Shorthands like {@code font} are expanded into their longhands.
     */
    @NotNull
    List<@NotNull NormalizedProperty> parseStyleAttribute(@NotNull String input, @NotNull CssHints hints);

    @NotNull
    List<@NotNull ComponentValue> parseCssAttribute(@NotNull String input);

    @NotNull
    List<@NotNull List<@NotNull ComponentValue>> parseCommaSeparatedCssAttribute(@NotNull String input);
}
