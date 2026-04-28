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

/**
 * Token types from CSS Syntax Module Level 3, §4.2.
 * <p>See <a href="https://www.w3.org/TR/css-syntax-3/#tokenization">https://www.w3.org/TR/css-syntax-3/#tokenization</a>.
 */
public enum TokenType {
    IDENT,
    FUNCTION,
    AT_KEYWORD,
    HASH,
    STRING,
    BAD_STRING,
    URL,
    BAD_URL,
    DELIM,
    NUMBER,
    PERCENTAGE,
    DIMENSION,
    WHITESPACE,
    CDO,
    CDC,
    COLON,
    SEMICOLON,
    COMMA,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    EOF
}
