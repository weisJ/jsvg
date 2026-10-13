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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.css.data.Token;

class LexerTest {

    private static @NotNull List<Token> tokens(@NotNull String css) {
        Lexer lexer = new Lexer(() -> List.of(css.toCharArray()));
        List<Token> out = new ArrayList<>();
        for (Token t = lexer.nextToken(); t != Token.Static.EOF; t = lexer.nextToken()) {
            out.add(t);
        }
        return out;
    }

    @Test
    void urlWithQuotedArgumentIsAFunction() {
        // CSS Syntax 3 §4.3.4: url( followed by optional whitespace and a quote is a <function-token>. All
        // but the last whitespace code point are consumed, so one <whitespace-token> remains before the
        // string.
        List<Token> quoted = List.of(new Token.Function("url"), new Token.Str("a.svg"), Token.Static.RIGHT_PAREN);
        List<Token> spaced = List.of(new Token.Function("url"), Token.Static.WHITESPACE, new Token.Str("a.svg"),
                Token.Static.RIGHT_PAREN);
        assertEquals(quoted, tokens("url(\"a.svg\")"));
        assertEquals(spaced, tokens("url( \"a.svg\")"));
        assertEquals(spaced, tokens("url(  \"a.svg\")"));
        assertEquals(spaced, tokens("url(\t\"a.svg\")"));
        assertEquals(spaced, tokens("url(\n\"a.svg\")"));
        assertEquals(spaced, tokens("url( \t\n \"a.svg\")"));
        assertEquals(spaced, tokens("url(\n'a.svg')"));
    }

    @Test
    void unquotedUrl() {
        // §4.3.6: whitespace before the argument is consumed; whitespace inside makes it a <bad-url-token>.
        assertEquals(List.of(new Token.Url("#a")), tokens("url(#a)"));
        assertEquals(List.of(new Token.Url("#a")), tokens("url( #a )"));
        assertEquals(List.of(new Token.Url("#a")), tokens("url(\t\n#a\n)"));
        assertEquals(List.of(Token.Static.BAD_URL), tokens("url(#a b)"));
        assertEquals(List.of(Token.Static.BAD_URL), tokens("url(a\"b)"));
    }

    @Test
    void identAndFunction() {
        assertEquals(List.of(new Token.Ident("url")), tokens("url"));
        assertEquals(List.of(new Token.Function("rgb")), tokens("rgb("));
        assertEquals(List.of(new Token.Function("urls")), tokens("urls(")); // any ident + '(' is a function
    }

    @Test
    void numberWithAllZeroFraction() {
        assertEquals(List.of(new Token.Number(0, Token.NumericType.NUMBER)), tokens("0.000000"));
        assertEquals(List.of(new Token.Number(509, Token.NumericType.NUMBER)), tokens("509.000000"));
        assertEquals(List.of(new Token.Number(-1, Token.NumericType.NUMBER)), tokens("-1.0"));
        assertEquals(List.of(new Token.Number(0.05f, Token.NumericType.NUMBER)), tokens("0.050"));
        assertEquals(List.of(new Token.Number(0, Token.NumericType.NUMBER)), tokens(".000"));
        assertEquals(List.of(new Token.Number(100, Token.NumericType.NUMBER)), tokens("1.000e2"));
        assertEquals(List.of(new Token.Dimension(1, Token.NumericType.NUMBER, "px")), tokens("1.000px"));
        assertEquals(List.of(new Token.Percentage(50)), tokens("50.00%"));
    }

    @Test
    void integerNumbers() {
        assertEquals(List.of(integer(0)), tokens("0"));
        assertEquals(List.of(integer(42)), tokens("42"));
        assertEquals(List.of(integer(7)), tokens("+7"));
        assertEquals(List.of(integer(-7)), tokens("-7"));
        assertEquals(List.of(integer(7)), tokens("007"));
    }

    @Test
    void fractionalNumbers() {
        assertEquals(List.of(number(1.5f)), tokens("1.5"));
        assertEquals(List.of(number(-0.5f)), tokens("-0.5"));
        assertEquals(List.of(number(0.5f)), tokens(".5"));
        assertEquals(List.of(number(0.5f)), tokens("+.5"));
        assertEquals(List.of(number(-0.5f)), tokens("-.5"));
        assertEquals(List.of(number(0.1f)), tokens("0.1"));
        assertEquals(List.of(number(1.05f)), tokens("1.05"));
        assertEquals(List.of(number(0.0001f)), tokens("0.0001"));
    }

    @Test
    void exponentNumbers() {
        // §4.3.12: an exponent makes the type "number" even without a fraction
        assertEquals(List.of(number(1000)), tokens("1e3"));
        assertEquals(List.of(number(1000)), tokens("1E3"));
        assertEquals(List.of(number(1000)), tokens("1e+3"));
        assertEquals(List.of(number(0.001f)), tokens("1e-3"));
        assertEquals(List.of(number(150)), tokens("1.5e2"));
        assertEquals(List.of(number(-0.25f)), tokens("-2.5E-1"));
        assertEquals(List.of(number(2)), tokens("2e0"));
        assertEquals(List.of(number(0.5f)), tokens(".5e0"));
        assertEquals(List.of(number(1000)), tokens("1e00000000000000000003"));
    }

    @Test
    void negativeZeroKeepsItsSign() {
        // Token equality uses ==, which treats -0 and 0 as equal
        assertEquals(Float.floatToIntBits(-0f), Float.floatToIntBits(numberValue("-0")));
        assertEquals(Float.floatToIntBits(-0f), Float.floatToIntBits(numberValue("-0.0")));
        assertEquals(Float.floatToIntBits(0f), Float.floatToIntBits(numberValue("+0")));
    }

    @Test
    void numbersWithManyDigits() {
        assertEquals(List.of(number(0.5f)), tokens("0.50000000000000000000"));
        assertEquals(List.of(number(0.12345679f)), tokens("0.12345678901234567890123"));
        assertEquals(List.of(integer(1.2345679E20f)), tokens("123456789012345678901"));
        assertEquals(List.of(number(0)), tokens("0e999"));
        assertEquals(List.of(number(0)), tokens("0e99999999999999999999"));
        assertEquals(List.of(number(0)), tokens("1e-99999999999999999999"));
    }

    @Test
    void numbersAreRoundedToFloatOnce() {
        // expected values are Java literals, which are correctly rounded
        assertEquals(List.of(number(1.64688E10f)), tokens("1646.880e7"));
        assertEquals(List.of(integer(16777216f)), tokens("16777217"));
        assertEquals(List.of(number(Float.MAX_VALUE)), tokens("3.4028235e38"));
        assertEquals(List.of(number(Float.MIN_VALUE)), tokens("1.4e-45"));
    }

    @Test
    void numericDimensionsAndPercentages() {
        // "e" not followed by a digit starts the unit, not an exponent
        assertEquals(List.of(new Token.Dimension(1, Token.NumericType.INTEGER, "em")), tokens("1em"));
        assertEquals(List.of(new Token.Dimension(2, Token.NumericType.INTEGER, "ex")), tokens("2ex"));
        assertEquals(List.of(new Token.Dimension(1000, Token.NumericType.NUMBER, "em")), tokens("1e3em"));
        assertEquals(List.of(new Token.Dimension(-0.5f, Token.NumericType.NUMBER, "px")), tokens("-.5px"));
        assertEquals(List.of(new Token.Percentage(100)), tokens("1e2%"));
        assertEquals(List.of(new Token.Percentage(5)), tokens("+5%"));
    }

    @Test
    void dotWithoutDigitsIsNotPartOfTheNumber() {
        assertEquals(List.of(integer(1), new Token.Delim('.')), tokens("1."));
    }

    private static @NotNull Token.Number integer(float value) {
        return new Token.Number(value, Token.NumericType.INTEGER);
    }

    private static @NotNull Token.Number number(float value) {
        return new Token.Number(value, Token.NumericType.NUMBER);
    }

    private static float numberValue(@NotNull String css) {
        List<Token> tokens = tokens(css);
        assertEquals(1, tokens.size(), css);
        return ((Token.Number) tokens.get(0)).value();
    }
}
