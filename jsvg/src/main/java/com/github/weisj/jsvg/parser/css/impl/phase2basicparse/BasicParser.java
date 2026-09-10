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
package com.github.weisj.jsvg.parser.css.impl.phase2basicparse;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Declaration;
import com.github.weisj.jsvg.parser.css.data.DeclarationListItem;
import com.github.weisj.jsvg.parser.css.data.Rule;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.data.TokenType;
import com.github.weisj.jsvg.parser.css.impl.FullCssParser;
import com.github.weisj.jsvg.parser.css.impl.phase1lexer.Lexer;

/**
 * CSS parser following <a href="https://www.w3.org/TR/css-syntax-3/#parsing">CSS Syntax Module Level 3, §5</a>.
 * Public methods are the §5.3 entry points; private methods are the §5.4 consume algorithms. Single-use
 * (one mutable cursor): instantiate per parse and call exactly one entry point. Never throws on malformed
 * input — parse errors are logged at {@link Level#WARNING} and parsing recovers per spec. Operates over
 * {@link ComponentValue}s via a {@link ComponentValueCursor} that adapts a {@link Lexer}.
 */
public final class BasicParser {

    private final @NotNull ComponentValueCursor componentValueCursor;

    private BasicParser(@NotNull BasicParserInput input) {
        this.componentValueCursor = ComponentValueCursor.from(input);
    }

    // ===== §5.3 Parser entry points =====

    /** §5.3.3 Parse a stylesheet (a top-level {@code <style>} element). */
    public static @NotNull List<@NotNull Rule> parseStylesheet(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        return parser.consumeListOfRules(true);
    }

    /** §5.3.4 Parse a list of rules (nested in a CSS structure). */
    public static @NotNull List<@NotNull Rule> parseListOfRules(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        return parser.consumeListOfRules(false);
    }

    /** §5.3.5 Parse a rule. */
    public static @Nullable Rule parseRule(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        parser.skipWhitespace();
        ComponentValue cv = parser.componentValueCursor.peek();
        if (isEof(cv)) {
            FullCssParser.logParseEvent("expected a rule, got EOF");
            return null;
        }
        Rule rule;
        if (isPreservedTokenOf(cv, TokenType.AT_KEYWORD)) {
            rule = parser.consumeAtRule();
        } else {
            rule = parser.consumeQualifiedRule();
            if (rule == null) return null;
        }
        parser.skipWhitespace();
        if (!isEof(parser.componentValueCursor.peek())) {
            FullCssParser.logParseEvent("trailing input after rule");
            return null;
        }
        return rule;
    }

    /** §5.3.6 Parse a declaration. */
    public static @Nullable Declaration parseDeclaration(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        parser.skipWhitespace();
        if (!isPreservedTokenOf(parser.componentValueCursor.peek(), TokenType.IDENT)) {
            FullCssParser.logParseEvent("expected an ident at the start of a declaration");
            return null;
        }
        return parser.consumeDeclaration();
    }

    /** §5.3.8 Parse a list of declarations. */
    public static @NotNull List<@NotNull DeclarationListItem> parseListOfDeclarations(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        return parser.consumeListOfDeclarations();
    }

    /** §5.3.9 Parse a component value. */
    public static @Nullable ComponentValue parseComponentValue(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        parser.skipWhitespace();
        if (isEof(parser.componentValueCursor.peek())) {
            FullCssParser.logParseEvent("expected a component value, got EOF");
            return null;
        }
        ComponentValue cv = parser.componentValueCursor.next();
        parser.skipWhitespace();
        if (!isEof(parser.componentValueCursor.peek())) {
            FullCssParser.logParseEvent("trailing input after component value");
            return null;
        }
        return cv;
    }

    /** §5.3.10 Parse a list of component values. */
    public static @NotNull List<@NotNull ComponentValue> parseListOfComponentValues(@NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        List<ComponentValue> list = new ArrayList<>();
        while (!isEof(parser.componentValueCursor.peek())) {
            list.add(parser.componentValueCursor.next());
        }
        return list;
    }

    /** §5.3.11 Parse a comma-separated list of component values. */
    public static @NotNull List<@NotNull List<@NotNull ComponentValue>> parseCommaSeparatedListOfComponentValues(
            @NotNull BasicParserInput input) {
        BasicParser parser = new BasicParser(input);
        List<List<ComponentValue>> result = new ArrayList<>();
        if (isEof(parser.componentValueCursor.peek())) return result;
        List<ComponentValue> current = new ArrayList<>();
        while (true) {
            ComponentValue cv = parser.componentValueCursor.next();
            if (isEof(cv)) {
                result.add(current);
                return result;
            }
            if (isPreservedTokenOf(cv, TokenType.COMMA)) {
                result.add(current);
                current = new ArrayList<>();
                continue;
            }
            current.add(cv);
        }
    }

    // ===== §5.4 Parser algorithms =====

    /** §5.4.1 Consume a list of rules. */
    private @NotNull List<@NotNull Rule> consumeListOfRules(boolean topLevel) {
        List<Rule> rules = new ArrayList<>();
        while (true) {
            ComponentValue cv = componentValueCursor.next();
            if (isEof(cv)) return rules;
            if (isPreservedTokenOf(cv, TokenType.WHITESPACE)) continue;
            if (isPreservedTokenOf(cv, TokenType.CDO) || isPreservedTokenOf(cv, TokenType.CDC)) {
                if (topLevel) continue;
                componentValueCursor.reconsume();
                Rule.QualifiedRule qr = consumeQualifiedRule();
                if (qr != null) rules.add(qr);
                continue;
            }
            if (isPreservedTokenOf(cv, TokenType.AT_KEYWORD)) {
                componentValueCursor.reconsume();
                rules.add(consumeAtRule());
                continue;
            }
            componentValueCursor.reconsume();
            Rule.QualifiedRule qr = consumeQualifiedRule();
            if (qr != null) rules.add(qr);
        }
    }

    /** §5.4.2 Consume an at-rule. The next input must be an at-keyword. */
    private @NotNull Rule.AtRule consumeAtRule() {
        ComponentValue first = componentValueCursor.next();
        String name = ((Token.AtKeyword) first).name();

        List<ComponentValue> prelude = new ArrayList<>();

        while (true) {
            ComponentValue cv = componentValueCursor.next();
            if (isEof(cv)) {
                FullCssParser.logParseEvent("unexpected EOF in @" + name);
                return new Rule.AtRule(name, prelude, null);
            }
            if (isPreservedTokenOf(cv, TokenType.SEMICOLON)) {
                return new Rule.AtRule(name, prelude, null);
            }
            if (cv instanceof ComponentValue.SimpleBlock.Brace) {
                return new Rule.AtRule(name, prelude, (ComponentValue.SimpleBlock.Brace) cv);
            }
            prelude.add(cv);
        }
    }

    /** §5.4.3 Consume a qualified rule. */
    private @Nullable Rule.QualifiedRule consumeQualifiedRule() {
        List<ComponentValue> prelude = new ArrayList<>();
        while (true) {
            ComponentValue cv = componentValueCursor.next();
            if (isEof(cv)) {
                FullCssParser.logParseEvent("EOF before qualified rule's block");
                return null;
            }
            if (cv instanceof ComponentValue.SimpleBlock.Brace) {
                return new Rule.QualifiedRule(prelude, (ComponentValue.SimpleBlock.Brace) cv);
            }
            prelude.add(cv);
        }
    }

    /** §5.4.5 Consume a list of declarations. */
    private @NotNull List<@NotNull DeclarationListItem> consumeListOfDeclarations() {
        List<DeclarationListItem> result = new ArrayList<>();
        while (true) {
            ComponentValue cv = componentValueCursor.next();
            if (isEof(cv)) return result;
            if (isPreservedTokenOf(cv, TokenType.WHITESPACE) || isPreservedTokenOf(cv, TokenType.SEMICOLON)) {
                continue;
            }
            if (isPreservedTokenOf(cv, TokenType.AT_KEYWORD)) {
                componentValueCursor.reconsume();
                result.add(consumeAtRule());
                continue;
            }
            if (isPreservedTokenOf(cv, TokenType.IDENT)) {
                List<ComponentValue> tempList = new ArrayList<>();
                tempList.add(cv);
                while (true) {
                    ComponentValue n = componentValueCursor.next();
                    if (isEof(n) || isPreservedTokenOf(n, TokenType.SEMICOLON)) {
                        componentValueCursor.reconsume();
                        break;
                    }
                    tempList.add(n);
                }
                Declaration decl = new BasicParser(BasicParserInput.fromComponentValues(tempList))
                        .consumeDeclaration();
                if (decl != null) result.add(decl);
                continue;
            }
            // anything else: parse error; skip until ; or EOF
            FullCssParser.logParseEvent("unexpected " + describe(cv) + " at start of declaration");
            componentValueCursor.reconsume();
            while (true) {
                ComponentValue n = componentValueCursor.next();
                if (isEof(n) || isPreservedTokenOf(n, TokenType.SEMICOLON)) {
                    componentValueCursor.reconsume();
                    break;
                }
            }
        }
    }

    /**
     * §5.4.6 Consume a declaration from a component-value list. Returns {@code null} on parse error.
     * <p>
     * Assumes that the next input token has already been checked to be an {@code <ident-token>}.
     */
    private @Nullable Declaration consumeDeclaration() {
        String declarationName = ((Token.Ident) componentValueCursor.next()).name();

        skipWhitespace();

        if (!isPreservedTokenOf(componentValueCursor.peek(), TokenType.COLON)) {
            FullCssParser.logParseEvent("expected ':' in declaration of '" + declarationName + "'");
            return null;
        }
        componentValueCursor.next();

        skipWhitespace();

        List<ComponentValue> declarationValue = new ArrayList<>();
        while (!isEof(componentValueCursor.peek())) {
            declarationValue.add(componentValueCursor.next());
        }

        // Detect !important: find the last and second-to-last non-whitespace component values.
        boolean important = false;
        int lastIdx = -1;
        int prevIdx = -1;
        for (int i = declarationValue.size() - 1; i >= 0; i--) {
            if (!isPreservedTokenOf(declarationValue.get(i), TokenType.WHITESPACE)) {
                if (lastIdx == -1) {
                    lastIdx = i;
                } else {
                    prevIdx = i;
                    break;
                }
            }
        }
        if (lastIdx >= 0 && prevIdx >= 0
                && isDelim(declarationValue.get(prevIdx), '!')
                && isIdentEqualsIgnoreCase(declarationValue.get(lastIdx), "important")) {
            declarationValue.subList(prevIdx, declarationValue.size()).clear();
            important = true;
        }

        // Trim trailing whitespace.
        int lastNonWhitespaceIdx = -1;
        for (int i = declarationValue.size() - 1; i >= 0; i--) {
            if (!isPreservedTokenOf(declarationValue.get(i), TokenType.WHITESPACE)) {
                lastNonWhitespaceIdx = i;
                break;
            }
        }
        if (lastNonWhitespaceIdx + 1 <= declarationValue.size()) {
            declarationValue.subList(lastNonWhitespaceIdx + 1, declarationValue.size()).clear();
        }

        return new Declaration(declarationName, declarationValue, important);
    }

    // ===== Helpers =====

    private void skipWhitespace() {
        while (true) {
            ComponentValue cv = componentValueCursor.next();
            if (!isPreservedTokenOf(cv, TokenType.WHITESPACE)) {
                componentValueCursor.reconsume();
                return;
            }
        }
    }

    private static boolean isEof(@NotNull ComponentValue cv) {
        return isPreservedTokenOf(cv, TokenType.EOF);
    }

    private static boolean isPreservedTokenOf(@NotNull ComponentValue cv, @NotNull TokenType type) {
        return cv instanceof Token && ((Token) cv).type() == type;
    }

    private static boolean isDelim(@NotNull ComponentValue cv, int value) {
        return cv instanceof Token.Delim && ((Token.Delim) cv).value() == value;
    }

    private static boolean isIdentEqualsIgnoreCase(@NotNull ComponentValue cv, @NotNull String name) {
        return cv instanceof Token.Ident && ((Token.Ident) cv).name().equalsIgnoreCase(name);
    }

    private static @NotNull String describe(@NotNull ComponentValue cv) {
        if (cv instanceof Token) {
            return ((Token) cv).type().name().toLowerCase() + "-token";
        }
        if (cv instanceof ComponentValue.SimpleBlock.Brace) return "{}-block";
        if (cv instanceof ComponentValue.SimpleBlock.Bracket) return "[]-block";
        if (cv instanceof ComponentValue.SimpleBlock.Paren) return "()-block";
        if (cv instanceof ComponentValue.FunctionBlock) {
            return ((ComponentValue.FunctionBlock) cv).name() + "()";
        }
        return cv.toString();
    }
}
