/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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

import static com.github.weisj.jsvg.util.AttributeUtil.toNonnullArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.time.Duration;
import com.github.weisj.jsvg.animation.time.TimeUnit;
import com.github.weisj.jsvg.attributes.HasMatchName;
import com.github.weisj.jsvg.attributes.SuffixUnit;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Angle;
import com.github.weisj.jsvg.geometry.size.AngleUnit;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.PaintParser;


public final class AttributeParser {

    private static final Logger LOGGER = Logger.getLogger(AttributeParser.class.getName());
    private final @NotNull PaintParser paintParser;

    public AttributeParser(@NotNull PaintParser paintParser) {
        this.paintParser = paintParser;
    }

    @Contract("_,!null,_ -> !null")
    public @Nullable Length parseLength(@Nullable String value, @Nullable Length fallback,
            @NotNull PercentageDimension dimension) {
        return parseSuffixUnit(value, Unit.RAW, fallback, u -> {
            if (u == Unit.PERCENTAGE) {
                switch (dimension) {
                    case WIDTH:
                        return Unit.PERCENTAGE_WIDTH;
                    case HEIGHT:
                        return Unit.PERCENTAGE_HEIGHT;
                    case LENGTH:
                        return Unit.PERCENTAGE_LENGTH;
                    case CUSTOM:
                        return Unit.PERCENTAGE;
                    case NONE:
                        return null;
                }
            }
            return u;
        });
    }

    @Contract("_,!null -> !null")
    public @Nullable Duration parseTimeOffsetValue(@Nullable String value, @Nullable Duration fallback) {
        // Parse clock-value
        // https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#clock-value

        // Parse Timecount-value
        return parseSuffixUnit(value, TimeUnit.Raw, fallback, u -> u);
    }

    @Contract("_,!null -> !null")
    public @Nullable Duration parseDuration(@Nullable String value, @Nullable Duration fallback) {
        if (value == null) return fallback;
        if ("indefinite".equals(value)) return Duration.INDEFINITE;

        // Parse clock-value
        // https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#clock-value

        // Parse Timecount-value
        Duration timeCount = parseSuffixUnit(value, TimeUnit.Raw, null, u -> u);
        if (timeCount != null) return timeCount;

        return fallback;
    }

    @Contract("_,!null -> !null")
    public @Nullable Percentage parsePercentage(@Nullable String value, @Nullable Percentage fallback) {
        return parsePercentage(value, fallback, 0, 1);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable Percentage parsePercentage(@Nullable String value, @Nullable Percentage fallback, float min,
            float max) {
        if (value == null) return fallback;
        try {
            float parsed;
            if (value.endsWith("%")) {
                parsed = Float.parseFloat(value.substring(0, value.length() - 1)) / 100f;
            } else {
                parsed = Float.parseFloat(value);
            }
            return new Percentage(Math.max(min, Math.min(max, parsed)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Contract("_,_,!null,_ -> !null")
    private <U, V> @Nullable V parseSuffixUnit(@Nullable String value, @NotNull SuffixUnit<U, V> defaultUnit,
            @Nullable V fallback,
            @NotNull UnaryOperator<@Nullable SuffixUnit<U, V>> unitMapper) {
        if (value == null) return fallback;
        SuffixUnit<U, V> unit = defaultUnit;
        String lower = value.toLowerCase(Locale.ENGLISH);
        int i = lower.length() - 1;
        for (; i >= 0; i--) {
            if (Character.isDigit(lower.charAt(i))) {
                break;
            }
        }
        String suffix = lower.substring(i + 1);
        for (SuffixUnit<U, V> u : defaultUnit.units()) {
            if (suffix.equals(u.suffix())) {
                unit = u;
                break;
            }
        }
        unit = unitMapper.apply(unit);
        if (unit == null) return fallback;
        String str = lower.substring(0, lower.length() - unit.suffix().length());
        try {
            return unit.valueOf(Float.parseFloat(str));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public int parseInt(@Nullable String value, int fallback) {
        return ParserUtil.parseInt(value, fallback);
    }


    @Contract("_,!null -> !null")
    public @Nullable Length parseNumber(@Nullable String value, @Nullable Length fallback) {
        return ParserUtil.parseNumber(value, fallback);
    }

    public float parseFloat(@Nullable String value, float fallback) {
        return ParserUtil.parseFloat(value, fallback);
    }


    public @NotNull Angle parseAngle(@Nullable String value, @NotNull Angle fallback) {
        if (value == null) return fallback;
        AngleUnit unit = AngleUnit.Raw;
        String lower = value.toLowerCase(Locale.ENGLISH);
        for (AngleUnit u : AngleUnit.units()) {
            if (lower.endsWith(u.suffix())) {
                unit = u;
                break;
            }
        }
        String str = lower.substring(0, lower.length() - unit.suffix().length());
        try {
            return new Angle(unit, Float.parseFloat(str));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Contract("_,!null,_ -> !null")
    public @NotNull Length @Nullable [] parseLengthList(@Nullable String value, @NotNull Length @Nullable [] fallback,
            @NotNull PercentageDimension dimension) {
        if (value != null && value.equalsIgnoreCase("none")) return new Length[0];
        String[] values = parseStringList(value, SeparatorMode.COMMA_AND_WHITESPACE, null);
        if (values == null) return fallback;
        Length[] ret = new Length[values.length];
        for (int i = 0; i < ret.length; i++) {
            Length length = parseLength(values[i], null, dimension);
            if (length == null) return fallback;
            ret[i] = length;
        }
        return ret;
    }

    public float @NotNull [] parseFloatList(@Nullable String value) {
        return ParserUtil.parseFloatList(value);
    }

    public double @NotNull [] parseDoubleList(@Nullable String value) {
        return ParserUtil.parseDoubleList(value);
    }

    public @NotNull String @NotNull [] parseStringList(@Nullable String value, SeparatorMode separatorMode) {
        return ParserUtil.parseStringList(value, separatorMode);
    }

    @Contract("_,_,!null -> !null")
    public @NotNull String @Nullable [] parseStringList(@Nullable String value, SeparatorMode separatorMode,
            @NotNull String @Nullable [] fallback) {
        return ParserUtil.parseStringList(value, separatorMode, fallback);
    }

    public @Nullable SVGPaint parsePaint(@Nullable String value, @NotNull AttributeNode attributeNode) {
        return paintParser.parsePaint(value);
    }

    public <E extends Enum<E>> @NotNull E parseEnum(@Nullable String value, @NotNull E fallback) {
        E e = parseEnum(value, fallback.getDeclaringClass());
        if (e == null) return fallback;
        return e;
    }

    public <E extends Enum<E>> @Nullable E parseEnum(@Nullable String value, @NotNull Class<E> enumType) {
        if (value == null) return null;
        for (E enumConstant : enumType.getEnumConstants()) {
            String name = enumConstant instanceof HasMatchName
                    ? ((HasMatchName) enumConstant).matchName()
                    : enumConstant.name();
            if (name.equalsIgnoreCase(value)) return enumConstant;
        }
        return null;
    }

    public @Nullable String parseUrl(@Nullable String value) {
        return ParserUtil.parseUrl(value);
    }

    private static final Pattern TRANSFORM_PATTERN = Pattern.compile("\\w+\\([^)]*\\)");

    public @Nullable List<@NotNull TransformPart> parseTransform(@Nullable String value) {
        if (value == null) return null;
        if ("none".equals(value)) return null;
        final Matcher transformMatcher = TRANSFORM_PATTERN.matcher(value);
        List<TransformPart> parts = new ArrayList<>();
        while (transformMatcher.find()) {
            String group = transformMatcher.group();
            TransformPart part = parseSingleTransformPart(group);
            if (part == null) {
                LOGGER.warning(
                        () -> String.format("Illegal transform definition '%s' encountered error while parsing '%s'",
                                value, group));
                return null;
            }
            parts.add(part);
        }
        return parts;
    }

    private @Nullable TransformPart parseSingleTransformPart(@NotNull String value) {
        int first = value.indexOf('(');
        int last = value.lastIndexOf(')');
        String command = value.substring(0, value.indexOf('(')).toLowerCase(Locale.ENGLISH);
        TransformPart.TransformType type = parseEnum(command, TransformPart.TransformType.class);
        if (type == null) return null;

        return parseTransformPart(type, value.substring(first + 1, last));
    }

    public @Nullable TransformPart parseTransformPart(TransformPart.TransformType type, @NotNull String value) {
        String[] values = parseStringList(value, SeparatorMode.COMMA_AND_WHITESPACE);
        Length[] lengths = parseTransformLengths(type, values);
        if (lengths == null) return null;
        return new TransformPart(type, lengths);
    }

    private Length @Nullable [] parseTransformLengths(TransformPart.@NotNull TransformType type,
            @NotNull String @NotNull [] values) {
        Length[] lengths;
        switch (type) {
            case MATRIX:
                if (values.length == 4) {
                    lengths = toNonnullArray(
                            parseNumber(values[0], null),
                            parseNumber(values[1], null),
                            parseNumber(values[2], null),
                            parseNumber(values[3], null),
                            Length.ZERO,
                            Length.ZERO);
                } else if (values.length == 6) {
                    lengths = toNonnullArray(
                            parseNumber(values[0], null),
                            parseNumber(values[1], null),
                            parseNumber(values[2], null),
                            parseNumber(values[3], null),
                            parseNumber(values[4], null),
                            parseNumber(values[5], null));
                } else {
                    lengths = null;
                }
                break;
            case TRANSLATE:
                if (values.length == 1) {
                    lengths = toNonnullArray(
                            parseLength(values[0], null, PercentageDimension.WIDTH),
                            Length.ZERO);
                } else {
                    lengths = toNonnullArray(
                            parseLength(values[0], null, PercentageDimension.WIDTH),
                            parseLength(values[1], null, PercentageDimension.HEIGHT));
                }
                break;
            case TRANSLATE_X:
                lengths = toNonnullArray(
                        parseLength(values[0], null, PercentageDimension.WIDTH));
                break;
            case TRANSLATE_Y:
                lengths = toNonnullArray(
                        parseLength(values[0], null, PercentageDimension.HEIGHT));
                break;
            case ROTATE:
                if (values.length > 2) {
                    lengths = toNonnullArray(
                            parseNumber(values[0], null),
                            parseLength(values[1], null, PercentageDimension.WIDTH),
                            parseLength(values[2], null, PercentageDimension.HEIGHT));
                } else {
                    lengths = toNonnullArray(
                            parseNumber(values[0], null));
                }
                break;
            case SCALE:
            case SKEW:
                if (values.length == 1) {
                    lengths = toNonnullArray(
                            parseNumber(values[0], null));
                } else {
                    lengths = toNonnullArray(
                            parseNumber(values[0], null),
                            parseNumber(values[1], null));
                }
                break;
            case SCALE_X:
            case SCALE_Y:
            case SKEW_X:
            case SKEW_Y:
                lengths = toNonnullArray(
                        parseLength(values[0], null, PercentageDimension.NONE));
                break;
            default:
                lengths = null;
        }
        return lengths;
    }

    public @NotNull PaintParser paintParser() {
        return paintParser;
    }

}
