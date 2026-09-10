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
package com.github.weisj.jsvg.parser.css.data;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.impl.phase1lexer.Lexer;
import com.google.errorprone.annotations.Immutable;

/**
 * Component value following <a href="https://www.w3.org/TR/css-syntax-3/#parsing">CSS Syntax Module Level 3, §5</a>.
 * Per §5.2 one of a preserved token ({@link Token}, which implements this interface), a {@link SimpleBlock},
 * or a {@link FunctionBlock}. Built from the {@link Lexer}'s token stream; the bracket-opening and
 * {@link TokenType#FUNCTION} tokens are not preserved tokens — the parser consumes them into the corresponding
 * block kinds instead (§5.4.7–5.4.9).
 */
@Immutable
public interface ComponentValue {

    String serialize();

    /**
     * A simple block (§5.2 / §5.4.8). The bracket kind is the concrete subclass — {@link Brace},
     * {@link Bracket}, or {@link Paren} — so callers can distinguish them statically (e.g.
     * {@link Rule.QualifiedRule#block()} is typed as {@link Brace}).
     */
    @Immutable
    abstract class SimpleBlock implements ComponentValue {

        protected final @NotNull List<? extends @NotNull ComponentValue> value;

        /** Takes ownership of {@code value}. */
        protected SimpleBlock(@NotNull List<? extends @NotNull ComponentValue> value) {
            this.value = value;
        }

        /** Component values inside the block, in source order. */
        public final @NotNull List<? extends @NotNull ComponentValue> value() {
            return value;
        }

        /** A {@code {...}} curly-brace simple block. Delimits qualified-rule and at-rule bodies. */
        @Immutable
        public static final class Brace extends SimpleBlock {
            public Brace(@NotNull List<? extends @NotNull ComponentValue> value) {
                super(value);
            }

            @Override
            public String serialize() {
                return "{" + value.stream().map(ComponentValue::serialize).collect(Collectors.joining("")) + "}";
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Brace)) return false;
                return value.equals(((Brace) o).value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(value);
            }

            @Override
            public String toString() {
                return "Brace{" + value + "}";
            }
        }

        /** A {@code [...]} square-bracket simple block. */
        @Immutable
        public static final class Bracket extends SimpleBlock {
            public Bracket(@NotNull List<? extends @NotNull ComponentValue> value) {
                super(value);
            }

            @Override
            public String serialize() {
                return "[" + value.stream().map(ComponentValue::serialize).collect(Collectors.joining("")) + "]";
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Bracket)) return false;
                return value.equals(((Bracket) o).value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(value);
            }

            @Override
            public String toString() {
                return "Bracket{" + value + "}";
            }
        }

        /** A {@code (...)} round-bracket simple block. */
        @Immutable
        public static final class Paren extends SimpleBlock {
            public Paren(@NotNull List<? extends @NotNull ComponentValue> value) {
                super(value);
            }


            @Override
            public String serialize() {
                return "(" + value.stream().map(ComponentValue::serialize).collect(Collectors.joining("")) + ")";
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Paren)) return false;
                return value.equals(((Paren) o).value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(value);
            }

            @Override
            public String toString() {
                return "Paren{" + value + "}";
            }
        }
    }

    /** A function block (§5.4.9): {@code name(value)}. */
    @Immutable
    final class FunctionBlock implements ComponentValue {
        private final @NotNull String name;
        private final @NotNull List<? extends @NotNull ComponentValue> value;

        /** Takes ownership of {@code value}. */
        public FunctionBlock(@NotNull String name, @NotNull List<? extends @NotNull ComponentValue> value) {
            this.name = name;
            this.value = value;
        }

        public @NotNull String name() {
            return name;
        }

        public @NotNull List<? extends @NotNull ComponentValue> value() {
            return value;
        }

        @Override
        public String serialize() {
            return name + "(" + value.stream().map(ComponentValue::serialize).collect(Collectors.joining("")) + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FunctionBlock)) return false;
            FunctionBlock that = (FunctionBlock) o;
            return name.equals(that.name) && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "FunctionBlock{name='" + name + "', value=" + value + "}";
        }
    }

    default boolean isOneOfKeywords(String... keywords) {
        if (!(this instanceof Token.Ident)) {
            return false;
        }
        String keyword = ((Token.Ident) this).name();
        return Arrays.stream(keywords).anyMatch(keyword::equalsIgnoreCase);
    }

    default boolean isSlash() {
        return this instanceof Token.Delim && ((Token.Delim) this).value() == "/".codePointAt(0);
    }

    default boolean isColon() {
        return this instanceof Token.Delim && ((Token.Delim) this).value() == ":".codePointAt(0);
    }

    default boolean isLt() {
        return this instanceof Token.Delim && ((Token.Delim) this).value() == "<".codePointAt(0);
    }

    default boolean isGt() {
        return this instanceof Token.Delim && ((Token.Delim) this).value() == ">".codePointAt(0);
    }

    default boolean isEq() {
        return this instanceof Token.Delim && ((Token.Delim) this).value() == "=".codePointAt(0);
    }

    default boolean isANumberInRange(int min, int max) {
        return this instanceof Token.Number
                && ((Token.Number) this).value() >= min
                && ((Token.Number) this).value() <= max;
    }
}
