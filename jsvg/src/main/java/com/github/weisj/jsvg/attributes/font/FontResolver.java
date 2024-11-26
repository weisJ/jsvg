/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
import java.util.*;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class FontResolver {
    private FontResolver() {}

    public static void clearFontCache() {
        FontCache.INSTANCE.cache.clear();
    }

    public static @NotNull SVGFont resolve(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext) {
        FontCache.CacheKey key = new FontCache.CacheKey(fontSpec, measureContext);
        SVGFont cachedFont = FontCache.INSTANCE.cache.get(key);
        if (cachedFont != null) return cachedFont;
        SVGFont resolvedFont = resolveWithoutCache(fontSpec, measureContext);
        FontCache.INSTANCE.cache.put(key, resolvedFont);
        return resolvedFont;
    }

    public static @NotNull SVGFont resolveWithoutCache(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext) {
        String family = findSupportedFontFamily(fontSpec);

        FontStyle style = fontSpec.style();

        float weight = cssWeightToAwtWeight(fontSpec.currentWeight());
        float size = fontSpec.effectiveSize(measureContext);
        float stretch = fontSpec.stretch().orElseIfUnspecified(1).value();

        Map<AttributedCharacterIterator.Attribute, Object> attributes = new HashMap<>(5, 1f);
        attributes.put(TextAttribute.FAMILY, family);
        attributes.put(TextAttribute.SIZE, size);
        attributes.put(TextAttribute.WEIGHT, weight);
        attributes.put(TextAttribute.WIDTH, stretch);


        if (style instanceof FontStyle.Normal) {
            attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
        } else if (style instanceof FontStyle.Italic) {
            attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        } else {
            AffineTransform transform = style.transform();
            if (transform != null) attributes.put(TextAttribute.TRANSFORM, transform);
        }

        Font font = new Font(attributes);
        return new AWTSVGFont(font);
    }

    private static float cssWeightToAwtWeight(float weight) {
        int normalWeight = PredefinedFontWeight.NORMAL_WEIGHT;
        float currentWeight = weight;
        if (currentWeight > normalWeight) {
            // The bold weight for css and awt differ. We compensate for this difference to ensure
            // that bold css fonts correspond to bold awt fonts, as this is the most commonly supported
            // font variation.
            float awtWeightCompensationFactor =
                    (TextAttribute.WEIGHT_BOLD * normalWeight) / PredefinedFontWeight.BOLD_WEIGHT;
            currentWeight *= awtWeightCompensationFactor;
        }
        return currentWeight / normalWeight;
    }

    private static @NotNull String findSupportedFontFamily(@NotNull MeasurableFontSpec fontSpec) {
        String[] families = fontSpec.families();
        for (String family : families) {
            if (FontFamiliesCache.INSTANCE.isSupportedFontFamily(family)) return family;
        }
        return MeasurableFontSpec.DEFAULT_FONT_FAMILY_NAME;
    }

    public static @NotNull List<@NotNull String> supportedFonts() {
        return Collections.unmodifiableList(Arrays.asList(FontFamiliesCache.INSTANCE.supportedFonts));
    }

    @SuppressWarnings("ImmutableEnumChecker")
    private enum FontFamiliesCache {
        INSTANCE;

        private final @NotNull String[] supportedFonts;

        FontFamiliesCache() {
            supportedFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        }

        boolean isSupportedFontFamily(final @NotNull String fontName) {
            for (String supportedFont : supportedFonts) {
                if (supportedFont.equalsIgnoreCase(fontName)) return true;
            }
            return false;
        }
    }

    @SuppressWarnings("ImmutableEnumChecker")
    private enum FontCache {
        INSTANCE;

        private final HashMap<CacheKey, SVGFont> cache = new HashMap<>();

        private static final class CacheKey {
            private final @NotNull MeasurableFontSpec spec;
            private final @NotNull MeasureContext context;

            private CacheKey(@NotNull MeasurableFontSpec spec, @NotNull MeasureContext context) {
                this.spec = spec;
                this.context = context;
            }

            @Override
            public String toString() {
                return "CacheKey{" +
                        "spec=" + spec +
                        ", context=" + context +
                        '}';
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
