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
import com.github.weisj.jsvg.geometry.size.AngleUnit;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.ComponentValueGrammarParser;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;

public final class FontParser {
    private FontParser() {}

    // Todo: font-variant
    public static @NotNull AttributeFontSpec parseFontSpec(@NotNull AttributeNode node) {
        String[] fontFamilies = parseFontFamily(node);

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
        ComponentValueGrammarParser parser = new ComponentValueGrammarParser(shorthandValue, true);

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
                if (parser.isCurrentOneOfKeywords("oblique") && isAngleDimension(parser.peekNext())) {
                    // a non-angle dimension is the font-size
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
                    || parser.current().isANumberInRange(1, 1000))) {
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
        List<ComponentValue> normal = Collections.singletonList(new Token.Ident("normal"));
        result.put("font-style", fontStyle != null ? fontStyle : normal);
        result.put("font-variant", fontVariant != null ? fontVariant : normal);
        result.put("font-weight", fontWeight != null ? fontWeight : normal);
        result.put("font-stretch", fontStretch != null ? fontStretch : normal);
        result.put("font-size", fontSize);
        result.put("line-height", lineHeight != null ? lineHeight : normal);
        result.put("font-family", fontFamily);
        return result;
    }

    /** A {@code <dimension>} with an angle unit. */
    private static boolean isAngleDimension(@Nullable ComponentValue value) {
        if (!(value instanceof Token.Dimension)) return false;
        String unit = ((Token.Dimension) value).unit().toLowerCase(Locale.ENGLISH);
        for (AngleUnit angleUnit : AngleUnit.units()) {
            if (angleUnit != AngleUnit.Raw && angleUnit.suffix().equals(unit)) return true;
        }
        return false;
    }

    public static @NotNull String @NotNull [] parseFontFamily(@NotNull AttributeNode node) {
        List<List<ComponentValue>> groups = node.getSplitTokenList("font-family", SeparatorMode.COMMA_ONLY);
        if (groups == null) return new String[0];
        String[] families = familyNames(groups);
        canonicalizeFontFamily(families);
        return families;
    }

    private static @NotNull String @NotNull [] familyNames(
            @NotNull List<@NotNull List<@NotNull ComponentValue>> groups) {
        String[] families = new String[groups.size()];
        for (int i = 0; i < families.length; i++) {
            families[i] = familyName(groups.get(i));
        }
        return families;
    }

    /** {@code <family-name> = <string> | <custom-ident>+}; idents joined by spaces. */
    private static @NotNull String familyName(@NotNull List<@NotNull ComponentValue> group) {
        StringBuilder name = new StringBuilder();
        for (ComponentValue token : group) {
            if (token instanceof Token.Str) return ((Token.Str) token).value();
            if (!(token instanceof Token.Ident)) continue;
            if (name.length() > 0) name.append(' ');
            name.append(((Token.Ident) token).name());
        }
        return name.toString();
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
        List<ComponentValue> tokens = node.getTokens("font-style");
        if (tokens == null) return null;
        ComponentValueGrammarParser parser = new ComponentValueGrammarParser(tokens, true);
        if (parser.isEof()) return null;
        if (parser.isCurrentOneOfKeywords("normal")) return FontStyle.normal();
        if (parser.isCurrentOneOfKeywords("italic")) return FontStyle.italic();
        if (parser.isCurrentOneOfKeywords("oblique")) {
            parser.advance();
            return parser.isEof()
                    ? FontStyle.oblique()
                    : new FontStyle.Oblique(
                            node.parser().parseAngle(parser.remainingTokens(), FontStyle.Oblique.DEFAULT_ANGLE));
        }
        return null;
    }
}
