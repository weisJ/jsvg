/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jannis Weis
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.font.*;
import com.github.weisj.jsvg.parser.impl.ParserTestUtil;
import com.github.weisj.jsvg.renderer.MeasureContext;
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
        SVGFont font1 = FontResolver.resolve(fontSpec.get(), MEASURE_CONTEXT, SVGFont.defaultFontFamily());
        SVGFont font2 = FontResolver.resolve(fontSpec.get(), MEASURE_CONTEXT, SVGFont.defaultFontFamily());
        Assertions.assertSame(font1, font2);
    }

    @Test
    void checkFontParsing() {
        String fontName = FontResolver.supportedFonts().getFirst();
        MeasurableFontSpec fontSpec = createFontSpec(
                entry("font-family", fontName),
                entry("font-size", "3em"));
        SVGFont font = FontResolver.resolveWithoutCache(fontSpec, MEASURE_CONTEXT, SVGFont.defaultFontFamily());
        Assertions.assertEquals(fontName, font.family());
        Assertions.assertEquals(3 * MEASURE_CONTEXT.em(), font.size());
    }

    private static @NotNull MeasurableFontSpec createFontSpec(@NotNull AttributeEntry... attributes) {
        Map<String, String> attrs = new HashMap<>();
        for (AttributeEntry attribute : attributes) {
            attrs.put(attribute.key, attribute.value);
        }
        return MeasurableFontSpec.createDefault().derive(FontParser.parseFontSpec(
                ParserTestUtil.createDummyAttributeNode(attrs)));
    }

    private static AttributeEntry entry(@NotNull String key, @NotNull String value) {
        return new AttributeEntry(key, value);
    }

    private record AttributeEntry(String key, String value) {
    }
}
