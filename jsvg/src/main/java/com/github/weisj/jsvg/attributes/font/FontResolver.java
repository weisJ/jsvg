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
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.FontLoader;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.util.supplier.LazySupplier;

public final class FontResolver {
    private FontResolver() {}

    public static void clearFontCache() {
        FontCache.INSTANCE.loaderToCache.clear();
    }

    public static @NotNull SVGFont resolve(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext, @NotNull String defaultFontFamily,
            @Nullable FontLoader customFontLoader) {
        // Compute font attributes only once and avoid computing them if the font has been cached.
        Supplier<Map<@NotNull TextAttribute, Object>> attributes = new LazySupplier<>(
                () -> computeFontAttributes(fontSpec, measureContext));

        SVGFont font = resolve(fontSpec, measureContext, attributes, customFontLoader);
        if (font != null) return font;
        font = resolve(fontSpec, measureContext, attributes, DefaultFontLoader.INSTANCE);
        if (font != null) return font;
        return new AWTSVGFont(fallbackFont(attributes.get(), defaultFontFamily));
    }

    private static @Nullable SVGFont resolve(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext,
            @NotNull Supplier<Map<@NotNull TextAttribute, Object>> attributes,
            @Nullable FontLoader loader) {
        if (loader == null) return null;
        FontCache.CacheKey key = new FontCache.CacheKey(fontSpec, measureContext);

        Font font = FontCache.INSTANCE.loaderToCache
                .computeIfAbsent(loader, k -> new FontCache.Cache()).fonts
                .computeIfAbsent(key, k -> resolveAWTFont(fontSpec, loader, attributes.get()));
        if (font == null) return null;
        return new AWTSVGFont(font);
    }

    public static @NotNull SVGFont resolveWithoutCache(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext, @NotNull String defaultFontFamily,
            @Nullable FontLoader fontLoader) {
        return new AWTSVGFont(resolveAWTFontWithoutCache(fontSpec, measureContext, defaultFontFamily, fontLoader));
    }

    private static @NotNull Font resolveAWTFontWithoutCache(@NotNull MeasurableFontSpec fontSpec,
            @NotNull MeasureContext measureContext, @NotNull String defaultFontFamily,
            @Nullable FontLoader fontLoader) {
        Map<@NotNull TextAttribute, Object> attributes = computeFontAttributes(fontSpec, measureContext);
        Font font = resolveAWTFont(fontSpec, fontLoader, attributes);
        if (font != null) return font;

        font = resolveAWTFont(fontSpec, DefaultFontLoader.INSTANCE, attributes);
        if (font != null) return font;

        return fallbackFont(attributes, defaultFontFamily);
    }

    private static @NotNull Font fallbackFont(@NotNull Map<@NotNull TextAttribute, Object> attributes,
            @NotNull String defaultFontFamily) {
        attributes.put(TextAttribute.FAMILY, defaultFontFamily);
        return new Font(attributes);
    }

    private static @NotNull Map<@NotNull TextAttribute, Object> computeFontAttributes(
            @NotNull MeasurableFontSpec fontSpec, @NotNull MeasureContext measureContext) {
        FontStyle style = fontSpec.style();

        float weight = cssWeightToAwtWeight(fontSpec.currentWeight());
        float size = fontSpec.effectiveSize(measureContext);
        float stretch = fontSpec.stretch().orElseIfUnspecified(1).value();

        Map<@NotNull TextAttribute, Object> attributes = new HashMap<>(5, 1f);
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
        return attributes;
    }

    /** Resolves and sets the first matching font family based on {@code fontSpec.families}. */
    private static @Nullable Font resolveAWTFont(@NotNull MeasurableFontSpec fontSpec,
            @Nullable FontLoader fontLoader, @NotNull Map<@NotNull TextAttribute, Object> attributes) {
        if (fontLoader == null) return null;
        for (String family : fontSpec.families()) {
            // A supplied font makes font selection deterministic, including for CSS generic families.
            Font customFont = fontLoader.customFont(family, attributes);
            if (customFont != null) return customFont;
        }
        return null;
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

    public static @NotNull List<@NotNull String> supportedFonts() {
        return Collections.unmodifiableList(Arrays.asList(DefaultFontLoader.INSTANCE.supportedFonts));
    }

    @SuppressWarnings("ImmutableEnumChecker")
    private enum DefaultFontLoader implements FontLoader {
        INSTANCE;

        private final @NotNull String[] supportedFonts;

        DefaultFontLoader() {
            supportedFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        }

        boolean isSupportedFontFamily(final @NotNull String fontName) {
            for (String supportedFont : supportedFonts) {
                if (supportedFont.equalsIgnoreCase(fontName)) return true;
            }
            return false;
        }

        @Override
        public @Nullable Font customFont(@NotNull String family,
                @NotNull Map<@NotNull TextAttribute, Object> attributes) {
            if (!isSupportedFontFamily(family)) return null;
            attributes.put(TextAttribute.FAMILY, family);
            return new Font(attributes);
        }
    }

    @SuppressWarnings("ImmutableEnumChecker")
    private enum FontCache {
        INSTANCE;

        private final WeakHashMap<FontLoader, Cache> loaderToCache = new WeakHashMap<>();

        private static final class Cache {
            private final HashMap<CacheKey, Font> fonts = new HashMap<>();
        }

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
