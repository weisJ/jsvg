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
package com.github.weisj.jsvg.parser.css.data;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

/**
 * A CSS Syntax Module Level 3 token (§4.2). Each kind is a distinct nested type with only the fields the
 * spec assigns it; stateless kinds are singletons in {@link Static}, parametric kinds are immutable classes.
 * A token is a {@link ComponentValue}: per §5.2 every token except the bracket-opening and function tokens
 * is a preserved token; those are instead folded into a {@link ComponentValue.SimpleBlock} /
 * {@link ComponentValue.FunctionBlock} by the parser.
 */
@Immutable
public interface Token extends ComponentValue {

    @NotNull
    TokenType type();

    String serialize();

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
    enum Static implements Token {
        EOF(TokenType.EOF, ""),
        WHITESPACE(TokenType.WHITESPACE, " "),
        COLON(TokenType.COLON, ":"),
        SEMICOLON(TokenType.SEMICOLON, ";"),
        COMMA(TokenType.COMMA, ","),
        LEFT_BRACKET(TokenType.LEFT_BRACKET, "["),
        RIGHT_BRACKET(TokenType.RIGHT_BRACKET, "]"),
        LEFT_PAREN(TokenType.LEFT_PAREN, "("),
        RIGHT_PAREN(TokenType.RIGHT_PAREN, ")"),
        LEFT_BRACE(TokenType.LEFT_BRACE, "{"),
        RIGHT_BRACE(TokenType.RIGHT_BRACE, "}"),
        CDO(TokenType.CDO, "<!--"),
        CDC(TokenType.CDC, "-->"),
        BAD_STRING(TokenType.BAD_STRING, ""),
        BAD_URL(TokenType.BAD_URL, "");

        private final @NotNull TokenType type;
        private final @NotNull String serialized;

        Static(@NotNull TokenType type, @NotNull String serialized) {
            this.type = type;
            this.serialized = serialized;
        }

        @Override
        public @NotNull TokenType type() {
            return type;
        }

        @Override
        public String serialize() {
            return serialized;
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
        public String serialize() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ident)) return false;
            return name.equals(((Ident) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
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
        public String serialize() {
            return name + "(";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Function)) return false;
            return name.equals(((Function) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
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
        public String serialize() {
            return "@" + name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AtKeyword)) return false;
            return name.equals(((AtKeyword) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
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
        public String serialize() {
            return "#" + name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Hash)) return false;
            return name.equals(((Hash) o).name) && hashType == ((Hash) o).hashType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, hashType);
        }

        @Override
        public String toString() {
            return "Hash{name='" + name + "', hashType=" + hashType + "}";
        }
    }

    /** {@code <string-token>}. */
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
        public String serialize() {
            return "'" + value + "'";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Str)) return false;
            return value.equals(((Str) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
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
        public String serialize() {
            return "url(" + value + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Url)) return false;
            return value.equals(((Url) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
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
        public String serialize() {
            return new String(Character.toChars(value));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Delim)) return false;
            return value == ((Delim) o).value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Delim{value='" + new String(Character.toChars(value)) + "'}";
        }
    }

    /** {@code <number-token>} per §4.2: numeric value plus type flag. */
    @Immutable
    final class Number implements Token {
        private final float value;
        private final @NotNull NumericType numericType;

        public Number(float value, @NotNull NumericType numericType) {
            this.value = value;
            this.numericType = numericType;
        }

        public float value() {
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
        public String serialize() {
            if (numericType == NumericType.NUMBER) {
                return Float.toString(value);
            } else {
                return Integer.toString((int) value);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Number)) return false;
            return value == ((Number) o).value
                    && numericType == ((Number) o).numericType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, numericType);
        }

        @Override
        public String toString() {
            return "Number{value=" + value + ", numericType=" + numericType + "}";
        }
    }

    /** {@code <percentage-token>} per §4.2: numeric value only (no type flag). */
    @Immutable
    final class Percentage implements Token {
        private final float value;

        public Percentage(float value) {
            this.value = value;
        }

        public float value() {
            return value;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.PERCENTAGE;
        }

        @Override
        public String serialize() {
            return value + "%";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Percentage)) return false;
            return value == ((Percentage) o).value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Percentage{value=" + value + "}";
        }
    }

    /** {@code <dimension-token>} per §4.2: numeric value, type flag, and unit ident. */
    @Immutable
    final class Dimension implements Token {
        private final float value;
        private final @NotNull NumericType numericType;
        private final @NotNull String unit;

        public Dimension(float value, @NotNull NumericType numericType, @NotNull String unit) {
            this.value = value;
            this.numericType = numericType;
            this.unit = unit;
        }

        public float value() {
            return value;
        }

        public @NotNull NumericType numericType() {
            return numericType;
        }

        /** Non-empty string */
        public @NotNull String unit() {
            return unit;
        }

        @Override
        public @NotNull TokenType type() {
            return TokenType.DIMENSION;
        }

        @Override
        public String serialize() {
            if (numericType == NumericType.NUMBER) {
                return value + unit;
            } else {
                return ((int) value) + unit;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dimension)) return false;
            return value == ((Dimension) o).value
                    && numericType == ((Dimension) o).numericType
                    && unit.equals(((Dimension) o).unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, numericType, unit);
        }

        @Override
        public String toString() {
            return "Dimension{value=" + value + ", numericType=" + numericType + ", unit='" + unit + "'}";
        }
    }
}
