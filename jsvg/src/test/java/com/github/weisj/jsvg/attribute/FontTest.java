/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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
package com.github.weisj.jsvg.attribute;

import java.awt.Font;
import java.awt.image.ImageObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.impl.FullCssParser;
import com.github.weisj.jsvg.parser.impl.ParserTestUtil;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.FloatSize;

class FontTest {

    private static final MeasureContext MEASURE_CONTEXT =
            MeasureContext.createInitial(
                    new FloatSize(100, 100), 12, 6,
                    AnimationState.NO_ANIMATION);

    @BeforeEach
    void clearFontCache() {
        FontResolver.clearFontCache();
    }

    @Test
    void cachedFontShouldBeUsed() {
        Supplier<MeasurableFontSpec> fontSpec = () -> createFontSpec(
                entry("font-family", "sans-serif"),
                entry("font-size", "11"));
        SVGFont font1 = FontResolver.resolve(fontSpec.get(), MEASURE_CONTEXT, NullPlatformSupport.INSTANCE);
        SVGFont font2 = FontResolver.resolve(fontSpec.get(), MEASURE_CONTEXT, NullPlatformSupport.INSTANCE);
        Assertions.assertSame(font1, font2);
    }

    @Test
    void checkFontParsing() {
        String fontName = FontResolver.supportedFonts().getFirst();
        MeasurableFontSpec fontSpec = createFontSpec(
                entry("font-family", fontName),
                entry("font-size", "3em"));
        SVGFont font = FontResolver.resolveWithoutCache(fontSpec, MEASURE_CONTEXT, NullPlatformSupport.INSTANCE);
        Assertions.assertEquals(fontName, font.family());
        Assertions.assertEquals(3 * MEASURE_CONTEXT.em(), font.size());
    }

    String queriedFontFamily;

    @Test
    void customFontIsUsed() {
        PlatformSupport support = getSupport();

        // "NoSuchFamily" is not a registered AWT family, so the custom hook must be used.
        MeasurableFontSpec fontSpec = createFontSpec(
                entry("font-family", "NoSuchFamily"),
                entry("font-size", "12"));
        SVGFont font = FontResolver.resolveWithoutCache(fontSpec, MEASURE_CONTEXT, support);

        Assertions.assertEquals("nosuchfamily", queriedFontFamily); // CSS-canonicalized
        Assertions.assertEquals(12f, font.size());
    }

    @Test
    void checkFontShorthandExpansion() {
        //@formatter:off

        // font-size font-family
        assertFontShorthand("1.2em \"Fira Sans\", sans-serif",
            mapOf(
                "font-style", "normal",
                "font-variant", "normal",
                "font-weight", "normal",
                "font-stretch", "normal",
                "font-size", "1.2em",
                "line-height", "normal",
                "font-family", "\"Fira Sans\", sans-serif"));

        // font-size/line-height font-family
        assertFontShorthand("1.2em/2 \"Fira Sans\", sans-serif",
            mapOf(
                "font-style", "normal",
                "font-variant", "normal",
                "font-weight", "normal",
                "font-stretch", "normal",
                "font-size", "1.2em",
                "line-height", "2",
                "font-family", "\"Fira Sans\", sans-serif"));

        // font-style font-weight font-size font-family
        assertFontShorthand("italic bold 1.2em \"Fira Sans\", sans-serif",
            mapOf(
                "font-style", "italic",
                "font-variant", "normal",
                "font-weight", "bold",
                "font-stretch", "normal",
                "font-size", "1.2em",
                "line-height", "normal",
                "font-family", "\"Fira Sans\", sans-serif"));

        // font-stretch font-variant font-size font-family
        assertFontShorthand("ultra-condensed small-caps 1.2em \"Fira Sans\", sans-serif",
            mapOf(
                "font-style", "normal",
                "font-variant", "small-caps",
                "font-weight", "normal",
                "font-stretch", "ultra-condensed",
                "font-size", "1.2em",
                "line-height", "normal",
                "font-family", "\"Fira Sans\", sans-serif"));

        // system font
        assertFontShorthand("caption", mapOf("font", "caption"));

        //@formatter:on
    }

    private static void assertFontShorthand(@NotNull String input,
            @NotNull Map<String, List<ComponentValue>> expected) {
        List<ComponentValue> parsedInput = new FullCssParser().parseCssAttribute(input);
        Assertions.assertEquals(expected, FontParser.expandFontShorthand(parsedInput));
    }

    private static @NotNull Map<String, List<ComponentValue>> mapOf(@NotNull String... keysAndValues) {
        FullCssParser parser = new FullCssParser();
        Map<String, List<ComponentValue>> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], parser.parseCssAttribute(keysAndValues[i + 1]));
        }
        return map;
    }

    private @NotNull PlatformSupport getSupport() {
        Font stub = new Font(Font.DIALOG, Font.PLAIN, 1);
        PlatformSupport support = new PlatformSupport() {
            @Override
            public ImageObserver imageObserver() {
                return null;
            }

            @Override
            public TargetSurface targetSurface() {
                return null;
            }

            @Override
            public @NotNull Font customFont(@NotNull String family) {
                queriedFontFamily = family;
                return stub;
            }
        };
        return support;
    }

    private static @NotNull MeasurableFontSpec createFontSpec(@NotNull AttributeEntry... attributes) {
        Map<String, String> attrs = new HashMap<>();
        for (AttributeEntry attribute : attributes) {
            attrs.put(attribute.key, attribute.value);
        }
        return MeasurableFontSpec.createDefault(SVGFont.defaultFontSize()).derive(FontParser.parseFontSpec(
                ParserTestUtil.createDummyAttributeNode(attrs)));
    }

    private static AttributeEntry entry(@NotNull String key, @NotNull String value) {
        return new AttributeEntry(key, value);
    }

    private record AttributeEntry(String key, String value) {
    }
}
