/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
package com.github.weisj.jsvg.parser;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.*;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.parser.css.StyleSheet;

public final class AttributeNode {
    private static final Length TopOrLeft = new Length(Unit.PERCENTAGE, 0f);
    private static final Length Center = new Length(Unit.PERCENTAGE, 50f);
    private static final Length BottomOrRight = new Length(Unit.PERCENTAGE, 100f);

    private final @NotNull String tagName;
    private final @NotNull Map<String, String> attributes;
    private final @Nullable AttributeNode parent;
    private final @NotNull Map<@NotNull String, @NotNull Object> namedElements;
    private final @NotNull List<@NotNull StyleSheet> styleSheets;

    private final @NotNull LoadHelper loadHelper;

    public AttributeNode(@NotNull String tagName, @NotNull Map<String, String> attributes,
            @Nullable AttributeNode parent,
            @NotNull Map<@NotNull String, @NotNull Object> namedElements,
            @NotNull List<@NotNull StyleSheet> styleSheets,
            @NotNull LoadHelper loadHelper) {
        this.tagName = tagName;
        this.attributes = attributes;
        this.parent = parent;
        this.namedElements = namedElements;
        this.styleSheets = styleSheets;
        this.loadHelper = loadHelper;
    }

    void addAttributes(@NotNull Map<String, String> attributes) {
        this.attributes.putAll(attributes);
    }

    void prepareForNodeBuilding(@NotNull ParsedElement parsedElement) {
        Map<String, String> styleSheetAttributes = new HashMap<>();

        // First process the inline styles. They have the highest priority.
        preprocessAttributes(attributes, styleSheetAttributes);

        List<StyleSheet> sheets = styleSheets();
        // Traverse the style sheets in backwards order to only use the newest definition.
        // FIXME: Only use the newest *valid* definition of a property value.
        for (int i = sheets.size() - 1; i >= 0; i--) {
            StyleSheet sheet = sheets.get(i);
            sheet.forEachMatchingRule(parsedElement, p -> {
                if (!styleSheetAttributes.containsKey(p.name())) {
                    styleSheetAttributes.put(p.name(), p.value());
                }
            });
        }
        attributes.putAll(styleSheetAttributes);
    }

    private static boolean isBlank(@NotNull String s) {
        return s.trim().isEmpty();
    }

    private static void preprocessAttributes(@NotNull Map<String, String> attributes,
            @NotNull Map<String, String> styleAttributes) {
        String styleStr = attributes.get("style");
        if (styleStr != null && !isBlank(styleStr)) {
            String[] styles = styleStr.split(";");
            for (String style : styles) {
                if (isBlank(style)) continue;
                String[] styleDef = style.split(":", 2);
                styleAttributes.put(styleDef[0].trim().toLowerCase(Locale.ENGLISH), styleDef[1].trim());
            }
        }
    }

    @NotNull
    Map<String, Object> namedElements() {
        return namedElements;
    }

    @NotNull
    List<@NotNull StyleSheet> styleSheets() {
        return styleSheets;
    }

    private <T> @Nullable T getElementById(@NotNull Class<T> type, @Nullable String id) {
        if (id == null) return null;
        // Todo: Look up in spec how elements should be resolved if multiple elements have the same id.
        Object node = namedElements.get(id);
        if (node instanceof ParsedElement) {
            node = ((ParsedElement) node).nodeEnsuringBuildStatus();
        }
        return type.isInstance(node) ? type.cast(node) : null;
    }

    private <T> @Nullable T getElementByUrl(@NotNull Class<T> type, @Nullable String value) {
        String url = loadHelper.attributeParser().parseUrl(value);
        if (url != null && url.startsWith("#")) url = url.substring(1);
        return getElementById(type, url);
    }

    public <T> @Nullable T getElementByHref(@NotNull Class<T> type, @Nullable String value) {
        if (value == null) return null;
        return getElementByUrl(type, value);
    }

    public <T> @Nullable T getElementByHref(@NotNull Class<T> type, @NotNull Category category,
            @Nullable String value) {
        T element = getElementByHref(type, value);
        if (element == null) return null;
        for (Category cat : element.getClass().getAnnotation(ElementCategories.class).value()) {
            if (cat == category) return element;
        }
        return null;
    }

    public @NotNull Map<String, String> attributes() {
        return attributes;
    }

    public @NotNull String tagName() {
        return tagName;
    }

    public boolean tagIsOneOf(@NotNull String... tags) {
        for (String tag : tags) {
            if (tagName.equals(tag)) return true;
        }
        return false;
    }

    public @Nullable AttributeNode parent() {
        return parent;
    }

    public @Nullable String getValue(@NotNull String key) {
        return attributes.get(key);
    }

    public @NotNull Color getColor(@NotNull String key) {
        return getColor(key, PaintParser.DEFAULT_COLOR);
    }

    @Contract("_,!null -> !null")
    public @Nullable Color getColor(@NotNull String key, @Nullable Color fallback) {
        String value = getValue(key);
        if (value == null) return fallback;
        Color c = loadHelper.attributeParser().paintParser().parseColor(value.toLowerCase(Locale.ENGLISH), this);
        return c != null ? c : fallback;
    }

    public @NotNull SVGPaint getPaint(@NotNull String key, @NotNull SVGPaint fallback) {
        SVGPaint paint = getPaint(key);
        return paint != null ? paint : fallback;
    }

    public @Nullable SVGPaint getPaint(@NotNull String key) {
        String value = getValue(key);
        SVGPaint paint = getElementByUrl(SVGPaint.class, value);
        if (paint != null) return paint;
        return loadHelper.attributeParser().parsePaint(value, this);
    }

    public @Nullable Length getLength(@NotNull String key) {
        return getLengthInternal(key, null);
    }

    public @NotNull Length getLength(@NotNull String key, float fallback) {
        return getLength(key, Unit.Raw.valueOf(fallback));
    }

    public @NotNull Length getLength(@NotNull String key, @NotNull Length fallback) {
        return getLengthInternal(key, fallback);
    }

    @Contract("_,!null -> !null")
    private @Nullable Length getLengthInternal(@NotNull String key, @Nullable Length fallback) {
        return loadHelper.attributeParser().parseLength(getValue(key), fallback);
    }

    public @NotNull Length getHorizontalReferenceLength(@NotNull String key) {
        return parseReferenceLength(key, "left", "right");
    }

    public @NotNull Length getVerticalReferenceLength(@NotNull String key) {
        return parseReferenceLength(key, "top", "bottom");
    }

    private @NotNull Length parseReferenceLength(@NotNull String key, @NotNull String topLeft,
            @NotNull String bottomRight) {
        String value = getValue(key);
        if (topLeft.equals(value)) {
            return TopOrLeft;
        } else if ("center".equals(value)) {
            return Center;
        } else if (bottomRight.equals(value)) {
            return BottomOrRight;
        } else {
            return loadHelper.attributeParser().parseLength(value, Length.ZERO);
        }
    }

    public @Percentage float getPercentage(@NotNull String key, @Percentage float fallback) {
        return loadHelper.attributeParser().parsePercentage(getValue(key), fallback);
    }

    public @NotNull Length[] getLengthList(@NotNull String key) {
        return loadHelper.attributeParser().parseLengthList(getValue(key));
    }

    public float[] getFloatList(@NotNull String key) {
        return loadHelper.attributeParser().parseFloatList(getValue(key));
    }

    public double[] getDoubleList(@NotNull String key) {
        return loadHelper.attributeParser().parseDoubleList(getValue(key));
    }

    public <E extends Enum<E>> @NotNull E getEnum(@NotNull String key, @NotNull E fallback) {
        return loadHelper.attributeParser().parseEnum(getValue(key), fallback);
    }

    public <E extends Enum<E>> @Nullable E getEnumNullable(@NotNull String key, @NotNull Class<E> enumType) {
        return loadHelper.attributeParser().parseEnum(getValue(key), enumType);
    }

    public @Nullable ClipPath getClipPath() {
        return getElementByUrl(ClipPath.class, getValue("clip-path"));
    }

    public @Nullable Mask getMask() {
        return getElementByUrl(Mask.class, getValue("mask"));
    }

    public @Nullable Filter getFilter() {
        return getElementByUrl(Filter.class, getValue("filter"));
    }

    public @Nullable AffineTransform parseTransform(@NotNull String key) {
        return loadHelper.attributeParser().parseTransform(getValue(key));
    }

    public boolean hasAttribute(@NotNull String name) {
        return attributes.containsKey(name);
    }

    public @NotNull String[] getStringList(@NotNull String name) {
        return getStringList(name, SeparatorMode.COMMA_AND_WHITESPACE);
    }


    public @NotNull String[] getStringList(@NotNull String name, SeparatorMode separatorMode) {
        return loadHelper.attributeParser().parseStringList(getValue(name), separatorMode);
    }

    public float getFloat(@NotNull String name, float fallback) {
        return loadHelper.attributeParser().parseFloat(getValue(name), fallback);
    }

    public int getInt(@NotNull String key, int fallback) {
        return loadHelper.attributeParser().parseInt(getValue(key), fallback);
    }

    public @Nullable String getHref() {
        String href = getValue("href");
        if (href == null) return getValue("xlink:href");
        return href;
    }

    public @Nullable ViewBox getViewBox() {
        float[] viewBoxCords = getFloatList("viewBox");
        return viewBoxCords.length == 4 ? new ViewBox(viewBoxCords) : null;
    }

    public @NotNull AttributeParser parser() {
        return loadHelper.attributeParser();
    }

    public @NotNull ResourceLoader resourceLoader() {
        return loadHelper.resourceLoader();
    }
}
