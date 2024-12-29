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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
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

    public @Nullable SVGPaint getPaint(@NotNull String key, Inherited inherited, Animatable animatable) {
        return getPaint(key, null, inherited, animatable);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable SVGPaint getPaint(@NotNull String key, @Nullable SVGPaint fallback,
            Inherited inherited, Animatable animatable) {
        SVGPaint value = getPaintInternal(key, fallback);
        if (animatable == Animatable.YES) {
            SVGPaint initial = value;
            if (initial == null) {
                initial = inherited == Inherited.YES
                        ? PredefinedPaints.INHERITED
                        : PredefinedPaints.DEFAULT_PAINT;
            }
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
        return (Length) getLength(key, dimension, fallback, Inherited.NO, Animatable.NO);
    }

    public @Nullable LengthValue getLength(@NotNull String key, @NotNull PercentageDimension dimension,
            Inherited inherited, Animatable animatable) {
        return getLength(key, dimension, null, inherited, animatable);
    }

    @Contract("_,_,!null,_,_ -> !null")
    public @Nullable LengthValue getLength(@NotNull String key, @NotNull PercentageDimension dimension,
            @Nullable LengthValue fallback, Inherited inherited, Animatable animatable) {
        LengthValue value = getLengthInternal(key, dimension);
        if (value == FALLBACK_LENGTH) {
            value = fallback;
        }

        if (animatable == Animatable.YES) {
            LengthValue initial = value;
            if (initial == null) {
                initial = inherited == Inherited.YES
                        ? Length.INHERITED
                        : NeutralElements.NEUTRAL_LENGTH;
            }
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

    @Deprecated
    public float getPercentage(@NotNull String key, float fallback) {
        return loadHelper.attributeParser()
                .parsePercentage(getValue(key), new Percentage(fallback)).value();
    }

    @Contract("_,!null -> !null")
    public @Nullable Percentage getPercentage(@NotNull String key, @Nullable Percentage fallback) {
        return loadHelper.attributeParser().parsePercentage(getValue(key), fallback);
    }

    public @Nullable PercentageValue getPercentage(@NotNull String key, Inherited inherited, Animatable animatable) {
        return getPercentage(key, null, inherited, animatable);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable PercentageValue getPercentage(@NotNull String key, @Nullable PercentageValue fallback,
            Inherited inherited, Animatable animatable) {
        PercentageValue value =
                loadHelper.attributeParser().parsePercentage(getValue(key), FALLBACK_PERCENTAGE);
        if (value == FALLBACK_PERCENTAGE) {
            value = fallback;
        }

        if (animatable == Animatable.YES) {
            PercentageValue initial = value;
            if (initial == null) {
                initial = inherited == Inherited.YES
                        ? Percentage.INHERITED
                        : NeutralElements.NEUTRAL_PERCENTAGE;
            }
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

    public @NotNull FloatListValue getFloatList(@NotNull String key, Inherited inherited, Animatable animatable) {
        String value = getValue(key);
        float[] initialRaw = loadHelper.attributeParser().parseFloatList(getValue(key));

        FloatListValue initial = value != null
                ? new ConstantFloatList(initialRaw)
                : null;

        if (animatable == Animatable.YES) {
            if (initial == null) {
                if (inherited == Inherited.YES) {
                    throw new IllegalStateException("Inherited values for float lists aren't implemented yet");
                }
                initial = NeutralElements.NEUTRAL_FLOAT_LIST;
            }
            AnimatedFloatList animatedLength = getAnimatedFloatList(key, initial);
            if (animatedLength != null) return animatedLength;
        }
        return initial != null ? initial : ConstantFloatList.EMPTY;
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
        return parseTransform(key, Inherited.NO, Animatable.NO);
    }

    private @NotNull TransformValue createTransformValueFromParts(@NotNull List<TransformPart> parts) {
        for (TransformPart part : parts) {
            if (!part.canBeFlattened()) return new ConstantLengthTransform(parts);
        }
        // Optimization: If all parts can be flattened we can just return a single AffineTransform.
        return new ConstantTransform(new ConstantLengthTransform(parts).get(DUMMY_MEASURE_CONTEXT));
    }

    public @Nullable TransformValue parseTransform(@NotNull String key, Inherited inherited, Animatable animatable) {
        List<TransformPart> parts = loadHelper.attributeParser().parseTransform(getValue(key));
        TransformValue value = parts != null
                ? createTransformValueFromParts(parts)
                : null;

        if (animatable == Animatable.YES) {
            TransformValue initial = value;
            if (initial == null) {
                initial = inherited == Inherited.YES
                        ? ConstantLengthTransform.INHERITED
                        : NeutralElements.NEUTRAL_TRANSFORM;
            }
            if (initial instanceof AnimatedTransform) {
                initial = ((AnimatedTransform) initial).initial();
            }
            AnimatedTransform animatedTransform = getAnimatedTransform(key, initial);
            if (animatedTransform != null) return animatedTransform;
        }

        return value;
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

    private <T extends BaseAnimationNode> List<@NotNull T> animateNodes(@NotNull String property, Class<T> type) {
        List<ParsedElement> parsedElements = element.animationElements().get(property);
        if (parsedElements == null || parsedElements.isEmpty()) return Collections.emptyList();

        List<T> animateNodes = parsedElements
                .stream()
                .filter(n -> type.isInstance(n.node()))
                .map(n -> type.cast(n.nodeEnsuringBuildStatus(document().currentNestingDepth())))
                .collect(Collectors.toList());
        for (T animateNode : animateNodes) {
            document().registerAnimatedElement(animateNode);
        }
        return animateNodes;
    }

    private static <T, A extends T, N extends BaseAnimationNode> @Nullable A makeAnimated(
            @NotNull List<N> animationNodes,
            @NotNull T initial,
            @NotNull BiFunction<N, T, A> factory) {
        if (animationNodes.isEmpty()) return null;

        @NotNull T currentInitial = initial;
        @Nullable A lastAnimationValue = null;
        for (N animate : animationNodes) {
            A animated = factory.apply(animate, currentInitial);
            if (animated != null) {
                currentInitial = animated;
                lastAnimationValue = animated;
            }
        }
        return lastAnimationValue;
    }

    public @Nullable AnimatedLength getAnimatedLength(@NotNull String property, @NotNull LengthValue initial,
            @NotNull PercentageDimension dimension) {
        return makeAnimated(
                animateNodes(property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedLength(currentInitial, dimension, this));
    }

    private @Nullable AnimatedFloatList getAnimatedFloatList(@NotNull String property,
            @NotNull FloatListValue initial) {
        return makeAnimated(
                animateNodes(property, Animate.class), initial,
                // NOTE: For some reason on some configurations the compiler needs these type hints here
                (Animate animate, FloatListValue currentInitial) -> animate.animatedFloatList(currentInitial, this));
    }

    private @Nullable AnimatedPercentage getAnimatedPercentage(@NotNull String property,
            @NotNull PercentageValue initial) {
        return makeAnimated(
                animateNodes(property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedPercentage(currentInitial, this));
    }

    private @Nullable AnimatedPaint getAnimatedPaint(@NotNull String property, @NotNull SVGPaint initial) {
        return makeAnimated(
                animateNodes(property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedPaint(currentInitial, this));
    }

    public @Nullable AnimatedColor getAnimatedColor(@NotNull String property, @NotNull ColorValue initial) {
        return makeAnimated(
                animateNodes(property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedColor(currentInitial, this));
    }

    public @Nullable AnimatedTransform getAnimatedTransform(@NotNull String property,
            @NotNull TransformValue initial) {
        return makeAnimated(
                animateNodes(property, AnimateTransform.class), initial,
                (animate, currentInitial) -> animate.animatedTransform(currentInitial, this));
    }
}
