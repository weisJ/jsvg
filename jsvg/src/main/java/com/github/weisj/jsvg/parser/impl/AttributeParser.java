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

import static com.github.weisj.jsvg.util.AttributeUtil.toNonnullArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

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
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.NumberListSplitter;
import com.github.weisj.jsvg.parser.PaintParser;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;

public final class AttributeParser {

    private static final Logger LOGGER = LogFactory.createLogger(AttributeParser.class);
    private final @NotNull PaintParser paintParser;

    public AttributeParser(@NotNull PaintParser paintParser) {
        this.paintParser = paintParser;
    }

    @Contract("_,!null,_ -> !null")
    public @Nullable Length parseLength(@Nullable String value, @Nullable Length fallback,
            @NotNull PercentageDimension dimension) {
        return parseSuffixUnit(value, Unit.RAW, fallback, u -> {
            if (u == Unit.PERCENTAGE) {
                return dimension.unit();
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
        String[] values = parseStringList(value, NumberListSplitter.INSTANCE, null);
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

    public @NotNull String @NotNull [] parseStringList(@Nullable String value, @NotNull ListSplitter listSplitter) {
        return ParserUtil.parseStringList(value, listSplitter);
    }

    @Contract("_,_,!null -> !null")
    public @NotNull String @Nullable [] parseStringList(@Nullable String value, @NotNull ListSplitter listSplitter,
            @NotNull String @Nullable [] fallback) {
        return ParserUtil.parseStringList(value, listSplitter, fallback);
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

    private static void warnIllegalTransform(@NotNull String value, @NotNull String input) {
        LOGGER.log(Level.WARNING,
                () -> String.format("Illegal transform definition '%s' encountered error while parsing '%s'",
                        value, input));
    }

    private static final class RawTransformFunction {
        final int endIndex;
        final @NotNull String name;
        final @NotNull String args;

        RawTransformFunction(int endIndex, @NotNull String name, @NotNull String args) {
            this.endIndex = endIndex;
            this.name = name;
            this.args = args;
        }
    }

    /**
     * Parses the next transform function starting at {@code start} in {@code value}, skipping
     * leading whitespace/commas.
     *
     * @return a {@link RawTransformFunction} whose {@code endIndex} points to the character
     *         after the closing {@code ')'}, or {@code null} if parsing fails.
     */
    private static @Nullable AttributeParser.RawTransformFunction parseNextTransformFunction(@NotNull String value,
            int start) {
        int i = start;
        int len = value.length();
        // Skip whitespace and commas
        while (i < len && (Character.isWhitespace(value.charAt(i)) || value.charAt(i) == ',')) {
            i++;
        }
        if (i >= len) return null;
        // Read function name
        int nameStart = i;
        while (i < len && (Character.isLetterOrDigit(value.charAt(i))
                || value.charAt(i) == '-' || value.charAt(i) == '_')) {
            i++;
        }
        if (i >= len || value.charAt(i) != '(') return null;
        String name = value.substring(nameStart, i);
        i++; // skip '('
        int argStart = i;
        while (i < len && value.charAt(i) != ')')
            i++;
        if (i >= len) return null;
        String args = value.substring(argStart, i);
        i++; // skip ')'
        return new RawTransformFunction(i, name, args);
    }

    public @Nullable List<@NotNull TransformPart> parseTransform(@Nullable String value) {
        if (value == null) return null;
        if ("none".equals(value)) return null;
        List<TransformPart> parts = new ArrayList<>();
        int i = 0;
        while (i < value.length()) {
            // Skip remaining whitespace/commas (handles end-of-string)
            int skipped = i;
            while (skipped < value.length()
                    && (Character.isWhitespace(value.charAt(skipped)) || value.charAt(skipped) == ','))
                skipped++;
            if (skipped >= value.length()) break;

            RawTransformFunction parsed = parseNextTransformFunction(value, i);
            if (parsed == null) {
                warnIllegalTransform(value, value.substring(i));
                return null;
            }
            TransformPart part = parseSingleTransformPart(parsed);
            if (part == null) {
                warnIllegalTransform(value, parsed.args);
                return null;
            }
            parts.add(part);
            i = parsed.endIndex;
        }
        return parts;
    }

    private @Nullable TransformPart parseSingleTransformPart(@NotNull RawTransformFunction transformFunction) {
        String command = transformFunction.name.toLowerCase(Locale.ENGLISH);
        TransformPart.TransformType type = parseEnum(command, TransformPart.TransformType.class);
        if (type == null) return null;

        return parseTransformPart(type, transformFunction.args);
    }

    public @Nullable TransformPart parseTransformPart(TransformPart.TransformType type, @NotNull String value) {
        String[] values = parseStringList(value, NumberListSplitter.INSTANCE);
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

    // Token-native parsing (CSS Syntax §5): consume already-lexed component values directly instead of
    // serializing and re-scanning. A single value-bearing token carries its number/unit/keyword;
    // irregular or multi-token inputs fall back to the string parsers above.

    @Contract("_,!null,_ -> !null")
    public @Nullable Length parseLength(@NotNull List<@NotNull ComponentValue> tokens, @Nullable Length fallback,
            @NotNull PercentageDimension dimension) {
        Length length = lengthFromToken(singleToken(tokens), dimension);
        return length != null ? length : fallback;
    }

    /** {@code <number>}/{@code <length>}/{@code <percentage>} from one token, or {@code null} if it is neither. */
    private @Nullable Length lengthFromToken(@Nullable ComponentValue token, @NotNull PercentageDimension dimension) {
        if (token instanceof Token.Number) {
            return Unit.RAW.valueOf(((Token.Number) token).value());
        }
        if (token instanceof Token.Dimension) {
            Token.Dimension dimensionToken = (Token.Dimension) token;
            Unit unit = Unit.fromNonPercentageSuffix(dimensionToken.unit().toLowerCase(Locale.ENGLISH));
            if (unit == null) return null;
            return unit.valueOf(dimensionToken.value());
        }
        if (token instanceof Token.Percentage) {
            Unit unit = dimension.unit();
            if (unit == null) return null;
            return unit.valueOf(((Token.Percentage) token).value());
        }
        return null;
    }

    public float parseFloat(@NotNull List<@NotNull ComponentValue> tokens, float fallback) {
        ComponentValue token = singleToken(tokens);
        if (token instanceof Token.Number) return ((Token.Number) token).value();
        return fallback;
    }

    public @NotNull Angle parseAngle(@NotNull List<@NotNull ComponentValue> tokens, @NotNull Angle fallback) {
        Angle angle = angleFromToken(singleToken(tokens));
        return angle != null ? angle : fallback;
    }

    /** {@code <angle>} from one token; bare {@code <number>} = degrees. */
    private static @Nullable Angle angleFromToken(@Nullable ComponentValue token) {
        if (token instanceof Token.Number) {
            return new Angle(AngleUnit.Raw, ((Token.Number) token).value());
        }
        if (token instanceof Token.Dimension) {
            Token.Dimension dimension = (Token.Dimension) token;
            String suffix = dimension.unit().toLowerCase(Locale.ENGLISH);
            for (AngleUnit unit : AngleUnit.units()) {
                if (unit != AngleUnit.Raw && unit.suffix().equals(suffix)) {
                    return new Angle(unit, dimension.value());
                }
            }
        }
        return null;
    }

    @Contract("_,!null,_ -> !null")
    public @NotNull Length @Nullable [] parseLengthList(@NotNull List<@NotNull ComponentValue> tokens,
            @NotNull Length @Nullable [] fallback, @NotNull PercentageDimension dimension) {
        ComponentValue single = singleToken(tokens);
        if (single != null && single.isOneOfKeywords("none")) {
            return new Length[0];
        }
        List<Length> result = new ArrayList<>();
        for (ComponentValue token : tokens) {
            if (isSeparator(token)) continue;
            Length length = lengthFromToken(token, dimension);
            if (length == null) return fallback;
            result.add(length);
        }
        return result.toArray(new Length[0]);
    }

    /** Splits a token stream into groups on the separators of {@code separatorMode}; each group stays tokens. */
    public @NotNull List<@NotNull List<@NotNull ComponentValue>> splitList(
            @NotNull List<@NotNull ComponentValue> tokens, @NotNull SeparatorMode separatorMode) {
        boolean splitWhitespace = separatorMode.splitOnWhitespace();
        char separator = separatorMode.separatorChar();
        boolean commaSplits = separator == ',';
        boolean semicolonSplits = separator == ';';
        List<List<ComponentValue>> groups = new ArrayList<>();
        List<ComponentValue> current = new ArrayList<>();
        for (ComponentValue token : tokens) {
            boolean separates = (token == Token.Static.WHITESPACE && splitWhitespace)
                    || (token == Token.Static.COMMA && commaSplits)
                    || (token == Token.Static.SEMICOLON && semicolonSplits);
            if (separates) {
                if (!current.isEmpty()) {
                    groups.add(trimWhitespace(current));
                    current = new ArrayList<>();
                }
                continue;
            }
            // drop leading whitespace; keep interior
            if (token == Token.Static.WHITESPACE && current.isEmpty()) continue;
            current.add(token);
        }
        if (!current.isEmpty()) groups.add(trimWhitespace(current));
        return groups;
    }

    private static @NotNull List<@NotNull ComponentValue> trimWhitespace(
            @NotNull List<@NotNull ComponentValue> group) {
        int end = group.size();
        while (end > 0 && group.get(end - 1) == Token.Static.WHITESPACE) {
            end--;
        }
        return end == group.size() ? group : group.subList(0, end);
    }

    public @Nullable List<@NotNull TransformPart> parseTransform(@NotNull List<@NotNull ComponentValue> tokens) {
        ComponentValue singleToken = singleToken(tokens);
        if (singleToken != null && singleToken.isOneOfKeywords("none")) return null;

        List<TransformPart> parts = new ArrayList<>();
        for (ComponentValue token : tokens) {
            if (token == Token.Static.WHITESPACE) continue;
            if (!(token instanceof ComponentValue.FunctionBlock)) return null;
            ComponentValue.FunctionBlock function = (ComponentValue.FunctionBlock) token;
            TransformPart.TransformType type =
                    parseEnum(function.name().toLowerCase(Locale.ENGLISH), TransformPart.TransformType.class);
            if (type == null) return null;
            List<ComponentValue> args = new ArrayList<>();
            for (ComponentValue arg : function.value()) {
                if (!isSeparator(arg)) args.add(arg);
            }
            Length[] lengths = parseTransformLengths(type, args);
            if (lengths == null) return null;
            parts.add(new TransformPart(type, lengths));
        }
        return parts.isEmpty() ? null : parts;
    }

    private Length @Nullable [] parseTransformLengths(TransformPart.@NotNull TransformType type,
            @NotNull List<@NotNull ComponentValue> args) {
        switch (type) {
            case MATRIX:
                if (args.size() == 4) {
                    return toNonnullArray(number(args, 0), number(args, 1), number(args, 2), number(args, 3),
                            Length.ZERO, Length.ZERO);
                } else if (args.size() == 6) {
                    return toNonnullArray(number(args, 0), number(args, 1), number(args, 2),
                            number(args, 3), number(args, 4), number(args, 5));
                }
                return null;
            case TRANSLATE:
                if (args.size() == 1) {
                    return toNonnullArray(
                            0 < args.size() ? lengthFromToken(args.get(0), PercentageDimension.WIDTH) : null,
                            Length.ZERO);
                }
                return toNonnullArray(
                        0 < args.size() ? lengthFromToken(args.get(0), PercentageDimension.WIDTH) : null,
                        1 < args.size() ? lengthFromToken(args.get(1), PercentageDimension.HEIGHT) : null);
            case TRANSLATE_X:
                return toNonnullArray(
                        0 < args.size() ? lengthFromToken(args.get(0), PercentageDimension.WIDTH) : null);
            case TRANSLATE_Y:
                return toNonnullArray(
                        0 < args.size() ? lengthFromToken(args.get(0), PercentageDimension.HEIGHT) : null);
            case ROTATE:
                if (args.size() > 2) {
                    return toNonnullArray(number(args, 0),
                            1 < args.size() ? lengthFromToken(args.get(1), PercentageDimension.WIDTH) : null,
                            2 < args.size() ? lengthFromToken(args.get(2), PercentageDimension.HEIGHT) : null);
                }
                return toNonnullArray(number(args, 0));
            case SCALE:
            case SKEW:
                if (args.size() == 1) return toNonnullArray(number(args, 0));
                return toNonnullArray(number(args, 0), number(args, 1));
            case SCALE_X:
            case SCALE_Y:
            case SKEW_X:
            case SKEW_Y:
                return toNonnullArray(
                        0 < args.size() ? lengthFromToken(args.get(0), PercentageDimension.NONE) : null);
            default:
                return null;
        }
    }

    private @Nullable Length number(@NotNull List<@NotNull ComponentValue> args, int index) {
        if (index >= args.size()) return null;
        ComponentValue token = args.get(index);
        return token instanceof Token.Number ? Unit.RAW.valueOf(((Token.Number) token).value()) : null;
    }

    private static boolean isSeparator(@NotNull ComponentValue token) {
        return token == Token.Static.WHITESPACE || token == Token.Static.COMMA;
    }

    @Contract("_,!null -> !null")
    public @Nullable Percentage parsePercentage(@NotNull List<@NotNull ComponentValue> tokens,
            @Nullable Percentage fallback) {
        return parsePercentage(tokens, fallback, 0, 1);
    }

    @Contract("_,!null,_,_ -> !null")
    public @Nullable Percentage parsePercentage(@NotNull List<@NotNull ComponentValue> tokens,
            @Nullable Percentage fallback, float min, float max) {
        ComponentValue token = singleToken(tokens);
        if (token instanceof Token.Percentage) {
            return new Percentage(clamp(min, max, ((Token.Percentage) token).value() / 100f));
        }
        if (token instanceof Token.Number) {
            return new Percentage(clamp(min, max, ((Token.Number) token).value()));
        }
        return fallback;
    }

    public <E extends Enum<E>> @NotNull E parseEnum(@NotNull List<@NotNull ComponentValue> tokens,
            @NotNull E fallback) {
        E e = parseEnum(tokens, fallback.getDeclaringClass());
        return e == null ? fallback : e;
    }

    public <E extends Enum<E>> @Nullable E parseEnum(@NotNull List<@NotNull ComponentValue> tokens,
            @NotNull Class<E> enumType) {
        ComponentValue token = singleToken(tokens);
        if (token instanceof Token.Ident) return parseEnum(((Token.Ident) token).name(), enumType);
        return null;
    }

    public @Nullable SVGPaint parsePaint(@NotNull List<@NotNull ComponentValue> tokens) {
        return paintParser.parsePaint(tokens);
    }

    /** The sole non-whitespace token, or {@code null} if there are zero or several. */
    public static @Nullable ComponentValue singleToken(@NotNull List<? extends @NotNull ComponentValue> tokens) {
        ComponentValue found = null;
        for (ComponentValue token : tokens) {
            if (token == Token.Static.WHITESPACE) continue;
            if (found != null) return null;
            found = token;
        }
        return found;
    }

    public static @Nullable String identOf(@NotNull List<? extends @NotNull ComponentValue> tokens) {
        ComponentValue token = singleToken(tokens);
        return token instanceof Token.Ident ? ((Token.Ident) token).name() : null;
    }

    private static float clamp(float min, float max, float value) {
        return Math.max(min, Math.min(max, value));
    }

    public @NotNull PaintParser paintParser() {
        return paintParser;
    }

}
