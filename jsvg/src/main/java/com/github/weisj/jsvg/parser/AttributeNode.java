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
package com.github.weisj.jsvg.parser;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.time.Duration;
import com.github.weisj.jsvg.animation.value.*;
import com.github.weisj.jsvg.attributes.*;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.PredefinedPaints;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.*;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.parser.css.StyleSheet;
import com.github.weisj.jsvg.renderer.AnimationState;

public final class AttributeNode {

    public enum ElementRelation {
        GEOMETRY_DATA,
        PAINTED_CHILD,
        TEMPLATE
    }

    private static final Length TopOrLeft = new Length(Unit.PERCENTAGE, 0f);
    private static final Length Center = new Length(Unit.PERCENTAGE, 50f);
    private static final Length BottomOrRight = new Length(Unit.PERCENTAGE, 100f);
    private static final Length FALLBACK_LENGTH = new Length(Unit.RAW, 0f);
    private static final Percentage FALLBACK_PERCENTAGE = new Percentage(1f);
    private static final MeasureContext DUMMY_MEASURE_CONTEXT =
            new MeasureContext(0, 0, 0, 0, 0, new AnimationState(0, 0));

    private final @NotNull String tagName;
    private final @NotNull Map<String, String> attributes;
    private final @NotNull List<@NotNull StyleSheet> styleSheets;

    private final @NotNull LoadHelper loadHelper;
    private ParsedElement element = null;

    public AttributeNode(@NotNull String tagName, @NotNull Map<String, String> attributes,
            @NotNull List<@NotNull StyleSheet> styleSheets,
            @NotNull LoadHelper loadHelper) {
        this.tagName = tagName;
        this.attributes = attributes;
        this.styleSheets = styleSheets;
        this.loadHelper = loadHelper;
    }

    void setElement(ParsedElement element) {
        this.element = element;
    }

    public @NotNull AttributeNode copy() {
        AttributeNode node = new AttributeNode(tagName, new HashMap<>(attributes), styleSheets, loadHelper);
        node.setElement(element);
        return node;
    }

    void prepareForNodeBuilding() {
        Map<String, String> styleSheetAttributes = new HashMap<>();

        // First process the inline styles. They have the highest priority.
        preprocessAttributes(attributes, styleSheetAttributes);

        List<StyleSheet> sheets = styleSheets();
        // Traverse the style sheets in backwards order to only use the newest definition.
        // FIXME: Only use the newest *valid* definition of a property value.
        for (int i = sheets.size() - 1; i >= 0; i--) {
            StyleSheet sheet = sheets.get(i);
            sheet.forEachMatchingRule(element, p -> {
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

    public @NotNull ParsedDocument document() {
        return element().document();
    }

    public @NotNull ParsedElement element() {
        return element;
    }

    @NotNull
    List<@NotNull StyleSheet> styleSheets() {
        return styleSheets;
    }

    private <T> @Nullable T getElementByUrl(@NotNull Class<T> type, @Nullable String value) {
        if (value == null) return null;
        return loadHelper.elementLoader().loadElement(type, value, document(), loadHelper.attributeParser());
    }

    private <T> T recordIndirectChild(T child, String value, ElementRelation relation) {
        if (child != null && relation == ElementRelation.PAINTED_CHILD) {
            ParsedElement containingElement = getElementByUrl(ParsedElement.class, value);
            if (containingElement != null) {
                element().addIndirectChild(containingElement);
            }
        }
        return child;
    }

    public <T> @Nullable T getElementByHref(@NotNull Class<T> type, @Nullable String value, ElementRelation relation) {
        return recordIndirectChild(getElementByUrl(type, value), value, relation);
    }

    public <T> @Nullable T getElementByHref(@NotNull Class<T> type, @NotNull Category category,
            @Nullable String value, ElementRelation relation) {
        T e = getElementByUrl(type, value);
        if (e == null) return null;
        for (Category cat : e.getClass().getAnnotation(ElementCategories.class).value()) {
            if (cat == category) return recordIndirectChild(e, value, relation);
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

    public @Nullable SVGPaint getPaint(@NotNull String key, Animatable animatable) {
        return getPaint(key, null, animatable);
    }

    @Contract("_,!null,_ -> !null")
    public @Nullable SVGPaint getPaint(@NotNull String key, @Nullable SVGPaint fallback, Animatable animatable) {
        SVGPaint value = getPaintInternal(key, fallback);
        if (animatable == Animatable.YES) {
            SVGPaint initial = value;
            if (initial == null) initial = PredefinedPaints.INHERITED;
            AnimatedPaint animatedPaint = getAnimatedPaint(key, initial);
            if (animatedPaint != null) return animatedPaint;
        }
        return value;
    }

    @Contract("_,!null -> !null")
    private @Nullable SVGPaint getPaintInternal(@NotNull String key, @Nullable SVGPaint fallback) {
        SVGPaint paint = parsePaint(getValue(key));
        if (paint == null) return fallback;
        return paint;
    }

    public @Nullable SVGPaint parsePaint(@Nullable String value) {
        if (value == null) return null;
        SVGPaint paint = getElementByUrl(SVGPaint.class, value);
        if (paint != null) return paint;
        return loadHelper.attributeParser().parsePaint(value, this);
    }

    public @Nullable Length getLength(@NotNull String key, @NotNull PercentageDimension dimension) {
        return getLength(key, dimension, (Length) null);
    }

    public @NotNull Length getLength(@NotNull String key, @NotNull PercentageDimension dimension, float fallback) {
        return getLength(key, dimension, Unit.RAW.valueOf(fallback));
    }

    @Contract("_,_,!null -> !null")
    public @Nullable Length getLength(@NotNull String key, @NotNull PercentageDimension dimension,
            @Nullable Length fallback) {
        return (Length) getLength(key, dimension, fallback, Animatable.NO);
    }

    public @Nullable LengthValue getLength(@NotNull String key, @NotNull PercentageDimension dimension,
            Animatable animatable) {
        return getLength(key, dimension, null, animatable);
    }

    @Contract("_,_,!null,_ -> !null")
    public @Nullable LengthValue getLength(@NotNull String key, @NotNull PercentageDimension dimension,
            @Nullable LengthValue fallback, Animatable animatable) {
        LengthValue value = getLengthInternal(key, dimension);
        if (value == FALLBACK_LENGTH) {
            value = fallback;
        }

        if (animatable == Animatable.YES) {
            LengthValue initial = value;
            if (initial == null) initial = Length.INHERITED;
            if (initial instanceof AnimatedLength) {
                initial = ((AnimatedLength) initial).initial();
            }
            AnimatedLength animatedLength = getAnimatedLength(key, initial, dimension);
            if (animatedLength != null) return animatedLength;
        }

        return value;
    }

    public @NotNull Duration getDuration(@NotNull String key, @NotNull Duration fallback) {
        return loadHelper.attributeParser().parseDuration(getValue(key), fallback);
    }

    private @NotNull Length getLengthInternal(@NotNull String key, @NotNull PercentageDimension dimension) {
        return loadHelper.attributeParser().parseLength(getValue(key), FALLBACK_LENGTH, dimension);
    }

    public @NotNull Length getHorizontalReferenceLength(@NotNull String key) {
        return parseReferenceLength(key, "left", "right", PercentageDimension.WIDTH);
    }

    public @NotNull Length getVerticalReferenceLength(@NotNull String key) {
        return parseReferenceLength(key, "top", "bottom", PercentageDimension.HEIGHT);
    }

    private @NotNull Length parseReferenceLength(@NotNull String key, @NotNull String topLeft,
            @NotNull String bottomRight, @NotNull PercentageDimension dimension) {
        String value = getValue(key);
        if (topLeft.equals(value)) {
            return TopOrLeft;
        } else if ("center".equals(value)) {
            return Center;
        } else if (bottomRight.equals(value)) {
            return BottomOrRight;
        } else {
            return loadHelper.attributeParser().parseLength(value, Length.ZERO, dimension);
        }
    }

    @Contract("_,!null -> !null")
    public @Nullable Percentage getPercentage(@NotNull String key, @Nullable Percentage fallback) {
        return loadHelper.attributeParser().parsePercentage(getValue(key), fallback);
    }

    public @Nullable PercentageValue getPercentage(@NotNull String key, Animatable animatable) {
        return getPercentage(key, null, animatable);
    }

    @Contract("_,!null,_ -> !null")
    public @Nullable PercentageValue getPercentage(@NotNull String key,
            @Nullable PercentageValue fallback,
            Animatable animatable) {
        PercentageValue value =
                loadHelper.attributeParser().parsePercentage(getValue(key), FALLBACK_PERCENTAGE);
        if (value == FALLBACK_PERCENTAGE) {
            value = fallback;
        }

        if (animatable == Animatable.YES) {
            PercentageValue initial = value;
            if (initial == null) initial = Percentage.INHERITED;
            if (initial instanceof AnimatedPercentage) {
                initial = ((AnimatedPercentage) initial).initial();
            }
            AnimatedPercentage animatedPercentage = getAnimatedPercentage(key, initial);
            if (animatedPercentage != null) return animatedPercentage;
        }
        return value;
    }

    public @NotNull Length @NotNull [] getLengthList(@NotNull String key, @NotNull PercentageDimension dimension) {
        return getLengthList(key, new Length[0], dimension);
    }


    @Contract("_,!null,_ -> !null")
    public @NotNull Length @Nullable [] getLengthList(@NotNull String key, @NotNull Length @Nullable [] fallback,
            @NotNull PercentageDimension dimension) {
        return loadHelper.attributeParser().parseLengthList(getValue(key), fallback, dimension);
    }

    public float @NotNull [] getFloatList(@NotNull String key) {
        return loadHelper.attributeParser().parseFloatList(getValue(key));
    }

    public @NotNull FloatListValue getFloatList(@NotNull String key, Animatable animatable) {
        float[] initial = loadHelper.attributeParser().parseFloatList(getValue(key));

        if (animatable == Animatable.YES) {
            AnimatedFloatList animatedLength = getAnimatedFloatList(key, initial);
            if (animatedLength != null) return animatedLength;
        }
        return new ConstantFloatList(initial);
    }

    public double @NotNull [] getDoubleList(@NotNull String key) {
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

    public @NotNull FilterChannelKey getFilterChannelKey(@NotNull String key, @NotNull FilterChannelKey fallback) {
        String in = getValue(key);
        if (in == null) return fallback;
        return new FilterChannelKey.StringKey(in);
    }

    public @Nullable TransformValue parseTransform(@NotNull String key) {
        List<TransformPart> parts = loadHelper.attributeParser().parseTransform(getValue(key));
        if (parts == null) return null;
        for (TransformPart part : parts) {
            if (!part.canBeFlattened()) return new ConstantLengthTransform(parts);
        }
        // Optimization: If all parts can be flattened we can just return a single AffineTransform.
        return new ConstantTransform(new ConstantLengthTransform(parts).get(DUMMY_MEASURE_CONTEXT));
    }

    public boolean hasAttribute(@NotNull String name) {
        return attributes.containsKey(name);
    }

    public @NotNull String @NotNull [] getStringList(@NotNull String name) {
        return getStringList(name, SeparatorMode.COMMA_AND_WHITESPACE);
    }


    public @NotNull String @NotNull [] getStringList(@NotNull String name, SeparatorMode separatorMode) {
        return loadHelper.attributeParser().parseStringList(getValue(name), separatorMode);
    }

    public float getFloat(@NotNull String name, float fallback) {
        return loadHelper.attributeParser().parseFloat(getValue(name), fallback);
    }

    public float getNonNegativeFloat(@NotNull String name, float fallback) {
        float value = getFloat(name, fallback);
        if (Float.isFinite(value) && value < 0) return fallback;
        return value;
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

    public @Nullable URI resolveResourceURI(@NotNull String url) {
        return loadHelper.externalResourcePolicy().resolveResourceURI(document().rootURI(), url);
    }

    private @Nullable Animate animateNode(@NotNull String property) {
        ParsedElement parsedElement = element.animationElements().get(property);
        if (parsedElement == null) return null;
        if (parsedElement.node() instanceof Animate) {
            Animate animate = (Animate) parsedElement.nodeEnsuringBuildStatus(document().currentNestingDepth());
            document().registerAnimatedElement(animate);
            return animate;
        }
        return null;
    }

    public @Nullable AnimatedLength getAnimatedLength(@NotNull String property, @NotNull LengthValue initial,
            @NotNull PercentageDimension dimension) {
        Animate animate = animateNode(property);
        if (animate == null) return null;
        return animate.animatedLength(initial, dimension, this);
    }

    private @Nullable AnimatedFloatList getAnimatedFloatList(@NotNull String property, float @NotNull [] initial) {
        Animate animate = animateNode(property);
        if (animate == null) return null;
        return animate.animatedFloatList(initial, this);
    }

    private @Nullable AnimatedPercentage getAnimatedPercentage(@NotNull String property,
            @NotNull PercentageValue initial) {
        Animate animate = animateNode(property);
        if (animate == null) return null;
        return animate.animatedPercentage(initial, this);
    }

    private @Nullable AnimatedPaint getAnimatedPaint(@NotNull String property, @NotNull SVGPaint initial) {
        Animate animate = animateNode(property);
        if (animate == null) return null;
        return animate.animatedPaint(initial, this);
    }

    public @Nullable AnimatedColor getAnimatedColor(@NotNull String property, @NotNull Color initial) {
        Animate animate = animateNode(property);
        if (animate == null) return null;
        return animate.animatedColor(initial, this);
    }
}
