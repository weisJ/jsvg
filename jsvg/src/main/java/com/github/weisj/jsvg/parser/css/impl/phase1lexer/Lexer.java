/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.css.impl.phase1lexer;

import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;
import com.github.weisj.jsvg.parser.css.impl.ParserException;
import com.github.weisj.jsvg.parser.css.impl.phase0preprocessor.PreProcessor;

/**
 * Stream of characters -> Stream of {@link Token}
 * <p>
 * CSS tokenizer following <a href="https://www.w3.org/TR/css-syntax-3/#tokenization">CSS Syntax Module Level 3, §4.3</a>.
 * <p>
 * Input preprocessing (§3.3) is delegated to {@link PreProcessor}, which streams the raw segments
 * without copying. The lexer keeps a small lookahead window over the preprocessed stream.
 * Comments (§4.3.2) are silently dropped between tokens; whitespace is emitted as
 * {@link TokenType#WHITESPACE} so the parser can decide where it is significant.
 */
public final class Lexer {

    /** §4.3 algorithm looks at most 4 code points ahead. */
    private static final int LOOKAHEAD = 4;

    private final @NotNull Lookahead look;

    public Lexer(@NotNull LexerInput input) {
        this.look = new Lookahead(new PreProcessor(input), LOOKAHEAD);
    }

    /** §4.3.1 Consume a token. */
    public @NotNull Token nextToken() {
        consumeComments();

        if (look.isEof()) return Token.Static.EOF;
        int c = look.peek(0);

        if (isWhitespace(c)) {
            look.consumeWhile(Lexer::isWhitespace);
            return Token.Static.WHITESPACE;
        }

        switch (c) {
            case '"':
            case '\'':
                return consumeStringToken(c);
            case '#':
                return consumeHashOrDelim();
            case '(':
                look.advance();
                return Token.Static.LEFT_PAREN;
            case ')':
                look.advance();
                return Token.Static.RIGHT_PAREN;
            case '+':
                if (wouldStartNumber(c, look.peek(1), look.peek(2))) return consumeNumericToken();
                look.advance();
                return new Token.Delim('+');
            case ',':
                look.advance();
                return Token.Static.COMMA;
            case '-':
                return consumeHyphenStart();
            case '.':
                if (wouldStartNumber(c, look.peek(1), look.peek(2))) return consumeNumericToken();
                look.advance();
                return new Token.Delim('.');
            case ':':
                look.advance();
                return Token.Static.COLON;
            case ';':
                look.advance();
                return Token.Static.SEMICOLON;
            case '<':
                if (look.peek(1) == '!' && look.peek(2) == '-' && look.peek(3) == '-') {
                    look.advance();
                    look.advance();
                    look.advance();
                    look.advance();
                    return Token.Static.CDO;
                }
                look.advance();
                return new Token.Delim('<');
            case '@':
                if (wouldStartIdentSequence(look.peek(1), look.peek(2), look.peek(3))) {
                    look.advance();
                    return new Token.AtKeyword(consumeIdentSequence());
                }
                look.advance();
                return new Token.Delim('@');
            case '[':
                look.advance();
                return Token.Static.LEFT_BRACKET;
            case '\\':
                if (isValidEscapeStart(c, look.peek(1))) return consumeIdentLikeToken();
                look.advance();
                return new Token.Delim('\\');
            case ']':
                look.advance();
                return Token.Static.RIGHT_BRACKET;
            case '{':
                look.advance();
                return Token.Static.LEFT_BRACE;
            case '}':
                look.advance();
                return Token.Static.RIGHT_BRACE;
            default:
                if (isDigit(c)) return consumeNumericToken();
                if (isIdentStart(c)) return consumeIdentLikeToken();
                look.advance();
                return new Token.Delim(c);
        }
    }

    /** §4.3.2 Consume comments. */
    private void consumeComments() {
        while (look.peek(0) == '/' && look.peek(1) == '*') {
            look.advance();
            look.advance();
            while (!look.isEof()) {
                if (look.peek(0) == '*' && look.peek(1) == '/') {
                    look.advance();
                    look.advance();
                    break;
                }
                look.advance();
            }
        }
    }

    /** §4.3.3 Consume a numeric token. */
    private @NotNull Token consumeNumericToken() {
        NumberValue n = consumeNumber();
        if (wouldStartIdentSequence(look.peek(0), look.peek(1), look.peek(2))) {
            String unit = consumeIdentSequence();
            return new Token.Dimension(n.value, n.numericType, unit);
        }
        if (look.peek(0) == '%') {
            look.advance();
            return new Token.Percentage(n.value);
        }
        return new Token.Number(n.value, n.numericType);
    }

    /** §4.3.4 Consume an ident-like token. */
    private @NotNull Token consumeIdentLikeToken() {
        String name = consumeIdentSequence();
        if (equalsIgnoreCaseAscii(name, "url") && look.peek(0) == '(') {
            look.advance(); // skip over '('
            while (look.peek(0) == ' ' && look.peek(1) == ' ') {
                look.advance();
            }

            int first = look.peek(0);
            int second = look.peek(1);
            if (first == '"' || first == '\'' || (first == ' ' && (second == '"' || second == '\''))) {
                return new Token.Function(name);
            }
            return consumeUrlToken();
        }
        if (look.peek(0) == '(') {
            look.advance();
            return new Token.Function(name);
        }
        return new Token.Ident(name);
    }

    /** §4.3.5 Consume a string token. */
    private @NotNull Token consumeStringToken(int endingCodePoint) {
        look.advance(); // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (look.isEof()) return new Token.Str(sb.toString());
            int c = look.peek(0);
            if (c == endingCodePoint) {
                look.advance();
                return new Token.Str(sb.toString());
            }
            if (c == '\n') return Token.Static.BAD_STRING;
            if (c == '\\') {
                if (look.isEofAt(1)) {
                    look.advance();
                    continue;
                }
                if (look.peek(1) == '\n') {
                    look.advance();
                    look.advance();
                    continue;
                }
                look.advance();
                sb.append(consumeEscapedCodePoint());
                continue;
            }
            sb.appendCodePoint(c);
            look.advance();
        }
    }

    /** §4.3.6 Consume a url token (caller has consumed "url(" and any leading whitespace). */
    private @NotNull Token consumeUrlToken() {
        StringBuilder sb = new StringBuilder();
        look.consumeWhile(Lexer::isWhitespace);
        while (true) {
            if (look.isEof()) return new Token.Url(sb.toString());
            int c = look.peek(0);
            if (c == ')') {
                look.advance();
                return new Token.Url(sb.toString());
            }
            if (isWhitespace(c)) {
                look.consumeWhile(Lexer::isWhitespace);
                if (look.isEof()) return new Token.Url(sb.toString());
                if (look.peek(0) == ')') {
                    look.advance();
                    return new Token.Url(sb.toString());
                }
                consumeBadUrlRemnants();
                return Token.Static.BAD_URL;
            }
            if (c == '"' || c == '\'' || c == '(' || isNonPrintable(c)) {
                consumeBadUrlRemnants();
                return Token.Static.BAD_URL;
            }
            if (c == '\\') {
                if (isValidEscapeStart(c, look.peek(1))) {
                    look.advance();
                    sb.append(consumeEscapedCodePoint());
                    continue;
                }
                consumeBadUrlRemnants();
                return Token.Static.BAD_URL;
            }
            sb.appendCodePoint(c);
            look.advance();
        }
    }

    /**
     * §4.3.7 Consume an escaped code point. Caller has consumed the leading backslash.
     * @return the consumed code point as a string, or the replacement character if invalid.
     */
    private @NotNull String consumeEscapedCodePoint() {
        if (look.isEof()) return String.valueOf(PreProcessor.REPLACEMENT_CHARACTER);
        int c = look.peek(0);
        if (isHexDigit(c)) {
            int number = 0;
            for (int i = 0; i < 6 && isHexDigit(look.peek(0)); i++) {
                number = (number << 4) + hexDigitToInt(look.peek(0));
                look.advance();
            }
            if (isWhitespace(look.peek(0))) look.advance();
            if (number == 0 || (number >= 0xD800 && number <= 0xDFFF) || number > 0x10FFFF) {
                return String.valueOf(PreProcessor.REPLACEMENT_CHARACTER);
            }
            return new String(Character.toChars(number));
        }
        look.advance();
        return new String(Character.toChars(c));
    }

    /** §4.3.8 Check if two code points are a valid escape. */
    private static boolean isValidEscapeStart(int first, int second) {
        return first == '\\' && second != '\n';
    }

    /** §4.3.9 Check if three code points would start an ident sequence. */
    private static boolean wouldStartIdentSequence(int first, int second, int third) {
        if (first == '-') {
            return isIdentStart(second) || second == '-' || isValidEscapeStart(second, third);
        }
        if (isIdentStart(first)) return true;
        if (first == '\\') return isValidEscapeStart(first, second);
        return false;
    }

    /** §4.3.10 Check if three code points would start a number. */
    private static boolean wouldStartNumber(int first, int second, int third) {
        if (first == '+' || first == '-') {
            if (isDigit(second)) return true;
            return second == '.' && isDigit(third);
        }
        if (first == '.') return isDigit(second);
        return isDigit(first);
    }

    /**
     * §4.3.11 Consume an ident sequence.
     * Assumes that the starting code points conform to {@link #wouldStartIdentSequence(int, int, int)}.
     */
    private @NotNull String consumeIdentSequence() {
        StringBuilder sb = new StringBuilder();
        while (!look.isEof()) {
            int c = look.peek(0);
            if (isIdentCodePoint(c)) {
                sb.appendCodePoint(c);
                look.advance();
            } else if (isValidEscapeStart(c, look.peek(1))) {
                look.advance();
                sb.append(consumeEscapedCodePoint());
            } else {
                break;
            }
        }
        return sb.toString();
    }

    /** §4.3.12 Consume a number. Returns the source representation, parsed value, and type flag. */
    private @NotNull NumberValue consumeNumber() {
        StringBuilder repr = new StringBuilder();
        Token.NumericType numericType = Token.NumericType.INTEGER;

        int c = look.peek(0);
        if (c == '+' || c == '-') {
            repr.appendCodePoint(c);
            look.advance();
        }
        while (isDigit(look.peek(0))) {
            repr.appendCodePoint(look.peek(0));
            look.advance();
        }
        if (look.peek(0) == '.' && isDigit(look.peek(1))) {
            repr.appendCodePoint(look.peek(0));
            look.advance();
            numericType = Token.NumericType.NUMBER;
            while (isDigit(look.peek(0))) {
                repr.appendCodePoint(look.peek(0));
                look.advance();
            }
        }
        int first = look.peek(0);
        int second = look.peek(1);
        int third = look.peek(2);
        boolean hasExponentSign = (second == '+' || second == '-') && isDigit(third);
        boolean hasExponent = (first == 'e' || first == 'E') && (isDigit(second) || hasExponentSign);

        if (hasExponent) {
            repr.appendCodePoint(look.peek(0));
            look.advance();
            if (hasExponentSign) {
                repr.appendCodePoint(look.peek(0));
                look.advance();
            }
            numericType = Token.NumericType.NUMBER;
            while (isDigit(look.peek(0))) {
                repr.appendCodePoint(look.peek(0));
                look.advance();
            }
        }
        return new NumberValue(convertStringToNumber(repr.toString()), numericType);
    }

    private static final class NumberValue {
        final float value;
        final @NotNull Token.NumericType numericType;

        NumberValue(float value, @NotNull Token.NumericType numericType) {
            this.value = value;
            this.numericType = numericType;
        }
    }

    /** §4.3.13 Consume the remnants of a bad url. */
    private void consumeBadUrlRemnants() {
        while (!look.isEof()) {
            int c = look.peek(0);
            if (c == ')') {
                look.advance();
                return;
            }
            if (c == '\\' && isValidEscapeStart(c, look.peek(1))) {
                look.advance();
                consumeEscapedCodePoint();
                continue;
            }
            look.advance();
        }
    }

    private @NotNull Token consumeHyphenStart() {
        int c1 = look.peek(1);
        int c2 = look.peek(2);
        if (wouldStartNumber('-', c1, c2)) return consumeNumericToken();
        if (c1 == '-' && c2 == '>') {
            look.advance();
            look.advance();
            look.advance();
            return Token.Static.CDC;
        }
        if (wouldStartIdentSequence('-', c1, c2)) return consumeIdentLikeToken();
        look.advance();
        return new Token.Delim('-');
    }

    private @NotNull Token consumeHashOrDelim() {
        int c1 = look.peek(1);
        int c2 = look.peek(2);
        int c3 = look.peek(3);
        boolean isHash = isIdentCodePoint(c1) || isValidEscapeStart(c1, c2);
        if (isHash) {
            Token.HashType hashType = wouldStartIdentSequence(c1, c2, c3)
                    ? Token.HashType.ID
                    : Token.HashType.UNRESTRICTED;
            look.advance();
            return new Token.Hash(consumeIdentSequence(), hashType);
        } else {
            look.advance();
            return new Token.Delim('#');
        }
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(int c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /** Returns -1 if not a valid hex digit */
    private static int hexDigitToInt(int c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static boolean isLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isIdentStart(int c) {
        return isLetter(c) || isNonAsciiCodePoint(c) || c == '_';
    }

    private static boolean isNonAsciiCodePoint(int c) {
        return c >= 0x80;
    }

    private static boolean isIdentCodePoint(int c) {
        return isIdentStart(c) || isDigit(c) || c == '-';
    }

    private static boolean isNonPrintable(int c) {
        return (c >= 0 && c <= 0x08) || c == 0x0B || (c >= 0x0E && c <= 0x1F) || c == 0x7F;
    }

    /**
     * §4.3.13 Converts a string to a number. Computed in double, then narrowed to the float used
     * throughout the geometry layer. Assumes that the string is a valid CSS number.
     */
    private static float convertStringToNumber(String str) {
        Matcher matcher = NUMBER_PATTERN.matcher(str);
        if (!matcher.matches())
            throw new ParserException();
        String sign = matcher.group("s") != null ? matcher.group("s") : "";
        String integer = matcher.group("i") != null ? matcher.group("i") : "";
        String fraction = matcher.group("f") != null ? matcher.group("f") : "";
        String exponentSign = matcher.group("t") != null ? matcher.group("t") : "";
        String exponent = matcher.group("e") != null ? matcher.group("e") : "";

        int s = sign.equals("-") ? -1 : 1; // sign of the number
        long i = integer.isEmpty() ? 0 : Long.parseUnsignedLong(integer); // integer part
        // fractional part as integer
        long f = fraction.isEmpty() ? 0 : Long.parseUnsignedLong(fraction.replaceFirst("^0+", ""));
        int d = fraction.length(); // number of fractional digits
        int t = exponentSign.equals("-") ? -1 : 1; // exponent sign
        long e = exponent.isEmpty() ? 0 : Long.parseUnsignedLong(exponent); // exponent

        return (float) (s * (i + f * Math.pow(10, -d)) * Math.pow(10, t * e));
    }

    private static boolean equalsIgnoreCaseAscii(@NotNull String a, @NotNull String b) {
        if (a.length() != b.length()) return false;
        for (int i = 0; i < a.length(); i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca >= 'A' && ca <= 'Z') ca += 32;
            if (cb >= 'A' && cb <= 'Z') cb += 32;
            if (ca != cb) return false;
        }
        return true;
    }

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("^(?<s>[-+]?)(?<i>[0-9]*+)\\.?(?<f>[0-9]*+)(?:[eE](?<t>[-+]?)(?<e>[0-9]*+))?$");

    /**
     * Sliding window of code points over a {@link PreProcessor}. Owns the circular-buffer
     * arithmetic and EOF semantics so the lexer body can stay focused on §4.3 token logic.
     */
    private static final class Lookahead {

        /** Returned by {@link #peek(int)} when the requested offset is past the input. */
        static final int EOF = -1;

        private final @NotNull PreProcessor input;
        private final int[] buffer;
        private final int capacity;
        private int head;
        private int count;

        Lookahead(@NotNull final PreProcessor input, final int capacity) {
            this.input = input;
            this.capacity = capacity;
            this.buffer = new int[capacity];
            for (int i = 0; i < capacity; i++) {
                int cp = input.read();
                if (cp == PreProcessor.EOF) break;
                buffer[count++] = cp;
            }
        }

        int peek(int offset) {
            if (offset >= count) return EOF;
            return buffer[(head + offset) % capacity];
        }

        void advance() {
            if (count == 0) return;
            head = (head + 1) % capacity;
            count--;
            int cp = input.read();
            if (cp != PreProcessor.EOF) {
                buffer[(head + count) % capacity] = cp;
                count++;
            }
        }

        boolean isEof() {
            return count == 0;
        }

        boolean isEofAt(int offset) {
            return offset >= count;
        }

        void consumeWhile(@NotNull IntPredicate filter) {
            while (count > 0 && filter.test(buffer[head]))
                advance();
        }
    }
}
