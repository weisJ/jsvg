/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class FontResolver {
    private FontResolver() {}

    public static @NotNull SVGFont resolve(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext) {
        FontCache.CacheKey key = new FontCache.CacheKey(fontSpec, measureContext);
        SVGFont cachedFont = FontCache.cache.get(key);
        if (cachedFont != null) return cachedFont;

        // Todo: Check family before caching.
        String[] families = fontSpec.families();

        FontStyle style = fontSpec.style();
        int weight = fontSpec.currentWeight();
        float size = fontSpec.currentSize().resolveLength(measureContext);
        float stretch = fontSpec.stretch();

        Map<AttributedCharacterIterator.Attribute, Object> attributes = new HashMap<>(4, 1f);
        attributes.put(TextAttribute.FAMILY, families[0]);
        attributes.put(TextAttribute.SIZE, size);
        attributes.put(TextAttribute.WEIGHT, weight / 100f);
        AffineTransform transform = style.transform();
        if (transform != null) attributes.put(TextAttribute.TRANSFORM, transform);
        int awtStyle = style.awtCode();
        Font font = new Font(attributes);
        if (awtStyle != Font.PLAIN) {
            font = font.deriveFont(awtStyle);
        }
        SVGFont resolvedFont = new AWTSVGFont(font, style, weight, stretch);
        FontCache.cache.put(key, resolvedFont);
        return resolvedFont;
    }

    private static final class FontCache {
        private static final HashMap<CacheKey, SVGFont> cache = new HashMap<>();

        private static final class CacheKey {
            private final @NotNull MeasurableFontSpec spec;
            private final @NotNull MeasureContext context;

            private CacheKey(@NotNull MeasurableFontSpec spec, @NotNull MeasureContext context) {
                this.spec = spec;
                this.context = context;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof CacheKey)) return false;
                CacheKey cacheKey = (CacheKey) o;
                return spec.equals(cacheKey.spec) && context.equals(cacheKey.context);
            }

            @Override
            public int hashCode() {
                return Objects.hash(spec, context);
            }
        }
    }
}
