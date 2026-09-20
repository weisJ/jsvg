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
}
