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
package com.github.weisj.jsvg.parser.css.data.selectors;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.impl.phase4matcher.StyleRuleMatcher;
import com.github.weisj.jsvg.parser.impl.ParsedElement;
import com.google.errorprone.annotations.Immutable;

/**
 * Simple selector following <a href="https://www.w3.org/TR/selectors-4/#simple">Selectors Level 4</a>:
 * the leaf node of the selector AST. Each kind contributes a fixed amount to the
 * {@link Specificity} of the enclosing compound selector.
 * <p>
 * Selectors Level 4 enumerates type, universal, attribute, class and id selectors, plus
 * pseudo-class and pseudo-element selectors. Structural pseudo-classes are modeled by
 * {@link PseudoClass}. Pseudo-element selectors are modeled but currently restricted to
 * {@code ::before} and its legacy single-colon form {@code :before}.
 */
@Immutable
public interface SimpleSelector {

    /** Specificity contribution of this simple selector. */
    @NotNull
    Specificity specificity();

    @NotNull
    MatchResult matches(@NotNull ParsedElement targetElement);

    /** Type selector: an element name (e.g. {@code circle}, {@code text}). Specificity {@code (0,0,1)}. */
    @Immutable
    final class Type implements SimpleSelector {
        private final @NotNull String name;

        public Type(@NotNull String name) {
            this.name = name;
        }

        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ONE_TYPE;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            return new MatchResult(name.equals(targetElement.tagName()), false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type)) return false;
            return name.equals(((Type) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Universal selector: {@code *}. Specificity {@code (0,0,0)}. */
    @Immutable
    final class Universal implements SimpleSelector {

        public static final @NotNull Universal INSTANCE = new Universal();

        private Universal() {}

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ZERO_IN_STYLESHEET;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            return new MatchResult(true, false);
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    /** Id selector: {@code #foo}. Specificity {@code (1,0,0)}. */
    @Immutable
    final class Id implements SimpleSelector {
        private final @NotNull String id;

        public Id(@NotNull String id) {
            this.id = id;
        }

        public @NotNull String id() {
            return id;
        }

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ONE_ID;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            return new MatchResult(id.equals(targetElement.id()), false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id)) return false;
            return id.equals(((Id) o).id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "#" + id;
        }
    }

    /** Class selector: {@code .foo}. Specificity {@code (0,1,0)}. */
    @Immutable
    final class Class implements SimpleSelector {
        private final @NotNull String name;

        public Class(@NotNull String name) {
            this.name = name;
        }

        public @NotNull String name() {
            return name;
        }

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ONE_CLASS;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            return new MatchResult(targetElement.classNames().contains(name), false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Class)) return false;
            return name.equals(((Class) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "." + name;
        }
    }

    /** Attribute selector: {@code [attr]}, {@code [attr=value]}, {@code [attr^=value]}, etc. Specificity {@code (0,1,0)}. */
    @Immutable
    final class Attribute implements SimpleSelector {

        /** The match operator, or absent for the bare {@code [attr]} form. */
        public enum Operator {
            /** {@code =} — exact match. */
            EQUALS,
            /** {@code ~=} — whitespace-separated word match. */
            INCLUDES,
            /** {@code |=} — equals or starts-with-then-hyphen (language matching). */
            DASH_MATCH,
            /** {@code ^=} — prefix match. */
            PREFIX,
            /** {@code $=} — suffix match. */
            SUFFIX,
            /** {@code *=} — substring match. */
            SUBSTRING
        }

        private final @NotNull String name;
        private final @Nullable Operator operator;
        private final @Nullable String value;
        private final @Nullable Boolean caseSensitive;

        public Attribute(@NotNull String name,
                @Nullable Operator operator,
                @Nullable String value,
                @Nullable Boolean caseSensitive) {
            if ((operator == null) != (value == null)) {
                throw new IllegalArgumentException("operator and value must both be present or both absent");
            }
            this.name = name;
            this.operator = operator;
            this.value = value;
            this.caseSensitive = caseSensitive;
        }

        public @NotNull String name() {
            return name;
        }

        public @Nullable Operator operator() {
            return operator;
        }

        public @Nullable String value() {
            return value;
        }

        public @Nullable Boolean caseSensitive() {
            return caseSensitive;
        }

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ONE_CLASS;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            boolean caseSensitive = this.caseSensitive != null ? this.caseSensitive
                    : StyleRuleMatcher.ATTRIBUTES_WITH_CASE_INSENSITIVE_VALUES.contains(name);

            String attributeValue = targetElement.attributeNode().declaredAttributes().get(name);

            if (operator == null) { // [name]
                return new MatchResult(attributeValue != null, false);
            }
            if (attributeValue == null) {
                return new MatchResult(false, false);
            }
            switch (operator) { // since operator is non-null, value is also non-null
                case EQUALS: { // [name=value]
                    return new MatchResult(
                            caseSensitive ? attributeValue.equals(value) : attributeValue.equalsIgnoreCase(value),
                            false);
                }
                case INCLUDES: { // [name~=value]
                    return new MatchResult(Arrays.stream(attributeValue.split("\\s+"))
                            .anyMatch(word -> caseSensitive ? word.equals(value) : word.equalsIgnoreCase(value)),
                            false);
                }
                case DASH_MATCH: { // [name|=value]
                    return new MatchResult(caseSensitive
                            ? attributeValue.equals(value) || attributeValue.startsWith(value + "-")
                            : attributeValue.equalsIgnoreCase(value)
                                    || startsWithIgnoreCase(attributeValue, value + "-"),
                            false);
                }
                case PREFIX: { // [name^=value]
                    return new MatchResult(caseSensitive ? attributeValue.startsWith(value)
                            : startsWithIgnoreCase(attributeValue, value), false);
                }
                case SUFFIX: { // [name$=value]
                    return new MatchResult(
                            caseSensitive ? attributeValue.endsWith(value) : endsWithIgnoreCase(attributeValue, value),
                            false);
                }
                case SUBSTRING: { // [name*=value]
                    return new MatchResult(caseSensitive
                            ? attributeValue.contains(value)
                            : attributeValue.toLowerCase(Locale.ENGLISH).contains(value.toLowerCase(Locale.ENGLISH)),
                            false);
                }
                default: {
                    throw new IllegalArgumentException("Unsupported selector operator: " + operator);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Attribute)) return false;
            Attribute that = (Attribute) o;
            return name.equals(that.name)
                    && operator == that.operator
                    && Objects.equals(value, that.value)
                    && Objects.equals(caseSensitive, that.caseSensitive);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, operator, value, caseSensitive);
        }

        @Override
        public String toString() {
            return "Attribute{name='" + name + "'"
                    + (operator != null ? ", op=" + operator + ", value='" + value + "'" : "")
                    + (caseSensitive != null ? ", caseSensitive=" + caseSensitive : "")
                    + "}";
        }

        private boolean startsWithIgnoreCase(String main, String prefix) {
            return main.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH));
        }

        private boolean endsWithIgnoreCase(String main, String suffix) {
            return main.toLowerCase(Locale.ENGLISH).endsWith(suffix.toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * Pseudo-element selector: {@code ::before}, or its legacy single-colon form {@code :before}.
     * Specificity {@code (0,0,1)}. Currently only {@link Kind#BEFORE} is supported.
     */
    @Immutable
    final class PseudoElement implements SimpleSelector {

        public enum Kind {
            BEFORE
        }

        private final @NotNull Kind kind;

        public PseudoElement(@NotNull Kind kind) {
            this.kind = kind;
        }

        public @NotNull Kind kind() {
            return kind;
        }

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ONE_TYPE;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            return new MatchResult(false, false); // pseudo-elements are parsed but not implemented
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PseudoElement)) return false;
            return kind == ((PseudoElement) o).kind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "::" + kind.name().toLowerCase();
        }
    }

    /**
     * Structural pseudo-class selector (e.g. {@code :first-child}, {@code :nth-of-type(2n+1)}).
     * Specificity {@code (0,1,0)}. For the {@code NTH_*} kinds {@link #a} and {@link #b} hold the
     * {@code an+b} coefficients; they are unused (0) otherwise.
     */
    @Immutable
    final class PseudoClass implements SimpleSelector {

        public enum Kind {
            ROOT,
            EMPTY,
            FIRST_CHILD,
            LAST_CHILD,
            ONLY_CHILD,
            FIRST_OF_TYPE,
            LAST_OF_TYPE,
            ONLY_OF_TYPE,
            NTH_CHILD,
            NTH_LAST_CHILD,
            NTH_OF_TYPE,
            NTH_LAST_OF_TYPE
        }

        private final @NotNull Kind kind;
        private final int a;
        private final int b;

        public PseudoClass(@NotNull Kind kind) {
            this(kind, 0, 0);
        }

        public PseudoClass(@NotNull Kind kind, int a, int b) {
            this.kind = kind;
            this.a = a;
            this.b = b;
        }

        public @NotNull Kind kind() {
            return kind;
        }

        public int a() {
            return a;
        }

        public int b() {
            return b;
        }

        @Override
        public @NotNull Specificity specificity() {
            return Specificity.ONE_CLASS;
        }

        @Override
        public @NotNull MatchResult matches(@NotNull ParsedElement targetElement) {
            if (kind == Kind.ROOT) {
                return new MatchResult(targetElement.parent() == null, true);
            }
            if (kind == Kind.EMPTY) {
                return new MatchResult(
                        targetElement.children().isEmpty() && !targetElement.hasNonWhitespaceText(), false);
            }
            // everything else depends on the element's position among its siblings
            return new MatchResult(matchesStructural(targetElement), true);
        }

        private boolean matchesStructural(@NotNull ParsedElement element) {
            ParsedElement parent = element.parent();
            if (parent == null) return false;
            switch (kind) {
                case FIRST_CHILD:
                    return element.oneBasedIndexInParent() == 1;
                case LAST_CHILD:
                    return element.oneBasedIndexInParent() == parent.children().size();
                case ONLY_CHILD:
                    return parent.children().size() == 1;
                case NTH_CHILD:
                    return matchesNth(element.oneBasedIndexInParent());
                case NTH_LAST_CHILD:
                    return matchesNth(parent.children().size() - element.oneBasedIndexInParent() + 1);
                case FIRST_OF_TYPE:
                    return element.oneBasedIndexAmongSiblingsWithSameTagName() == 1;
                case LAST_OF_TYPE:
                    return element.oneBasedIndexAmongSiblingsWithSameTagName() == parent
                            .childCountWithTagName(element.tagName());
                case ONLY_OF_TYPE:
                    return parent.childCountWithTagName(element.tagName()) == 1;
                case NTH_OF_TYPE:
                    return matchesNth(element.oneBasedIndexAmongSiblingsWithSameTagName());
                case NTH_LAST_OF_TYPE:
                    return matchesNth(parent.childCountWithTagName(element.tagName())
                            - element.oneBasedIndexAmongSiblingsWithSameTagName() + 1);
                default:
                    throw new IllegalStateException("Unhandled pseudo-class: " + kind);
            }
        }

        /** Does index (1-based) satisfy an+b for any integer n >= 0 */
        private boolean matchesNth(int index) {
            if (a == 0) return index == b;
            int diff = index - b;
            return diff % a == 0 && diff / a >= 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PseudoClass)) return false;
            PseudoClass that = (PseudoClass) o;
            return kind == that.kind && a == that.a && b == that.b;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, a, b);
        }

        @Override
        public String toString() {
            return ":" + kind.name().toLowerCase(Locale.ENGLISH).replace('_', '-');
        }
    }
}
