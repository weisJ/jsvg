/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.AwtSVGPaint;
import com.github.weisj.jsvg.paint.impl.ColorPropertyPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.FloatSize;

/** Parses color and paint attributes, including their inheritance and animation behavior. */
public final class ColorPaintHelper {

    private static final MeasureContext DUMMY_MEASURE_CONTEXT =
            MeasureContext.createInitial(new FloatSize(0, 0), 0, 0, new AnimationState(0, 0));

    private ColorPaintHelper() {}

    @Contract("_,_,!null -> !null")
    static @Nullable Color getColor(@NotNull AttributeNode attributeNode, @NotNull String key,
            @Nullable Color fallback) {
        ColorValue value = resolveColorValue(attributeNode, key, fallback, Animatable.NO);
        return value != null ? value.get(DUMMY_MEASURE_CONTEXT) : fallback;
    }

    static @NotNull ColorValue getColorValue(@NotNull AttributeNode attributeNode, @NotNull String key,
            @NotNull Color fallback, @NotNull Animatable animatable) {
        ColorValue value = resolveColorValue(attributeNode, key, fallback, animatable);
        return value != null ? value : new RGBColor(fallback);
    }

    @SuppressWarnings("java:S2589") // Sonarcloud does not understand that map lookups can return null
    static @NotNull SVGPaint getColorProperty(@NotNull AttributeNode attributeNode, @NotNull Animatable animatable) {
        AttributeValue attribute = attributeNode.attributes().get(Attributes.Name.COLOR);
        Color color = null;
        if (attribute != null
                && !isKeyword(attribute, Attributes.Keyword.INHERIT)
                && !isKeyword(attribute, Attributes.Keyword.CURRENT_COLOR)) {
            color = parseColor(attributeNode, attribute);
        }

        ColorValue initial = color != null ? new RGBColor(color) : RGBColor.INHERITED;
        SVGPaint specified = color != null ? new AwtSVGPaint(color) : null;
        if (animatable == Animatable.YES) {
            AnimatedColor animatedColor = AnimationHelper.animatedColor(attributeNode, Attributes.Name.COLOR, initial);
            if (animatedColor != null) specified = animatedColor;
        }
        return new ColorPropertyPaint(specified);
    }

    static @Nullable SVGPaint getPaint(@NotNull AttributeNode attributeNode, @NotNull String key,
            @Nullable SVGPaint fallback, @NotNull Inherited inherited, @NotNull Animatable animatable) {
        // Paint attributes are all CSS properties, so a set value is token-valued.
        List<ComponentValue> tokens = attributeNode.getTokens(key);
        SVGPaint paint = tokens != null ? parsePaint(attributeNode, tokens, animatable) : null;
        SVGPaint value = paint != null ? paint : fallback;
        if (animatable == Animatable.YES) {
            SVGPaint initial = value;
            if (initial == null) {
                initial = inherited == Inherited.YES
                        ? PredefinedPaints.INHERITED
                        : PredefinedPaints.DEFAULT_PAINT;
            }
            AnimatedPaint animatedPaint = AnimationHelper.animatedPaint(attributeNode, key, initial);
            if (animatedPaint != null) return animatedPaint;
        }
        return value;
    }

    static @NotNull SVGPaint currentColorPaint(@NotNull AttributeNode attributeNode) {
        return computedColorValue(attributeNode, Animatable.YES).asSVGPaint();
    }

    private static @Nullable ColorValue resolveColorValue(@NotNull AttributeNode attributeNode,
            @NotNull String key, @Nullable Color fallback, @NotNull Animatable animatable) {
        ComputedColor computed = computedColorProperty(attributeNode, key, fallback, animatable);
        return resolveComputedColor(attributeNode, computed, animatable);
    }

    @SuppressWarnings("java:S2583") // Sonarcloud does not understand that map lookups can return null
    @Contract("_,_,!null,_ -> !null")
    private static @Nullable ComputedColor computedColorProperty(@NotNull AttributeNode attributeNode,
            @NotNull String key, @Nullable Color fallback, @NotNull Animatable animatable) {
        AttributeValue attribute = attributeNode.attributes().get(key);
        ComputedColor computed;
        if (isKeyword(attribute, Attributes.Keyword.INHERIT)) {
            AttributeNode parent = attributeNode.parentAttributeNode();
            // The property is not inherited by default. If the immediate parent has no declaration,
            // its computed value is the property's initial value; do not search past that parent.
            computed = parent != null
                    ? computedColorProperty(parent, key, fallback, animatable)
                    : ComputedColor.of(fallback);
        } else if (isKeyword(attribute, Attributes.Keyword.CURRENT_COLOR)) {
            // Preserve the keyword while following `inherit`: it resolves against the color of
            // the element which receives it, not the element which declared it.
            computed = ComputedColor.CURRENT_COLOR;
        } else {
            Color color = attribute != null ? parseColor(attributeNode, attribute) : null;
            computed = ComputedColor.of(color != null ? color : fallback);
        }

        if (animatable == Animatable.YES) {
            ColorValue initial = resolveComputedColor(attributeNode, computed, animatable);
            if (initial != null) {
                AnimatedColor animatedColor = AnimationHelper.animatedColor(attributeNode, key, initial);
                if (animatedColor != null) return ComputedColor.of(animatedColor);
            }
        }
        return computed;
    }

    private static @Nullable ColorValue resolveComputedColor(@NotNull AttributeNode attributeNode,
            @Nullable ComputedColor computed, @NotNull Animatable animatable) {
        if (computed == null) return null;
        return computed == ComputedColor.CURRENT_COLOR
                ? computedColorValue(attributeNode, animatable)
                : computed.value;
    }

    private static @NotNull ColorValue computedColorValue(@NotNull AttributeNode attributeNode,
            @NotNull Animatable animatable) {

        AttributeValue attribute = attributeNode.attributes().get(Attributes.Name.COLOR);

        ColorValue value = RGBColor.DEFAULT;
        // On the color property itself currentColor has the same used-value behavior as inherit.
        if (attribute == null
                || isKeyword(attribute, Attributes.Keyword.INHERIT)
                || isKeyword(attribute, Attributes.Keyword.CURRENT_COLOR)) {
            AttributeNode parent = attributeNode.parentAttributeNode();
            value = parent != null
                    ? computedColorValue(parent, animatable)
                    : RGBColor.DEFAULT;
        } else {
            Color localColor = parseColor(attributeNode, attribute);
            if (localColor != null) value = new RGBColor(localColor);
        }

        if (animatable == Animatable.YES) {
            AnimatedColor animatedColor = AnimationHelper.animatedColor(
                    attributeNode, Attributes.Name.COLOR, value);
            if (animatedColor != null) return animatedColor;
        }
        return value;
    }

    private static boolean isKeyword(@Nullable AttributeValue value, @NotNull String keyword) {
        if (value instanceof AttributeValue.PlainString) {
            return keyword.equalsIgnoreCase(((AttributeValue.PlainString) value).string().trim());
        }
        if (value instanceof AttributeValue.Parsed) {
            ComponentValue token = AttributeParser.singleToken(((AttributeValue.Parsed) value).tokens());
            return token != null && token.isOneOfKeywords(keyword);
        }
        return false;
    }

    private static @Nullable Color parseColor(@NotNull AttributeNode attributeNode,
            @NotNull AttributeValue value) {
        if (value instanceof AttributeValue.Parsed) {
            return attributeNode.parser().parseColor(((AttributeValue.Parsed) value).tokens());
        }
        return attributeNode.parser().parseColor(((AttributeValue.PlainString) value).string());
    }

    private static @Nullable SVGPaint parsePaint(@NotNull AttributeNode attributeNode,
            @NotNull List<@NotNull ComponentValue> tokens, @NotNull Animatable animatable) {
        for (int i = 0; i < tokens.size(); i++) {
            String url = AttributeNode.urlOf(tokens.get(i));
            if (url != null) {
                return parseReferencedPaint(attributeNode, tokens, i, url, animatable);
            }
        }
        return parsePaintValue(attributeNode, tokens, animatable);
    }

    @ApiStatus.Internal
    public static @Nullable SVGPaint parsePaint(@NotNull AttributeNode attributeNode, @Nullable String value) {
        if (value == null) return null;
        List<ComponentValue> tokens = attributeNode.parser().cssParser().parseCssAttribute(value);
        return parsePaint(attributeNode, tokens, Animatable.YES);
    }

    private static @Nullable SVGPaint parseReferencedPaint(@NotNull AttributeNode attributeNode,
            @NotNull List<@NotNull ComponentValue> tokens, int urlIndex, @NotNull String url,
            @NotNull Animatable animatable) {
        SVGPaint paint = attributeNode.getElementByHref(
                SVGPaint.class, url, AttributeNode.ElementRelation.PAINT_SERVER);
        if (paint != null) return paint;

        List<ComponentValue> fallback = tokens.subList(urlIndex + 1, tokens.size());
        return hasPaintFallback(fallback)
                ? parsePaintValue(attributeNode, fallback, animatable)
                : PredefinedPaints.NONE;
    }

    private static @Nullable SVGPaint parsePaintValue(@NotNull AttributeNode attributeNode,
            @NotNull List<@NotNull ComponentValue> tokens, @NotNull Animatable animatable) {
        ComponentValue token = AttributeParser.singleToken(tokens);
        return token != null && token.isOneOfKeywords(Attributes.Keyword.CURRENT_COLOR)
                ? getColorProperty(attributeNode, animatable)
                : attributeNode.parser().parsePaint(tokens);
    }

    private static boolean hasPaintFallback(@NotNull List<@NotNull ComponentValue> fallback) {
        for (ComponentValue fallbackToken : fallback) {
            if (fallbackToken != Token.Static.WHITESPACE) return true;
        }
        return false;
    }

    private static final class ComputedColor {
        private static final ComputedColor CURRENT_COLOR = new ComputedColor(null);

        private final @Nullable ColorValue value;

        private ComputedColor(@Nullable ColorValue value) {
            this.value = value;
        }

        private static @Nullable ComputedColor of(@Nullable Color color) {
            return color != null ? of(new RGBColor(color)) : null;
        }

        private static @NotNull ComputedColor of(@NotNull ColorValue value) {
            return new ComputedColor(value);
        }
    }
}
