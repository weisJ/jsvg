/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;


public final class Lexer {
    private static final Logger LOGGER = Logger.getLogger(Lexer.class.getName());

    private final @NotNull List<char[]> input;
    private int listIndex = 0;
    private int index = 0;

    private boolean inRuleDefinition;
    private boolean parsingRaw;


    public Lexer(@NotNull List<char[]> input) {
        this.input = input;
    }

    @NotNull
    public Token nextToken() {
        consumeWhiteSpace();

        if (inRuleDefinition && parsingRaw) {
            // Raw parsing of RHS
            parsingRaw = false;
            return new Token(TokenType.RAW_DATA, readWhile(c -> c != ';'));
        }

        if (isEof()) return new Token(TokenType.EOF);
        char c = current();
        switch (c) {
            case '{':
                inRuleDefinition = true;
                parsingRaw = false;
                next();
                return new Token(TokenType.CURLY_OPEN);
            case '}':
                inRuleDefinition = false;
                parsingRaw = false;
                next();
                return new Token(TokenType.CURLY_CLOSE);
            case ':':
                parsingRaw = true;
                next();
                return new Token(TokenType.COLON);
            case ';':
                next();
                return new Token(TokenType.SEMICOLON);
            case ',':
                next();
                return new Token(TokenType.COMMA);
            case '.':
                next();
                return new Token(TokenType.CLASS_NAME, readIdentifier());
            case '#':
                next();
                return new Token(TokenType.ID_NAME, readIdentifier());
            default:
                return new Token(TokenType.IDENTIFIER, readIdentifier());
        }
    }

    private boolean isEof() {
        return listIndex >= input.size()
                || (listIndex == input.size() - 1 && index >= input.get(listIndex).length);
    }

    private void consumeWhiteSpace() {
        while (Character.isWhitespace(current())) {
            next();
        }
    }

    private boolean isIdentifierCharStart(char c) {
        if ('A' <= c && c <= 'Z') return true;
        if ('a' <= c && c <= 'z') return true;
        if (c == '-') return true;
        return c == '_';
    }

    private boolean isIdentifierChar(char c) {
        if (isIdentifierCharStart(c)) return true;
        return '0' <= c && c <= '9';
    }

    private @NotNull String readIdentifier() {
        if (!isIdentifierCharStart(current()) || !isIdentifierChar(current())) {
            LOGGER.warning(() -> MessageFormat.format("Identifier starting with unexpected char ''{0}''", current()));
            if (readWhile(this::isIdentifierChar).isEmpty()) {
                next();
            }
            throw new ParserException();
        }
        return readWhile(this::isIdentifierChar);
    }

    private @NotNull String readWhile(@NotNull Predicate<Character> filter) {
        if (isEof()) return "";

        int startListIndex = listIndex;
        int startIndex = index;
        while (!isEof() && filter.test(current())) {
            next();
        }
        int endListIndex = isEof() ? input.size() - 1 : listIndex;
        int endIndex = isEof() ? input.get(endListIndex).length - 1 : index;

        StringBuilder builder = new StringBuilder();
        int start = startIndex;
        for (int i = startListIndex; i <= endListIndex; i++) {
            char[] segment = input.get(i);
            int end = (i == endListIndex) ? endIndex : segment.length - 1;

            builder.append(String.valueOf(segment, start, end - start));
            start = 0;
        }
        return builder.toString();
    }

    private char current() {
        if (isEof()) return 0;
        return input.get(listIndex)[index];
    }

    private void next() {
        index++;
        if (index >= input.get(listIndex).length && listIndex + 1 < input.size()) {
            index = 0;
            listIndex++;
        }
    }
}
