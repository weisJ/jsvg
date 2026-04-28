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
package com.github.weisj.jsvg.parser.css.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.StyleProperty;
import com.github.weisj.jsvg.parser.css.impl.phase1.Lexer;
import com.github.weisj.jsvg.parser.css.impl.phase1.Token;
import com.github.weisj.jsvg.parser.css.impl.phase1.TokenType;

public final class SimpleCssParser implements CssParser {

    private static final Logger LOGGER = LogFactory.createLogger(SimpleCssParser.class);

    @Override
    public @NotNull SimpleStyleSheet parse(@NotNull List<char[]> input) {
        return new Parser(input).parseStyleSheet();
    }

    public @NotNull List<@NotNull StyleProperty> parseRules(@NotNull List<char[]> input) {
        return new Parser(input).parseDeclarationList();
    }

    private enum SelectorKind {
        TAG,
        ID,
        CLASS
    }

    private static final class Selector {
        final @NotNull SelectorKind kind;
        final @NotNull String name;

        Selector(@NotNull SelectorKind kind, @NotNull String name) {
            this.kind = kind;
            this.name = name;
        }
    }

    private static final class Parser {

        private final @NotNull Lexer lexer;
        private final @NotNull SimpleStyleSheet sheet = new SimpleStyleSheet();
        private @NotNull Token current;

        Parser(@NotNull List<char[]> input) {
            this.lexer = new Lexer(input);
            this.current = lexer.nextToken();
        }

        private void next() {
            current = lexer.nextToken();
        }

        private void skipWhitespace() {
            while (current.type() == TokenType.WHITESPACE)
                next();
        }

        private void expected(@NotNull String what) {
            LOGGER.log(Level.WARNING, () -> MessageFormat.format("Expected ''{0}'' but got ''{1}''", what, current));
        }

        @NotNull
        SimpleStyleSheet parseStyleSheet() {
            while (current.type() != TokenType.EOF) {
                if (current.type() == TokenType.WHITESPACE) {
                    next();
                    continue;
                }
                try {
                    List<Selector> selectors = readSelectorList();
                    consume(TokenType.LEFT_BRACE);
                    List<StyleProperty> properties = readDeclarations();
                    consume(TokenType.RIGHT_BRACE);
                    for (Selector s : selectors) {
                        switch (s.kind) {
                            case TAG:
                                sheet.addTagNameRules(s.name, properties);
                                break;
                            case ID:
                                sheet.addIdRules(s.name, properties);
                                break;
                            case CLASS:
                                sheet.addClassRules(s.name, properties);
                                break;
                        }
                    }
                } catch (ParserException e) {
                    skipToRuleEnd();
                }
            }
            return sheet;
        }

        @NotNull
        List<@NotNull StyleProperty> parseDeclarationList() {
            try {
                return readDeclarations();
            } catch (ParserException e) {
                return Collections.emptyList();
            }
        }

        private @NotNull List<Selector> readSelectorList() {
            List<Selector> list = new ArrayList<>();
            while (true) {
                skipWhitespace();
                if (current.type() == TokenType.LEFT_BRACE || current.type() == TokenType.EOF) break;
                list.add(readSelector());
                skipWhitespace();
                if (current.type() == TokenType.COMMA) {
                    next();
                    continue;
                }
                break;
            }
            return list;
        }

        private @NotNull Selector readSelector() {
            Token tok = current;
            switch (tok.type()) {
                case IDENT:
                    next();
                    return new Selector(SelectorKind.TAG, ((Token.Ident) tok).name());
                case HASH: {
                    Token.Hash hash = (Token.Hash) tok;
                    if (hash.hashType() != Token.HashType.ID) break;
                    next();
                    return new Selector(SelectorKind.ID, hash.name());
                }
                case DELIM:
                    if (((Token.Delim) tok).value() != '.') break;
                    next();
                    if (current.type() != TokenType.IDENT) {
                        expected("identifier after '.'");
                        throw new ParserException();
                    }
                    String name = ((Token.Ident) current).name();
                    next();
                    return new Selector(SelectorKind.CLASS, name);
                default:
                    break;
            }
            expected("selector");
            throw new ParserException();
        }

        private @NotNull List<StyleProperty> readDeclarations() {
            List<StyleProperty> list = new ArrayList<>();
            while (true) {
                skipWhitespace();
                TokenType t = current.type();
                if (t == TokenType.RIGHT_BRACE || t == TokenType.EOF) break;
                if (t == TokenType.SEMICOLON) {
                    next();
                    continue;
                }
                if (t != TokenType.IDENT) {
                    expected("identifier");
                    throw new ParserException();
                }
                String name = ((Token.Ident) current).name();
                next();
                skipWhitespace();
                if (current.type() != TokenType.COLON) {
                    expected("':'");
                    throw new ParserException();
                }
                next();
                String value = readDeclarationValue();
                list.add(new StyleProperty(name, value));
                if (current.type() == TokenType.SEMICOLON) next();
            }
            return list;
        }

        private @NotNull String readDeclarationValue() {
            skipWhitespace();
            StringBuilder sb = new StringBuilder();
            while (true) {
                TokenType t = current.type();
                if (t == TokenType.SEMICOLON || t == TokenType.RIGHT_BRACE || t == TokenType.EOF) break;
                sb.append(stringify(current));
                next();
            }
            int end = sb.length();
            while (end > 0 && Character.isWhitespace(sb.charAt(end - 1)))
                end--;
            return sb.substring(0, end);
        }

        private void consume(@NotNull TokenType type) {
            if (current.type() != type) {
                expected(type.toString());
                throw new ParserException();
            }
            next();
        }

        private void skipToRuleEnd() {
            while (current.type() != TokenType.RIGHT_BRACE && current.type() != TokenType.EOF) {
                next();
            }
            if (current.type() == TokenType.RIGHT_BRACE) next();
        }

        private static @NotNull String formatNumber(double v) {
            if (v == Math.floor(v) && !Double.isInfinite(v)
                    && v >= Long.MIN_VALUE && v <= Long.MAX_VALUE) {
                return Long.toString((long) v);
            }
            return Double.toString(v);
        }

        private static @NotNull String stringify(@NotNull Token t) {
            switch (t.type()) {
                case IDENT:
                    return ((Token.Ident) t).name();
                case FUNCTION:
                    return ((Token.Function) t).name() + "(";
                case AT_KEYWORD:
                    return "@" + ((Token.AtKeyword) t).name();
                case HASH:
                    return "#" + ((Token.Hash) t).name();
                case STRING:
                    return "\"" + ((Token.Str) t).value() + "\"";
                case BAD_STRING:
                    return "";
                case URL:
                    return "url(" + ((Token.Url) t).value() + ")";
                case BAD_URL:
                    return "url()";
                case DELIM:
                    return new String(Character.toChars(((Token.Delim) t).value()));
                case NUMBER:
                    return formatNumber(((Token.Number) t).value());
                case PERCENTAGE:
                    return formatNumber(((Token.Percentage) t).value()) + "%";
                case DIMENSION: {
                    Token.Dimension d = (Token.Dimension) t;
                    return formatNumber(d.value()) + d.unit();
                }
                case WHITESPACE:
                    return " ";
                case CDO:
                    return "<!--";
                case CDC:
                    return "-->";
                case COLON:
                    return ":";
                case SEMICOLON:
                    return ";";
                case COMMA:
                    return ",";
                case LEFT_BRACKET:
                    return "[";
                case RIGHT_BRACKET:
                    return "]";
                case LEFT_PAREN:
                    return "(";
                case RIGHT_PAREN:
                    return ")";
                case LEFT_BRACE:
                    return "{";
                case RIGHT_BRACE:
                    return "}";
                case EOF:
                default:
                    return "";
            }
        }
    }
}
