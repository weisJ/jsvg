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
package com.github.weisj.jsvg.parser.impl;

import java.awt.Color;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.time.Duration;
import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedFloatList;
import com.github.weisj.jsvg.animation.value.AnimatedLength;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.animation.value.AnimatedPercentage;
import com.github.weisj.jsvg.animation.value.AnimatedTransform;
import com.github.weisj.jsvg.animation.value.NeutralElements;
import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.attributes.value.ConstantFloatList;
import com.github.weisj.jsvg.attributes.value.ConstantLengthTransform;
import com.github.weisj.jsvg.attributes.value.ConstantTransform;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.Length;
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
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.parser.PaintParser;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Declaration;
import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.ShorthandExpander;
import com.github.weisj.jsvg.parser.css.impl.phase4matcher.CascadeResult;
import com.github.weisj.jsvg.parser.css.impl.phase4matcher.StyleSheets;
import com.github.weisj.jsvg.parser.resources.ResourceLoader;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.util.AttributeUtil;
import com.github.weisj.jsvg.view.ViewBox;

public final class AttributeNode {

    public enum ElementRelation {
        GEOMETRY_DATA,
        PAINTED_CHILD,
        TEMPLATE,
        PAINT_SERVER
    }

    private static final Length Top = new Length(Unit.PERCENTAGE_HEIGHT, 0f);
    private static final Length CenterHeight = new Length(Unit.PERCENTAGE_HEIGHT, 50f);
    private static final Length Bottom = new Length(Unit.PERCENTAGE_HEIGHT, 100f);
    private static final Length Left = new Length(Unit.PERCENTAGE_WIDTH, 0f);
    private static final Length CenterWidth = new Length(Unit.PERCENTAGE_WIDTH, 50f);
    private static final Length Right = new Length(Unit.PERCENTAGE_WIDTH, 100f);
    private static final Length FALLBACK_LENGTH = new Length(Unit.RAW, 0f);
    private static final Percentage FALLBACK_PERCENTAGE = new Percentage(1f);
    private static final MeasureContext DUMMY_MEASURE_CONTEXT =
            new MeasureContext(0, 0, 0, 0, 0, new AnimationState(0, 0));

    /**
     * Presentation attributes that are also CSS properties (SVG 2 §6.1): when declared on an element their value is
     * run through the CSS grammar and shorthands expanded. Non-shorthand properties are parsed on demand, so this list
     * need not be complete.
     */
    private static final Set<String> CSS_ATTRIBUTES = new HashSet<>(Arrays.asList(
            "fill", "fill-opacity", "fill-rule",
            "stroke", "stroke-opacity", "stroke-width", "stroke-linecap", "stroke-linejoin",
            "stroke-miterlimit", "stroke-dasharray", "stroke-dashoffset",
            "color", "opacity",
            "font", "font-family", "font-size", "font-size-adjust", "font-stretch",
            "font-style", "font-variant", "font-weight", "line-height",
            "marker", "marker-start", "marker-mid", "marker-end",
            "text-anchor", "dominant-baseline", "alignment-baseline", "baseline-shift",
            "direction", "letter-spacing", "word-spacing", "text-decoration", "writing-mode",
            "visibility", "display", "overflow",
            "color-interpolation", "color-interpolation-filters", "shape-rendering", "image-rendering",
            "clip", "clip-path", "clip-rule", "mask", "filter",
            "stop-color", "stop-opacity", "flood-color", "flood-opacity", "lighting-color",
            "paint-order", "vector-effect", "mix-blend-mode", "isolation", "pointer-events"));

    private final @NotNull String tagName;
    /** SVG attributes declared directly on the node; used for matching attribute selectors */
    private final @NotNull Map<@NotNull String, @NotNull String> declaredAttributes;
    private final @NotNull Map<@NotNull String, @NotNull AttributeValue> attributes = new HashMap<>();
    private final @NotNull StyleSheets styleSheets;
    private boolean selectorsUseElementPositionInDom;

    private ParsedElement element = null;

    public AttributeNode(@NotNull String tagName, @NotNull Map<@NotNull String, @NotNull String> declaredAttributes,
            @NotNull StyleSheets styleSheets) {
        this.tagName = tagName;
        this.declaredAttributes = declaredAttributes;
        this.styleSheets = styleSheets;
    }

    void setElement(ParsedElement element) {
        this.element = element;
    }

    private @NotNull LoadHelper loadHelper() {
        return document().loadHelper();
    }

    public @NotNull AttributeNode copy() {
        AttributeNode node = new AttributeNode(tagName, new HashMap<>(declaredAttributes), styleSheets);
        node.attributes.putAll(attributes);
        node.setElement(element);
        return node;
    }

    /**
     * Copy for re-matching at a new DOM position: carries over only the declared attributes, leaving the resolved
     * {@link #attributes} and {@link #selectorsUseElementPositionInDom} empty so {@link #prepareForNodeBuilding()}
     * re-runs the cascade (unlike {@link #copy()}, which keeps resolved values). Caller must
     * {@link #setElement(ParsedElement)}.
     */
    @NotNull
    AttributeNode copyForReparse() {
        return new AttributeNode(tagName, new HashMap<>(declaredAttributes), styleSheets);
    }

    void prepareForNodeBuilding() {
        CssParser cssParser = document().loaderContext().cssParser();

        // Presentation attributes: CSS properties are tokenized and shorthand-expanded so the CSS grammar
        // applies to them; other (SVG-only) attributes are kept as raw strings.
        for (Map.Entry<String, String> entry : declaredAttributes.entrySet()) {
            if (CSS_ATTRIBUTES.contains(entry.getKey())) {
                Declaration declaration =
                        new Declaration(entry.getKey(), cssParser.parseCssAttribute(entry.getValue()), false);
                for (NormalizedProperty property : ShorthandExpander.expand(declaration)) {
                    attributes.put(property.name(), new AttributeValue.Parsed(property.value()));
                }
            } else {
                attributes.put(entry.getKey(), new AttributeValue.PlainString(entry.getValue()));
            }
        }

        String styleStr = declaredAttributes.get("style");
        List<NormalizedProperty> inlineCssDeclarations = styleStr != null && !AttributeUtil.isBlank(styleStr)
                ? cssParser.parseStyleAttribute(styleStr, document().loaderContext().cssHints())
                : Collections.emptyList();

        // FIXME: Only use the highest priority *valid* definition of a property value.
        CascadeResult cascadeResult = styleSheets().matchAndCascade(inlineCssDeclarations, element);
        selectorsUseElementPositionInDom = cascadeResult.selectorsUseElementPositionInDom;
        // CSS attributes override SVG presentation attributes (CSS Cascade 4 §6.4).
        attributes.putAll(cascadeResult.attributes);
    }

    public boolean selectorsUseElementPositionInDom() {
        return selectorsUseElementPositionInDom;
    }

    void orSelectorsUseElementPositionInDom(boolean value) {
        selectorsUseElementPositionInDom |= value;
    }

    public @NotNull ParsedDocument document() {
        return element().document();
    }

    public @NotNull ParsedElement element() {
        return element;
    }

    @NotNull
    StyleSheets styleSheets() {
        return styleSheets;
    }

    private <T> @Nullable T getElementByUrl(@NotNull Class<T> type, @Nullable String value) {
        if (value == null) return null;
        return loadHelper().elementLoader().loadElement(type, value, document());
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

    public @NotNull Map<String, String> declaredAttributes() {
        return declaredAttributes;
    }

    public @NotNull Map<String, AttributeValue> attributes() {
        return attributes;
    }

    /** Sets a raw (non-CSS) string value on the resolved attribute map, e.g. synthesized scratch attributes. */
    public void setValue(@NotNull String key, @NotNull String value) {
        attributes.put(key, new AttributeValue.PlainString(value));
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

    public @NotNull List<@NotNull String> classNames() {
        @NotNull String @NotNull [] classes = getStringList("class", SeparatorMode.WHITESPACE_ONLY);
        if (classes.length == 0) return Collections.emptyList();
        return Arrays.asList(classes);
    }

    /**
     * Resolved value as CSS text. {@link AttributeValue.Parsed} tokens are serialized back per §5.2; prefer
     * {@link #tokensOf} where a token-consuming parser exists, to avoid the serialize/re-parse round-trip.
     */
    public @Nullable String getValue(@NotNull String key) {
        AttributeValue value = attributes.get(key);
        if (value instanceof AttributeValue.PlainString) {
            return ((AttributeValue.PlainString) value).value;
        }
        if (value instanceof AttributeValue.Parsed) {
            return ParserUtil.serialize(((AttributeValue.Parsed) value).tokens);
        }
        return null;
    }

    /** Tokens of a CSS-sourced value, or {@code null} for a raw string / absent attribute. */
    private @Nullable List<@NotNull ComponentValue> tokensOf(@NotNull String key) {
        AttributeValue value = attributes.get(key);
        return value instanceof AttributeValue.Parsed ? ((AttributeValue.Parsed) value).tokens : null;
    }

    private static @Nullable ComponentValue singleToken(@NotNull List<@NotNull ComponentValue> tokens) {
        ComponentValue found = null;
        for (ComponentValue token : tokens) {
            if (token instanceof Token && ((Token) token).type() == TokenType.WHITESPACE) continue;
            if (found != null) return null;
            found = token;
        }
        return found;
    }

    /** Whether the value is exactly the given keyword, reading the ident token directly (no serialize). */
    public boolean valueIsIdent(@NotNull String key, @NotNull String ident) {
        List<ComponentValue> tokens = tokensOf(key);
        if (tokens != null) {
            ComponentValue token = singleToken(tokens);
            return token instanceof Token.Ident && ((Token.Ident) token).name().equals(ident);
        }
        return ident.equals(getValue(key));
    }

    public @NotNull Color getColor(@NotNull String key) {
        return getColor(key, PaintParser.DEFAULT_COLOR);
    }

    @Contract("_,!null -> !null")
    public @Nullable Color getColor(@NotNull String key, @Nullable Color fallback) {
        List<ComponentValue> tokens = tokensOf(key);
        Color c;
        if (tokens != null) {
            c = loadHelper().attributeParser().paintParser().parseColor(tokens);
        } else {
            String value = getValue(key);
            if (value == null) return fallback;
            c = loadHelper().attributeParser().paintParser().parseColor(value.toLowerCase(Locale.ENGLISH));
        }
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
        List<ComponentValue> tokens = tokensOf(key);
        SVGPaint paint = tokens != null ? parsePaint(tokens) : parsePaint(getValue(key));
        return paint == null ? fallback : paint;
    }

    public @Nullable SVGPaint parsePaint(@Nullable String value) {
        if (value == null) return null;
        // TODO: url(#...) allows specifying a fallback color value.
        SVGPaint paint = getElementByHref(SVGPaint.class, value, ElementRelation.PAINT_SERVER);
        if (paint != null) return paint;
        return loadHelper().attributeParser().parsePaint(value, this);
    }

    private @Nullable SVGPaint parsePaint(@NotNull List<@NotNull ComponentValue> tokens) {
        for (ComponentValue token : tokens) {
            if (token instanceof Token.Url) {
                // TODO: url(#...) allows specifying a fallback color value.
                SVGPaint paint = getElementByHref(
                        SVGPaint.class, ((Token.Url) token).value(), ElementRelation.PAINT_SERVER);
                if (paint != null) return paint;
                break;
            }
        }
        return loadHelper().attributeParser().parsePaint(tokens);
    }

    public @Nullable Length getLength(@NotNull String key, @NotNull PercentageDimension dimension) {
        return getLength(key, dimension, null);
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
        return loadHelper().attributeParser().parseTimeOffsetValue(getValue(key), fallback);
    }

    private @NotNull Length getLengthInternal(@NotNull String key, @NotNull PercentageDimension dimension) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseLength(tokens, FALLBACK_LENGTH, dimension)
                : loadHelper().attributeParser().parseLength(getValue(key), FALLBACK_LENGTH, dimension);
    }

    public @NotNull Length getHorizontalReferenceLengthFromKey(@NotNull String key) {
        return getHorizontalReferenceLength(getValue(key));
    }

    public @NotNull Length getVerticalReferenceLengthFromKey(@NotNull String key) {
        return getVerticalReferenceLength(getValue(key));
    }

    public @NotNull Length getHorizontalReferenceLength(@Nullable String value) {
        if ("left".equals(value)) {
            return Left;
        } else if ("center".equals(value)) {
            return CenterWidth;
        } else if ("right".equals(value)) {
            return Right;
        } else {
            return loadHelper().attributeParser().parseLength(value, Length.ZERO, PercentageDimension.WIDTH);
        }
    }

    public @NotNull Length getVerticalReferenceLength(@Nullable String value) {
        if ("top".equals(value)) {
            return Top;
        } else if ("center".equals(value)) {
            return CenterHeight;
        } else if ("bottom".equals(value)) {
            return Bottom;
        } else {
            return loadHelper().attributeParser().parseLength(value, Length.ZERO, PercentageDimension.HEIGHT);
        }
    }

    public boolean isHorizontalKeyword(@NotNull String value) {
        return "left".equals(value) || "right".equals(value);
    }

    public boolean isVerticalKeyword(@NotNull String value) {
        return "top".equals(value) || "bottom".equals(value);
    }

    @Contract("_,!null -> !null")
    public @Nullable Percentage getPercentage(@NotNull String key, @Nullable Percentage fallback) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parsePercentage(tokens, fallback)
                : loadHelper().attributeParser().parsePercentage(getValue(key), fallback);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable Percentage getPercentage(@NotNull String key, @Nullable Percentage fallback, float min,
            float max) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parsePercentage(tokens, fallback, min, max)
                : loadHelper().attributeParser().parsePercentage(getValue(key), fallback, min, max);
    }

    public @Nullable PercentageValue getPercentage(@NotNull String key, Inherited inherited, Animatable animatable) {
        return getPercentage(key, null, inherited, animatable);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable PercentageValue getPercentage(@NotNull String key, @Nullable PercentageValue fallback,
            Inherited inherited, Animatable animatable) {
        List<ComponentValue> tokens = tokensOf(key);
        PercentageValue value = tokens != null
                ? loadHelper().attributeParser().parsePercentage(tokens, FALLBACK_PERCENTAGE)
                : loadHelper().attributeParser().parsePercentage(getValue(key), FALLBACK_PERCENTAGE);
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
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseLengthList(tokens, fallback, dimension)
                : loadHelper().attributeParser().parseLengthList(getValue(key), fallback, dimension);
    }

    public float @NotNull [] getFloatList(@NotNull String key) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseFloatList(tokens)
                : loadHelper().attributeParser().parseFloatList(getValue(key));
    }

    public @NotNull FloatListValue getFloatList(@NotNull String key, Inherited inherited, Animatable animatable) {
        List<ComponentValue> tokens = tokensOf(key);
        boolean hasValue;
        float[] initialRaw;
        if (tokens != null) {
            hasValue = true;
            initialRaw = loadHelper().attributeParser().parseFloatList(tokens);
        } else {
            String value = getValue(key);
            hasValue = value != null;
            initialRaw = loadHelper().attributeParser().parseFloatList(value);
        }

        FloatListValue initial = hasValue
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
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseDoubleList(tokens)
                : loadHelper().attributeParser().parseDoubleList(getValue(key));
    }

    public <E extends Enum<E>> @NotNull E getEnum(@NotNull String key, @NotNull E fallback) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseEnum(tokens, fallback)
                : loadHelper().attributeParser().parseEnum(getValue(key), fallback);
    }

    public <E extends Enum<E>> @Nullable E getEnumNullable(@NotNull String key, @NotNull Class<E> enumType) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseEnum(tokens, enumType)
                : loadHelper().attributeParser().parseEnum(getValue(key), enumType);
    }

    public @Nullable ClipPath getClipPath() {
        return referencedElement(ClipPath.class, "clip-path");
    }

    public @Nullable Mask getMask() {
        return referencedElement(Mask.class, "mask");
    }

    public @Nullable Filter getFilter() {
        return referencedElement(Filter.class, "filter");
    }

    /** Resolves a {@code url(#id)} reference, reading the id from the {@link Token.Url} without serializing. */
    private <T> @Nullable T referencedElement(@NotNull Class<T> type, @NotNull String key) {
        List<ComponentValue> tokens = tokensOf(key);
        if (tokens == null) return getElementByUrl(type, getValue(key));
        for (ComponentValue token : tokens) {
            if (token instanceof Token.Url) return getElementByUrl(type, ((Token.Url) token).value());
        }
        return null;
    }

    /** Like {@link #getElementByHref}, but resolves by attribute key and reads {@code url(#id)} from tokens. */
    public <T> @Nullable T getReference(@NotNull Class<T> type, @NotNull String key,
            @NotNull ElementRelation relation) {
        List<ComponentValue> tokens = tokensOf(key);
        if (tokens == null) return getElementByHref(type, getValue(key), relation);
        for (ComponentValue token : tokens) {
            if (token instanceof Token.Url) {
                return getElementByHref(type, ((Token.Url) token).value(), relation);
            }
        }
        return null;
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
        List<ComponentValue> tokens = tokensOf(key);
        List<TransformPart> parts = tokens != null
                ? loadHelper().attributeParser().parseTransform(tokens)
                : loadHelper().attributeParser().parseTransform(getValue(key));
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
        List<ComponentValue> tokens = tokensOf(name);
        return tokens != null
                ? loadHelper().attributeParser().parseStringList(tokens, separatorMode)
                : loadHelper().attributeParser().parseStringList(getValue(name), separatorMode);
    }

    public float getFloat(@NotNull String name, float fallback) {
        List<ComponentValue> tokens = tokensOf(name);
        return tokens != null
                ? loadHelper().attributeParser().parseFloat(tokens, fallback)
                : loadHelper().attributeParser().parseFloat(getValue(name), fallback);
    }

    public float getNonNegativeFloat(@NotNull String name, float fallback) {
        float value = getFloat(name, fallback);
        if (Float.isFinite(value) && value < 0) return fallback;
        return value;
    }

    public int getInt(@NotNull String key, int fallback) {
        List<ComponentValue> tokens = tokensOf(key);
        return tokens != null
                ? loadHelper().attributeParser().parseInt(tokens, fallback)
                : loadHelper().attributeParser().parseInt(getValue(key), fallback);
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
        return loadHelper().attributeParser();
    }

    public @NotNull ResourceLoader resourceLoader() {
        return loadHelper().resourceLoader();
    }

    public @Nullable URI resolveResourceURI(@NotNull String url) {
        return loadHelper().externalResourcePolicy().resolveResourceURI(document().rootURI(), url);
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
