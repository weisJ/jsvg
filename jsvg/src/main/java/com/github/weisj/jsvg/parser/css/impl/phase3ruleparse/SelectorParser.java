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
package com.github.weisj.jsvg.parser.css.impl.phase3ruleparse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Rule;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;
import com.github.weisj.jsvg.parser.css.data.selectors.Combinator;
import com.github.weisj.jsvg.parser.css.data.selectors.ComplexSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.CompoundSelector;
import com.github.weisj.jsvg.parser.css.data.selectors.SelectorList;
import com.github.weisj.jsvg.parser.css.data.selectors.SimpleSelector;

/**
 * Parses a {@link Rule.QualifiedRule#prelude() qualified rule's prelude} into a {@link SelectorList} per
 * <a href="https://www.w3.org/TR/selectors-4/#syntax">Selectors Level 4</a>. Not supported (any occurrence
 * invalidates the whole list): namespaces, {@code :not()}, non-structural pseudo-classes, and pseudo-elements
 * other than {@code ::before}. Returns {@code null} on any parse error so {@link CssNormalizer} drops the rule.
 */
public final class SelectorParser {

    private final @NotNull List<? extends @NotNull ComponentValue> input;
    private int pos;

    public SelectorParser(@NotNull final List<? extends @NotNull ComponentValue> input) {
        this.input = input;
    }

    /** Selector list. Returns {@code null} if the prelude is not a valid selector list. */
    public @Nullable SelectorList parse() {
        skipWhitespace();
        ComplexSelector first = parseSelector();
        if (first == null) return null;
        List<ComplexSelector> selectors = new ArrayList<>();
        selectors.add(first);
        while (pos < input.size()) {
            skipWhitespace();
            if (pos >= input.size()) break;
            if (!isPreservedTokenOf(current(), TokenType.COMMA)) {
                return null;
            }
            pos++;
            skipWhitespace();
            ComplexSelector next = parseSelector();
            if (next == null) return null;
            selectors.add(next);
        }
        if (selectors.isEmpty()) return null;
        return new SelectorList(selectors);
    }

    /** Complex selector: a chain of compound selectors separated by combinators. */
    private @Nullable ComplexSelector parseSelector() {
        CompoundSelector first = parseSequence();
        if (first == null) return null;
        List<CompoundSelector> sequences = new ArrayList<>();
        List<Combinator> combinators = new ArrayList<>();
        sequences.add(first);
        while (pos < input.size()) {
            int saved = pos;
            Combinator combinator = parseCombinator();
            if (combinator == null) break;
            CompoundSelector next = parseSequence();
            if (next == null) {
                pos = saved;
                return null;
            }
            sequences.add(next);
            combinators.add(combinator);
        }
        return new ComplexSelector(sequences, combinators);
    }

    /** Combinator: {@code +}, {@code >}, {@code ~}, or significant whitespace. */
    private @Nullable Combinator parseCombinator() {
        int start = pos;
        boolean sawWhitespace = false;
        while (pos < input.size() && isPreservedTokenOf(current(), TokenType.WHITESPACE)) {
            sawWhitespace = true;
            pos++;
        }
        if (pos >= input.size()) {
            pos = start;
            return null;
        }
        ComponentValue cv = current();
        if (isDelim(cv, '>')) {
            pos++;
            skipWhitespace();
            return Combinator.CHILD;
        }
        if (isDelim(cv, '+')) {
            pos++;
            skipWhitespace();
            return Combinator.NEXT_SIBLING;
        }
        if (isDelim(cv, '~')) {
            pos++;
            skipWhitespace();
            return Combinator.SUBSEQUENT_SIBLING;
        }
        if (sawWhitespace && couldStartSequence(cv)) {
            return Combinator.DESCENDANT;
        }
        pos = start;
        return null;
    }

    /** Compound selector: optional type/universal then any number of secondary selectors. */
    private @Nullable CompoundSelector parseSequence() {
        if (pos >= input.size()) return null;
        List<SimpleSelector> simples = new ArrayList<>();
        ComponentValue cv = current();
        if (isDelim(cv, '*')) {
            simples.add(SimpleSelector.Universal.INSTANCE);
            pos++;
        } else if (isPreservedTokenOf(cv, TokenType.IDENT)) {
            simples.add(new SimpleSelector.Type(((Token.Ident) cv).name()));
            pos++;
        }
        while (pos < input.size()) {
            cv = current();
            if (isPreservedTokenOf(cv, TokenType.HASH)) {
                Token.Hash hash = (Token.Hash) cv;
                if (hash.hashType() != Token.HashType.ID) return null;
                simples.add(new SimpleSelector.Id(hash.name()));
                pos++;
            } else if (isDelim(cv, '.')) {
                pos++;
                if (pos >= input.size() || !isPreservedTokenOf(current(), TokenType.IDENT)) return null;
                String name = ((Token.Ident) current()).name();
                simples.add(new SimpleSelector.Class(name));
                pos++;
            } else if (cv instanceof ComponentValue.SimpleBlock.Bracket) {
                SimpleSelector.Attribute attr =
                        parseAttributeSelector(((ComponentValue.SimpleBlock.Bracket) cv).value());
                if (attr == null) return null;
                simples.add(attr);
                pos++;
            } else if (isPreservedTokenOf(cv, TokenType.COLON)) {
                SimpleSelector pseudo = parsePseudo();
                if (pseudo == null) return null;
                simples.add(pseudo);
            } else {
                break;
            }
        }
        if (simples.isEmpty()) return null;
        return new CompoundSelector(simples);
    }

    /** Attribute selector: parses the contents of a {@code [...]} {@link ComponentValue.SimpleBlock.Bracket}. */
    private static @Nullable SimpleSelector.Attribute parseAttributeSelector(
            @NotNull List<? extends @NotNull ComponentValue> contents) {
        int p = 0;
        p = skipWs(contents, p);
        if (p >= contents.size() || !isPreservedTokenOf(contents.get(p), TokenType.IDENT)) return null;
        String name = ((Token.Ident) contents.get(p)).name();
        p++;
        p = skipWs(contents, p);
        if (p >= contents.size()) {
            return new SimpleSelector.Attribute(name, null, null, null);
        }
        SimpleSelector.Attribute.Operator op;
        ComponentValue first = contents.get(p);
        if (isDelim(first, '=')) {
            op = SimpleSelector.Attribute.Operator.EQUALS;
            p++;
        } else {
            // Two-character operator: <delim X> immediately followed by <delim '='>.
            if (p + 1 >= contents.size() || !isDelim(contents.get(p + 1), '=')) return null;
            if (isDelim(first, '~'))
                op = SimpleSelector.Attribute.Operator.INCLUDES;
            else if (isDelim(first, '|'))
                op = SimpleSelector.Attribute.Operator.DASH_MATCH;
            else if (isDelim(first, '^'))
                op = SimpleSelector.Attribute.Operator.PREFIX;
            else if (isDelim(first, '$'))
                op = SimpleSelector.Attribute.Operator.SUFFIX;
            else if (isDelim(first, '*'))
                op = SimpleSelector.Attribute.Operator.SUBSTRING;
            else
                return null;
            p += 2;
        }
        p = skipWs(contents, p);
        if (p >= contents.size()) return null;
        ComponentValue valCv = contents.get(p);
        String value;
        if (isPreservedTokenOf(valCv, TokenType.IDENT)) {
            value = ((Token.Ident) valCv).name();
        } else if (isPreservedTokenOf(valCv, TokenType.STRING)) {
            value = ((Token.Str) valCv).value();
        } else {
            return null;
        }
        p++;
        p = skipWs(contents, p);
        if (p >= contents.size()) return null;
        ComponentValue caseSensitivityCv = contents.get(p);
        Boolean caseSensitive = null;
        if (isPreservedTokenOf(caseSensitivityCv, TokenType.IDENT)) {
            switch (((Token.Ident) caseSensitivityCv).name()) {
                case "i":
                case "I":
                    caseSensitive = false;
                    break;
                case "s":
                case "S":
                    caseSensitive = true;
                    break;
                default:
                    return null;
            }
        } else {
            return null;
        }
        p++;
        p = skipWs(contents, p);
        if (p < contents.size()) return null;
        return new SimpleSelector.Attribute(name, op, value, caseSensitive);
    }

    /** Pseudo-class or pseudo-element. {@code ::} (or a legacy name) is a pseudo-element; {@code :} is a pseudo-class. */
    private @Nullable SimpleSelector parsePseudo() {
        pos++; // consume the leading ':'
        boolean doubleColon = false;
        if (pos < input.size() && isPreservedTokenOf(current(), TokenType.COLON)) {
            doubleColon = true;
            pos++;
        }
        if (pos >= input.size()) return null;
        ComponentValue cv = current();
        if (cv instanceof ComponentValue.FunctionBlock) {
            if (doubleColon) return null;
            pos++;
            return functionalPseudoClass((ComponentValue.FunctionBlock) cv);
        }
        if (!isPreservedTokenOf(cv, TokenType.IDENT)) return null;
        String name = ((Token.Ident) cv).name().toLowerCase(Locale.ENGLISH);
        pos++;
        if (doubleColon || isLegacyPseudoElementName(name)) {
            return pseudoElement(name);
        }
        return simplePseudoClass(name);
    }

    /** Only {@code before} is honored; other (legacy) pseudo-element names make the selector invalid. */
    private static @Nullable SimpleSelector pseudoElement(@NotNull String name) {
        if ("before".equals(name)) {
            return new SimpleSelector.PseudoElement(SimpleSelector.PseudoElement.Kind.BEFORE);
        }
        return null;
    }

    private static boolean isLegacyPseudoElementName(@NotNull String name) {
        return "before".equals(name) || "after".equals(name)
                || "first-line".equals(name) || "first-letter".equals(name);
    }

    private static @Nullable SimpleSelector simplePseudoClass(@NotNull String name) {
        SimpleSelector.PseudoClass.Kind kind;
        switch (name) {
            case "root":
                kind = SimpleSelector.PseudoClass.Kind.ROOT;
                break;
            case "empty":
                kind = SimpleSelector.PseudoClass.Kind.EMPTY;
                break;
            case "first-child":
                kind = SimpleSelector.PseudoClass.Kind.FIRST_CHILD;
                break;
            case "last-child":
                kind = SimpleSelector.PseudoClass.Kind.LAST_CHILD;
                break;
            case "only-child":
                kind = SimpleSelector.PseudoClass.Kind.ONLY_CHILD;
                break;
            case "first-of-type":
                kind = SimpleSelector.PseudoClass.Kind.FIRST_OF_TYPE;
                break;
            case "last-of-type":
                kind = SimpleSelector.PseudoClass.Kind.LAST_OF_TYPE;
                break;
            case "only-of-type":
                kind = SimpleSelector.PseudoClass.Kind.ONLY_OF_TYPE;
                break;
            default:
                return null;
        }
        return new SimpleSelector.PseudoClass(kind);
    }

    private static @Nullable SimpleSelector functionalPseudoClass(@NotNull ComponentValue.FunctionBlock fb) {
        SimpleSelector.PseudoClass.Kind kind;
        switch (fb.name().toLowerCase(Locale.ENGLISH)) {
            case "nth-child":
                kind = SimpleSelector.PseudoClass.Kind.NTH_CHILD;
                break;
            case "nth-last-child":
                kind = SimpleSelector.PseudoClass.Kind.NTH_LAST_CHILD;
                break;
            case "nth-of-type":
                kind = SimpleSelector.PseudoClass.Kind.NTH_OF_TYPE;
                break;
            case "nth-last-of-type":
                kind = SimpleSelector.PseudoClass.Kind.NTH_LAST_OF_TYPE;
                break;
            default:
                return null;
        }
        int[] ab = parseAnB(fb.value());
        if (ab == null) return null;
        return new SimpleSelector.PseudoClass(kind, ab[0], ab[1]);
    }

    /** Parses the {@code <an+b>} microsyntax (css-syntax-3 §"The An+B microsyntax"). Returns {@code {a, b}} or null. */
    private static int @Nullable [] parseAnB(@NotNull List<? extends @NotNull ComponentValue> values) {
        int p = skipWs(values, 0);
        if (p >= values.size()) return null;
        ComponentValue first = values.get(p);

        if (isPreservedTokenOf(first, TokenType.IDENT)) {
            String name = ((Token.Ident) first).name().toLowerCase(Locale.ENGLISH);
            if ("odd".equals(name)) return atEnd(values, p + 1) ? new int[] {2, 1} : null;
            if ("even".equals(name)) return atEnd(values, p + 1) ? new int[] {2, 0} : null;
        }

        // <integer>
        if (first instanceof Token.Number) {
            Token.Number n = (Token.Number) first;
            if (n.numericType() != Token.NumericType.INTEGER) return null;
            return atEnd(values, p + 1) ? new int[] {0, (int) n.value()} : null;
        }

        int a;
        int[] carrier; // length 0: trailing b allowed; length 1: b embedded in the ident/unit
        if (first instanceof Token.Dimension) {
            Token.Dimension dim = (Token.Dimension) first;
            if (dim.numericType() != Token.NumericType.INTEGER) return null;
            carrier = nDashDigit(dim.unit());
            if (carrier == null) return null;
            a = (int) dim.value();
            p++;
        } else if (isPreservedTokenOf(first, TokenType.IDENT)) {
            int[] c = coefficientFromIdent(((Token.Ident) first).name());
            if (c == null) return null;
            a = c[0];
            carrier = c.length == 2 ? new int[] {c[1]} : new int[0];
            p++;
        } else if (isDelim(first, '+')) {
            // '+' must be immediately followed by the n-ident (no whitespace)
            p++;
            if (p >= values.size() || !isPreservedTokenOf(values.get(p), TokenType.IDENT)) return null;
            int[] c = coefficientFromIdent(((Token.Ident) values.get(p)).name());
            if (c == null || c[0] != 1) return null;
            a = 1;
            carrier = c.length == 2 ? new int[] {c[1]} : new int[0];
            p++;
        } else {
            return null;
        }

        if (carrier.length == 1) {
            return atEnd(values, p) ? new int[] {a, carrier[0]} : null;
        }
        Integer b = trailingB(values, p);
        return b == null ? null : new int[] {a, b};
    }

    // 'n' -> {}, 'n-<digits>' -> {-digits}, else null (ASCII case-insensitive)
    private static int @Nullable [] nDashDigit(@NotNull String rawUnit) {
        String u = rawUnit.toLowerCase(Locale.ENGLISH);
        if ("n".equals(u)) return new int[0];
        if (!u.startsWith("n-")) return null;
        Integer digits = digits(u, 2);
        return digits == null ? null : new int[] {-digits};
    }

    // 'n'/'-n' -> {a}, 'n-<d>'/'-n-<d>' -> {a, -d}, else null
    private static int @Nullable [] coefficientFromIdent(@NotNull String raw) {
        String s = raw.toLowerCase(Locale.ENGLISH);
        int a;
        int idx;
        if (s.startsWith("-n")) {
            a = -1;
            idx = 2;
        } else if (s.startsWith("n")) {
            a = 1;
            idx = 1;
        } else {
            return null;
        }
        if (idx == s.length()) return new int[] {a};
        if (s.charAt(idx) != '-') return null;
        Integer digits = digits(s, idx + 1);
        return digits == null ? null : new int[] {a, -digits};
    }

    // optional trailing b; returns 0 if absent, null if malformed
    private static @Nullable Integer trailingB(@NotNull List<? extends @NotNull ComponentValue> values, int p) {
        int q = skipWs(values, p);
        if (q >= values.size()) return 0;
        ComponentValue t = values.get(q);
        if (t instanceof Token.Number) {
            Token.Number n = (Token.Number) t;
            if (n.numericType() != Token.NumericType.INTEGER || !atEnd(values, q + 1)) return null;
            return (int) n.value();
        }
        if (isDelim(t, '+') || isDelim(t, '-')) {
            int sign = isDelim(t, '+') ? 1 : -1;
            int r = skipWs(values, q + 1);
            if (r >= values.size() || !(values.get(r) instanceof Token.Number)) return null;
            Token.Number n = (Token.Number) values.get(r);
            if (n.numericType() != Token.NumericType.INTEGER || n.value() < 0 || !atEnd(values, r + 1)) return null;
            return sign * (int) n.value();
        }
        return null;
    }

    private static @Nullable Integer digits(@NotNull String s, int start) {
        if (start >= s.length()) return null;
        for (int i = start; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return null;
        }
        return Integer.parseInt(s.substring(start));
    }

    private static boolean atEnd(@NotNull List<? extends @NotNull ComponentValue> values, int p) {
        return skipWs(values, p) >= values.size();
    }

    private @NotNull ComponentValue current() {
        return input.get(pos);
    }

    private void skipWhitespace() {
        while (pos < input.size() && isPreservedTokenOf(current(), TokenType.WHITESPACE)) {
            pos++;
        }
    }

    private static int skipWs(@NotNull List<? extends @NotNull ComponentValue> list, int p) {
        while (p < list.size() && isPreservedTokenOf(list.get(p), TokenType.WHITESPACE))
            p++;
        return p;
    }

    private static boolean couldStartSequence(@NotNull ComponentValue cv) {
        if (cv instanceof ComponentValue.SimpleBlock.Bracket) return true;
        if (!(cv instanceof Token)) return false;
        Token t = (Token) cv;
        switch (t.type()) {
            case IDENT:
            case HASH:
            case COLON:
                return true;
            case DELIM:
                int v = ((Token.Delim) t).value();
                return v == '*' || v == '.';
            default:
                return false;
        }
    }

    private static boolean isPreservedTokenOf(@NotNull ComponentValue cv, @NotNull TokenType type) {
        return cv instanceof Token && ((Token) cv).type() == type;
    }

    private static boolean isDelim(@NotNull ComponentValue cv, int value) {
        return cv instanceof Token.Delim && ((Token.Delim) cv).value() == value;
    }
}
