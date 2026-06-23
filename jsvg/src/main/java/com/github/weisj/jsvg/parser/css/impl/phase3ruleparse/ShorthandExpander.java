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
package com.github.weisj.jsvg.parser.css.impl.phase3ruleparse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Declaration;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.google.errorprone.annotations.Immutable;

/**
 * Expands CSS shorthand properties into their longhands, or wraps a non-shorthand declaration as-is.
 */
@Immutable
public class ShorthandExpander {
    private static final String FONT_KEY = "font";
    private static final String MARKER_KEY = "marker";

    public static @NotNull List<NormalizedProperty> expand(@NotNull Declaration declaration) {
        switch (declaration.name()) {
            case FONT_KEY:
                return expandFont(declaration);
            case MARKER_KEY:
                return expandMarker(declaration);
            default:
                return Collections.singletonList(
                        new NormalizedProperty(declaration.name(), declaration.value(), declaration.important()));
        }
    }

    /** Expands the <a href="https://www.w3.org/TR/css-fonts-3/#font-prop">{@code font}</a> shorthand. */
    private static @NotNull List<NormalizedProperty> expandFont(@NotNull Declaration declaration) {
        Map<String, List<ComponentValue>> expandedAttributes = FontParser.expandFontShorthand(declaration.value());

        List<NormalizedProperty> result = new ArrayList<>(expandedAttributes.size());
        for (Map.Entry<String, List<ComponentValue>> expansion : expandedAttributes.entrySet()) {
            result.add(new NormalizedProperty(expansion.getKey(), expansion.getValue(), declaration.important()));
        }
        return result;
    }

    /** Expands the <a href="https://www.w3.org/TR/SVG2/painting.html#MarkerShorthand">{@code marker}</a> shorthand. */
    private static @NotNull List<NormalizedProperty> expandMarker(@NotNull Declaration declaration) {
        return Arrays.asList(
                new NormalizedProperty("marker-start", declaration.value(), declaration.important()),
                new NormalizedProperty("marker-mid", declaration.value(), declaration.important()),
                new NormalizedProperty("marker-end", declaration.value(), declaration.important()));
    }
}
