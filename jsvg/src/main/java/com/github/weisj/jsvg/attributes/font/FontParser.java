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
package com.github.weisj.jsvg.attributes.font;

import java.awt.Font;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.ComponentValueGrammarParser;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;

public final class FontParser {
    private FontParser() {}

    // Todo: font-variant
    public static @NotNull AttributeFontSpec parseFontSpec(@NotNull AttributeNode node) {
        String[] fontFamilies = node.getStringList("font-family", SeparatorMode.COMMA_ONLY);
        canonicalizeFontFamily(fontFamilies);

        // Todo: https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight#fallback_weights
        @Nullable FontWeight weight = parseWeight(node);
        @Nullable FontSize size = parseFontSize(node);
        @Nullable Length sizeAdjust = parseSizeAdjust(node);
        @Nullable FontStyle style = parseFontStyle(node);
        @NotNull Percentage stretch = parseStretch(node);

        return new AttributeFontSpec(fontFamilies, style, sizeAdjust, stretch, size, weight);
    }

    /**
     * Expands a CSS {@code font} shorthand into its longhands (indexed by longhand name) per its
     * <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/font#formal_syntax">formal syntax</a>.
     * Returns an empty map if the shorthand is invalid.
     *
     * @param shorthandValue the shorthand value, excluding {@code !important} and surrounding whitespace
     */
    public static @NotNull Map<String, List<ComponentValue>> expandFontShorthand(
            @NotNull List<@NotNull ComponentValue> shorthandValue) {
        ComponentValueGrammarParser parser = new ComponentValueGrammarParser(shorthandValue);

        // Handle system font keywords
        if (shorthandValue.size() == 1 && parser.isCurrentOneOfKeywords(
                "caption",
                "icon",
                "menu",
                "message-box",
                "small-caption",
                "status-bar")) {
            return Collections.singletonMap("font", shorthandValue);
        }

        // Parse optional font-style, font-variant, font-weight, font-stretch (in any order)
        @Nullable List<ComponentValue> fontStyle = null;
        @Nullable List<ComponentValue> fontVariant = null;
        @Nullable List<ComponentValue> fontWeight = null;
        @Nullable List<ComponentValue> fontStretch = null;
        while (!parser.isEof()) {
            // font-style: normal | italic | left | right| oblique [<angle>]?
            if (fontStyle == null && parser.isCurrentOneOfKeywords("normal", "italic", "left", "right", "oblique")) {
                if (parser.isCurrentOneOfKeywords("oblique")
                        && parser.isNextOfType(TokenType.DIMENSION)) {
                    fontStyle = Arrays.asList(parser.current(), parser.peekNext());
                    parser.advance();
                } else {
                    fontStyle = Collections.singletonList(parser.current());
                }
                parser.advance();
                continue;
            }

            // font-variant: normal | small-caps
            if (fontVariant == null && parser.isCurrentOneOfKeywords("small-caps")) {
                fontVariant = Collections.singletonList(parser.current());
                parser.advance();
                continue;
            }

            // font-weight: normal | bold | bolder | lighter | 1-1000
            if (fontWeight == null && (parser.isCurrentOneOfKeywords("bold", "bolder", "lighter")
                    || parser.isNextANumberInRange(1, 1000))) {
                fontWeight = Collections.singletonList(parser.current());
                parser.advance();
                continue;
            }

            // font-stretch: normal | ultra-condensed | extra-condensed | condensed | semi-condensed |
            // semi-expanded | expanded | extra-expanded | ultra-expanded
            if (fontStretch == null && parser.isCurrentOneOfKeywords(
                    "ultra-condensed",
                    "extra-condensed",
                    "condensed",
                    "semi-condensed",
                    "semi-expanded",
                    "expanded",
                    "extra-expanded",
                    "ultra-expanded")) {
                fontStretch = Collections.singletonList(parser.current());
                parser.advance();
                continue;
            }

            // If we get here, we've found font-size (required)
            break;
        }

        // Parse required font-size
        if (parser.isEof()) {
            return Collections.emptyMap(); // Invalid: font-size is required
        }
        @NotNull List<ComponentValue> fontSize = Collections.singletonList(parser.current());
        parser.advance();

        // Parse optional line-height (preceded by /)
        @Nullable List<ComponentValue> lineHeight = null;
        if (!parser.isEof() && parser.current().isSlash()) {
            parser.advance(); // skip the "/"
            if (!parser.isEof()) {
                lineHeight = Collections.singletonList(parser.current());
                parser.advance();
            }
        }

        // Parse required font-family (rest of the tokens)
        if (parser.isEof()) {
            return Collections.emptyMap(); // Invalid: font-family is required
        }
        @NotNull List<ComponentValue> fontFamily = parser.remainingTokens();

        Map<String, List<ComponentValue>> result = new HashMap<>();
        List<ComponentValue> normal = Collections.singletonList(new Token.Str("normal"));
        result.put("font-style", fontStyle != null ? fontStyle : normal);
        result.put("font-variant", fontVariant != null ? fontVariant : normal);
        result.put("font-weight", fontWeight != null ? fontWeight : normal);
        result.put("font-stretch", fontStretch != null ? fontStretch : normal);
        result.put("font-size", fontSize);
        result.put("line-height", lineHeight != null ? lineHeight : normal);
        result.put("font-family", fontFamily);
        return result;
    }

    private static @NotNull String stripQuotes(@NotNull String str, char quoteChar) {
        String quoteStr = String.valueOf(quoteChar);
        if (str.length() > 2 && str.startsWith(quoteStr) && str.endsWith(quoteStr)) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static void canonicalizeFontFamily(@NotNull String[] fontFamilies) {
        for (int i = 0; i < fontFamilies.length; i++) {
            String family = fontFamilies[i].toLowerCase(Locale.US);
            switch (family) {
                case "sans-serif":
                    family = Font.SANS_SERIF;
                    break;
                case "serif":
                    family = Font.SERIF;
                    break;
                case "monospace":
                    family = Font.MONOSPACED;
                    break;
                default:
                    family = stripQuotes(family, '\'');
                    family = stripQuotes(family, '"');
                    break;
            }
            fontFamilies[i] = family;
        }
    }

    public static @Nullable FontWeight parseWeight(@NotNull AttributeNode node) {
        final String fontWeightKey = "font-weight";
        FontWeight weight = node.getEnum(fontWeightKey, PredefinedFontWeight.Number);
        if (weight == PredefinedFontWeight.Number) {
            if (node.hasAttribute(fontWeightKey)) {
                weight = new NumberFontWeight(
                        Math.max(
                                1, Math.min(
                                        1000, node.getFloat(
                                                fontWeightKey,
                                                PredefinedFontWeight.NORMAL_WEIGHT))));
            } else {
                weight = null;
            }
        }
        return weight;
    }

    public static @NotNull Percentage parseStretch(@NotNull AttributeNode node) {
        FontStretch stretch = node.getEnum("font-stretch", FontStretch.Percentage);
        return stretch == FontStretch.Percentage
                ? node.getPercentage("font-stretch", Percentage.UNSPECIFIED, 0.5f, 2f)
                : stretch.percentage();
    }

    public static @Nullable FontSize parseFontSize(@NotNull AttributeNode node) {
        FontSize fontSize = node.getEnum("font-size", PredefinedFontSize.Number);
        if (fontSize == PredefinedFontSize.Number) {
            Length size = node.getLength("font-size", PercentageDimension.CUSTOM, Length.UNSPECIFIED);
            fontSize = size.isSpecified()
                    ? new LengthFontSize(size)
                    : null;
        }
        return fontSize;
    }

    public static @Nullable Length parseSizeAdjust(@NotNull AttributeNode node) {
        return node.getLength("font-size-adjust", PercentageDimension.NONE);
    }

    static @Nullable FontStyle parseFontStyle(@NotNull AttributeNode node) {
        String[] parts = node.getStringList("font-style", SeparatorMode.WHITESPACE_ONLY);
        if (parts.length == 0) return null;
        String kind = parts[0];
        if ("normal".equalsIgnoreCase(kind)) return FontStyle.normal();
        if ("italic".equalsIgnoreCase(kind)) return FontStyle.italic();
        if ("oblique".equalsIgnoreCase(kind)) {
            return parts.length >= 2
                    ? new FontStyle.Oblique(node.parser().parseAngle(parts[1], FontStyle.Oblique.DEFAULT_ANGLE))
                    : FontStyle.oblique();
        }
        return null;
    }
}
