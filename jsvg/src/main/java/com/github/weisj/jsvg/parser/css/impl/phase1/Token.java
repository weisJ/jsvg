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
package com.github.weisj.jsvg.parser.css.impl.phase1;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

/**
 * A CSS Syntax Module Level 3 token (§4.2). Each token kind is a distinct nested type carrying
 * only the fields the spec assigns to it. Stateless kinds (punctuation, whitespace, EOF, CDO/CDC,
 * bad-*) are pre-allocated singletons in {@link Simple}; parametric kinds are immutable
 * inner classes.
 */
public interface Token {

    @NotNull TokenType type();

    /** Type flag of a {@code <hash-token>} per §4.2. */
    enum HashType {
        ID,
        UNRESTRICTED
    }

    /** Type flag of a {@code <number-token>} or {@code <dimension-token>} per §4.2. */
    enum NumericType {
        INTEGER,
        NUMBER
    }

    /** Stateless token kinds — one singleton per kind. */
    enum Simple implements Token {
        EOF(TokenType.EOF),
        WHITESPACE(TokenType.WHITESPACE),
        COLON(TokenType.COLON),
        SEMICOLON(TokenType.SEMICOLON),
        COMMA(TokenType.COMMA),
        LEFT_BRACKET(TokenType.LEFT_BRACKET),
        RIGHT_BRACKET(TokenType.RIGHT_BRACKET),
        LEFT_PAREN(TokenType.LEFT_PAREN),
        RIGHT_PAREN(TokenType.RIGHT_PAREN),
        LEFT_BRACE(TokenType.LEFT_BRACE),
        RIGHT_BRACE(TokenType.RIGHT_BRACE),
        CDO(TokenType.CDO),
        CDC(TokenType.CDC),
        BAD_STRING(TokenType.BAD_STRING),
        BAD_URL(TokenType.BAD_URL);

        private final @NotNull TokenType type;

        Simple(@NotNull TokenType type) {
            this.type = type;
        }

        @Override
        public @NotNull TokenType type() {
            return type;
        }
    }

    /** {@code <ident-token>}. */
    @Immutable
    final class Ident implements Token {
        private final @NotNull String name;

        public Ident(@NotNull String name) {
            this.name = name;
        }

        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.IDENT;
        }

        @Override
        public String toString() {
            return "Ident{name='" + name + "'}";
        }
    }

    /** {@code <function-token>}. The trailing {@code (} is implicit. */
    @Immutable
    final class Function implements Token {
        private final @NotNull String name;

        public Function(@NotNull String name) {
            this.name = name;
        }

        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.FUNCTION;
        }

        @Override
        public String toString() {
            return "Function{name='" + name + "'}";
        }
    }

    /** {@code <at-keyword-token>}. The leading {@code @} is implicit. */
    @Immutable
    final class AtKeyword implements Token {
        private final @NotNull String name;

        public AtKeyword(@NotNull String name) {
            this.name = name;
        }

        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.AT_KEYWORD;
        }

        @Override
        public String toString() {
            return "AtKeyword{name='" + name + "'}";
        }
    }

    /** {@code <hash-token>}. The leading {@code #} is implicit. */
    @Immutable
    final class Hash implements Token {
        private final @NotNull String name;
        private final @NotNull HashType hashType;

        public Hash(@NotNull String name, @NotNull HashType hashType) {
            this.name = name;
            this.hashType = hashType;
        }

        public @NotNull String name() {
            return name;
        }

        public @NotNull HashType hashType() {
            return hashType;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.HASH;
        }

        @Override
        public String toString() {
            return "Hash{name='" + name + "', hashType=" + hashType + "}";
        }
    }

    /** {@code <string-token>}. Named {@code Str} to avoid shadowing {@link java.lang.String}. */
    @Immutable
    final class Str implements Token {
        private final @NotNull String value;

        public Str(@NotNull String value) {
            this.value = value;
        }

        public @NotNull String value() {
            return value;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.STRING;
        }

        @Override
        public String toString() {
            return "Str{value='" + value + "'}";
        }
    }

    /** {@code <url-token>}. The {@code url(} prefix and {@code )} suffix are implicit. */
    @Immutable
    final class Url implements Token {
        private final @NotNull String value;

        public Url(@NotNull String value) {
            this.value = value;
        }

        public @NotNull String value() {
            return value;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.URL;
        }

        @Override
        public String toString() {
            return "Url{value='" + value + "'}";
        }
    }

    /** {@code <delim-token>}. Carries the single delimiter code point. */
    @Immutable
    final class Delim implements Token {
        private final int value;

        public Delim(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.DELIM;
        }

        @Override
        public String toString() {
            return "Delim{value='" + new String(Character.toChars(value)) + "'}";
        }
    }

    /** {@code <number-token>} per §4.2: numeric value plus type flag. */
    @Immutable
    final class Number implements Token {
        private final double value;
        private final @NotNull NumericType numericType;

        public Number(double value, @NotNull NumericType numericType) {
            this.value = value;
            this.numericType = numericType;
        }

        public double value() {
            return value;
        }

        public @NotNull NumericType numericType() {
            return numericType;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.NUMBER;
        }

        @Override
        public String toString() {
            return "Number{value=" + value + ", numericType=" + numericType + "}";
        }
    }

    /** {@code <percentage-token>} per §4.2: numeric value only (no type flag). */
    @Immutable
    final class Percentage implements Token {
        private final double value;

        public Percentage(double value) {
            this.value = value;
        }

        public double value() {
            return value;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.PERCENTAGE;
        }

        @Override
        public String toString() {
            return "Percentage{value=" + value + "}";
        }
    }

    /** {@code <dimension-token>} per §4.2: numeric value, type flag, and unit ident. */
    @Immutable
    final class Dimension implements Token {
        private final double value;
        private final @NotNull NumericType numericType;
        private final @NotNull String unit;

        public Dimension(double value, @NotNull NumericType numericType, @NotNull String unit) {
            this.value = value;
            this.numericType = numericType;
            this.unit = unit;
        }

        public double value() {
            return value;
        }

        public @NotNull NumericType numericType() {
            return numericType;
        }

        public @NotNull String unit() {
            return unit;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.DIMENSION;
        }

        @Override
        public String toString() {
            return "Dimension{value=" + value + ", numericType=" + numericType + ", unit='" + unit + "'}";
        }
    }
}
