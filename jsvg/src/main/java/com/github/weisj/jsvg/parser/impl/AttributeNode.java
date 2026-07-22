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
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
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
    private static final MeasureContext DUMMY_MEASURE_CONTEXT =
            new MeasureContext(0, 0, 0, 0, 0, 0, new AnimationState(0, 0));

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
            "solid-color", "solid-opacity",
            "paint-order", "vector-effect", "mix-blend-mode", "isolation", "pointer-events",
            "transform", "transform-origin", "transform-box", "transform-style"));

    private final @NotNull String tagName;
    /** SVG attributes declared directly on the node; used for matching attribute selectors */
    private final @NotNull Map<@NotNull String, @NotNull String> declaredAttributes;
    private final @NotNull Map<@NotNull String, @NotNull AttributeValue> resolvedAttributes = new HashMap<>();
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
        node.resolvedAttributes.putAll(resolvedAttributes);
        node.setElement(element);
        return node;
    }

    /**
     * Copy for re-matching at a new DOM position: carries over only the declared attributes, leaving the resolved
     * {@link #resolvedAttributes} and {@link #selectorsUseElementPositionInDom} empty so {@link #prepareForNodeBuilding()}
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
                    resolvedAttributes.put(property.name(), new AttributeValue.Parsed(property.value()));
                }
            } else {
                resolvedAttributes.put(entry.getKey(), new AttributeValue.PlainString(entry.getValue()));
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
        resolvedAttributes.putAll(cascadeResult.attributes);
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
        return resolvedAttributes;
    }

    /** Sets a raw (non-CSS) string value on the resolved attribute map, e.g. synthesized scratch attributes. */
    public void setResolvedNonCssValue(@NotNull String key, @NotNull String value) {
        resolvedAttributes.put(key, new AttributeValue.PlainString(value));
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

    /** Returns the tokens created by the CSS parser, or {@code null} if the attribute is not set
     * or is not a CSS attribute. */
    public @Nullable List<@NotNull ComponentValue> getTokens(@NotNull String key) {
        AttributeValue attributeValue = resolvedAttributes.get(key);
        if (!(attributeValue instanceof AttributeValue.Parsed)) return null;
        return ((AttributeValue.Parsed) attributeValue).tokens();
    }

    /** Raw string of an SVG-only attribute; re-serialized CSS text in the unexpected case when a stylesheet
     * sets an SVG-only attribute. */
    public @Nullable String getValue(@NotNull String key) {
        AttributeValue value = resolvedAttributes.get(key);
        if (value == null) return null;
        if (value instanceof AttributeValue.PlainString) {
            return ((AttributeValue.PlainString) value).string();
        } else {
            return ((AttributeValue.Parsed) value).reserialize();
        }
    }

    /** Shared token-vs-string dispatch; {@code absent} when there is no value. */
    private <T> T parseValue(@NotNull String key, T absent,
            @NotNull Function<@NotNull List<@NotNull ComponentValue>, T> fromTokens,
            @NotNull Function<@NotNull String, T> fromString) {
        AttributeValue value = resolvedAttributes.get(key);
        if (value == null) return absent;
        if (value instanceof AttributeValue.Parsed) {
            return fromTokens.apply(((AttributeValue.Parsed) value).tokens());
        } else {
            return fromString.apply(((AttributeValue.PlainString) value).string());
        }
    }

    /** Whether the value is exactly the given keyword, reading the ident token directly (no serialize). */
    public boolean valueIsOneOfKeywords(@NotNull String key, @NotNull String... keywords) {
        // Callers (display, visibility) are CSS properties, so a set value is token-valued.
        List<ComponentValue> tokens = getTokens(key);
        if (tokens == null) return false;
        ComponentValue token = AttributeParser.singleToken(tokens);
        return token != null && token.isOneOfKeywords(keywords);
    }

    public @NotNull Color getColor(@NotNull String key) {
        return getColor(key, PaintParser.DEFAULT_COLOR);
    }

    @Contract("_,!null -> !null")
    public @Nullable Color getColor(@NotNull String key, @Nullable Color fallback) {
        // Color attributes are all CSS properties, so a set value is token-valued.
        List<ComponentValue> tokens = getTokens(key);
        Color c = tokens != null ? parser().paintParser().parseColor(tokens) : null;
        return c != null ? c : fallback;
    }

    public @Nullable SVGPaint getPaint(@NotNull String key, Inherited inherited, Animatable animatable) {
        return getPaint(key, null, inherited, animatable);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable SVGPaint getPaint(@NotNull String key, @Nullable SVGPaint fallback,
            Inherited inherited, Animatable animatable) {
        // Paint attributes are all CSS properties, so a set value is token-valued.
        List<ComponentValue> tokens = getTokens(key);
        SVGPaint paint = tokens != null ? parsePaint(tokens) : null;
        SVGPaint value = paint != null ? paint : fallback;
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

    public @Nullable SVGPaint parsePaint(@Nullable String value) {
        if (value == null) return null;
        // TODO: url(#...) allows specifying a fallback color value.
        SVGPaint paint = getElementByHref(SVGPaint.class, value, ElementRelation.PAINT_SERVER);
        if (paint != null) return paint;
        return parser().parsePaint(value, this);
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
        return parser().parsePaint(tokens);
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

    /** SMIL-only, so never CSS-sourced. */
    public @NotNull Duration getDuration(@NotNull String key, @NotNull Duration fallback) {
        return parser().parseTimeOffsetValue(getValue(key), fallback);
    }

    private @NotNull Length getLengthInternal(@NotNull String key, @NotNull PercentageDimension dimension) {
        return parseValue(key, FALLBACK_LENGTH,
                tokens -> parser().parseLength(tokens, FALLBACK_LENGTH, dimension),
                text -> parser().parseLength(text, FALLBACK_LENGTH, dimension));
    }

    public @NotNull Length getHorizontalReferenceLengthFromKey(@NotNull String key) {
        return parseValue(key, Length.ZERO,
                this::getHorizontalReferenceLength,
                this::getHorizontalReferenceLength);
    }

    public @NotNull Length getVerticalReferenceLengthFromKey(@NotNull String key) {
        return parseValue(key, Length.ZERO,
                this::getVerticalReferenceLength,
                this::getVerticalReferenceLength);
    }

    private static @Nullable Length horizontalKeyword(@Nullable String value) {
        if ("left".equalsIgnoreCase(value)) return Left;
        if ("center".equalsIgnoreCase(value)) return CenterWidth;
        if ("right".equalsIgnoreCase(value)) return Right;
        return null;
    }

    private static @Nullable Length verticalKeyword(@Nullable String value) {
        if ("top".equalsIgnoreCase(value)) return Top;
        if ("center".equalsIgnoreCase(value)) return CenterHeight;
        if ("bottom".equalsIgnoreCase(value)) return Bottom;
        return null;
    }

    public @NotNull Length getHorizontalReferenceLength(@Nullable String value) {
        Length keyword = horizontalKeyword(value);
        return keyword != null
                ? keyword
                : parser().parseLength(value, Length.ZERO, PercentageDimension.WIDTH);
    }

    public @NotNull Length getVerticalReferenceLength(@Nullable String value) {
        Length keyword = verticalKeyword(value);
        return keyword != null
                ? keyword
                : parser().parseLength(value, Length.ZERO, PercentageDimension.HEIGHT);
    }

    public @NotNull Length getHorizontalReferenceLength(@NotNull List<@NotNull ComponentValue> tokens) {
        Length keyword = horizontalKeyword(AttributeParser.identOf(tokens));
        return keyword != null
                ? keyword
                : parser().parseLength(tokens, Length.ZERO, PercentageDimension.WIDTH);
    }

    public @NotNull Length getVerticalReferenceLength(@NotNull List<@NotNull ComponentValue> tokens) {
        Length keyword = verticalKeyword(AttributeParser.identOf(tokens));
        return keyword != null
                ? keyword
                : parser().parseLength(tokens, Length.ZERO, PercentageDimension.HEIGHT);
    }

    public boolean isHorizontalKeyword(@NotNull List<@NotNull ComponentValue> tokens) {
        ComponentValue token = AttributeParser.singleToken(tokens);
        return token != null && token.isOneOfKeywords("left", "right");
    }

    public boolean isVerticalKeyword(@NotNull List<@NotNull ComponentValue> tokens) {
        ComponentValue token = AttributeParser.singleToken(tokens);
        return token != null && token.isOneOfKeywords("top", "bottom");
    }

    @Contract("_,!null -> !null")
    public @Nullable Percentage getPercentage(@NotNull String key, @Nullable Percentage fallback) {
        return parseValue(key, fallback,
                tokens -> parser().parsePercentage(tokens, fallback),
                text -> parser().parsePercentage(text, fallback));
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable Percentage getPercentage(@NotNull String key, @Nullable Percentage fallback, float min,
            float max) {
        // Sole caller (font-stretch) is a CSS property, so a set value is token-valued.
        List<ComponentValue> tokens = getTokens(key);
        return tokens != null ? parser().parsePercentage(tokens, fallback, min, max) : fallback;
    }

    public @Nullable PercentageValue getPercentage(@NotNull String key, Inherited inherited, Animatable animatable) {
        return getPercentage(key, null, inherited, animatable);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable PercentageValue getPercentage(@NotNull String key, @Nullable PercentageValue fallback,
            Inherited inherited, Animatable animatable) {
        PercentageValue value = getPercentage(key, (Percentage) null);
        if (value == null) {
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
        return parseValue(key, fallback,
                tokens -> parser().parseLengthList(tokens, fallback, dimension),
                text -> parser().parseLengthList(text, fallback, dimension));
    }

    public float @NotNull [] getFloatList(@NotNull String key) {
        return parser().parseFloatList(getValue(key));
    }

    public @NotNull FloatListValue getFloatList(@NotNull String key, Inherited inherited, Animatable animatable) {
        FloatListValue initial = hasAttribute(key)
                ? new ConstantFloatList(getFloatList(key))
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
        return parser().parseDoubleList(getValue(key));
    }

    public <E extends Enum<E>> @NotNull E getEnum(@NotNull String key, @NotNull E fallback) {
        E parsed = getEnumNullable(key, fallback.getDeclaringClass());
        return parsed != null ? parsed : fallback;
    }

    public <E extends Enum<E>> @Nullable E getEnumNullable(@NotNull String key, @NotNull Class<E> enumType) {
        return parseValue(key, null,
                tokens -> parser().parseEnum(tokens, enumType),
                text -> parser().parseEnum(text, enumType));
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

    /** Resolves a {@code url(#id)} reference, reading the id from tokens via {@link #urlOf} without serializing. */
    private <T> @Nullable T referencedElement(@NotNull Class<T> type, @NotNull String key) {
        // clip-path/mask/filter are CSS properties, so a set value is token-valued.
        List<ComponentValue> tokens = getTokens(key);
        if (tokens == null) return null;
        String url = urlOf(tokens);
        return url != null ? getElementByUrl(type, url) : null;
    }

    /** Like {@link #getElementByHref}, but resolves by attribute key and reads {@code url(#id)} via {@link #urlOf}. */
    public <T> @Nullable T getReference(@NotNull Class<T> type, @NotNull String key,
            @NotNull ElementRelation relation) {
        // marker/marker-* are CSS properties, so a set value is token-valued.
        List<ComponentValue> tokens = getTokens(key);
        if (tokens == null) return null;
        String url = urlOf(tokens);
        return url != null ? getElementByHref(type, url, relation) : null;
    }

    /** The id of a lone {@code url(#id)}: bare {@link Token.Url}, or a quoted {@code url("#id")} function block. */
    private static @Nullable String urlOf(@NotNull List<@NotNull ComponentValue> tokens) {
        ComponentValue token = AttributeParser.singleToken(tokens);
        if (token instanceof Token.Url) return ((Token.Url) token).value();
        if (token instanceof ComponentValue.FunctionBlock) {
            ComponentValue.FunctionBlock function = (ComponentValue.FunctionBlock) token;
            if (!function.name().equalsIgnoreCase("url")) return null;
            ComponentValue functionArgument = AttributeParser.singleToken(function.value());
            if (!(functionArgument instanceof Token.Str)) return null;
            return ((Token.Str) functionArgument).value();
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

    private @Nullable List<TransformPart> parseTransformParts(@NotNull String key) {
        return parseValue(key, null,
                tokens -> parser().parseTransform(tokens),
                text -> parser().parseTransform(text));
    }

    private @NotNull TransformValue createTransformValueFromParts(@NotNull List<TransformPart> parts) {
        for (TransformPart part : parts) {
            if (!part.canBeFlattened()) return new ConstantLengthTransform(parts);
        }
        // Optimization: If all parts can be flattened we can just return a single AffineTransform.
        return new ConstantTransform(new ConstantLengthTransform(parts).get(DUMMY_MEASURE_CONTEXT));
    }

    public @Nullable TransformValue parseTransform(@NotNull String key, Inherited inherited, Animatable animatable) {
        List<TransformPart> parts = parseTransformParts(key);
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
        return resolvedAttributes.containsKey(name);
    }

    /** Token-list form of {@link #getStringList}; {@code null} if not CSS-sourced, empty if it split to nothing. */
    public @Nullable List<@NotNull List<@NotNull ComponentValue>> getSplitTokenList(@NotNull String name,
            @NotNull SeparatorMode separatorMode) {
        List<ComponentValue> tokens = getTokens(name);
        return tokens != null ? parser().splitList(tokens, separatorMode) : null;
    }

    /** String form for SVG-only (never-tokenized) attributes; CSS-sourced ones use {@link #getSplitTokenList}. */
    public @NotNull String @NotNull [] getStringList(@NotNull String name, SeparatorMode separatorMode) {
        return loadHelper().attributeParser().parseStringList(getValue(name), separatorMode);
    }

    public float getFloat(@NotNull String name, float fallback) {
        return parseValue(name, fallback,
                tokens -> parser().parseFloat(tokens, fallback),
                text -> parser().parseFloat(text, fallback));
    }

    public float getNonNegativeFloat(@NotNull String name, float fallback) {
        float value = getFloat(name, fallback);
        if (Float.isFinite(value) && value < 0) return fallback;
        return value;
    }

    public int getInt(@NotNull String key, int fallback) {
        return parser().parseInt(getValue(key), fallback);
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
                .filter(n -> n != null)
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
