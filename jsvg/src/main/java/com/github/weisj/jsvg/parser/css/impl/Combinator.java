/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


enum Combinator {
    CURRENT(""),
    DESCENDANT(" "),
    CHILD(">"),
    ADJACENT_SIBLING("+"),
    GENERAL_SIBLING("~");

    private final @NotNull String symbol;

    Combinator(@NotNull String symbol) {
        this.symbol = symbol;
    }

    public @NotNull String symbol() {
        return symbol;
    }

    @Contract(pure = true)
    public static Combinator fromSymbol(@NotNull String symbol) {
        switch (symbol) {
            case ">":
                return CHILD;
            case "+":
                return ADJACENT_SIBLING;
            case "~":
                return GENERAL_SIBLING;
            case " ":
                return DESCENDANT;
            case "":
                return CURRENT;
            default:
                throw new IllegalArgumentException("Unknown combinator symbol: " + symbol);
        }
    }
}
