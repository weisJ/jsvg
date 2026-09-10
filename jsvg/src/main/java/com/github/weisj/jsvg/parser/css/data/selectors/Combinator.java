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

/**
 * Combinator following <a href="https://www.w3.org/TR/selectors-4/#combinators">Selectors Level 4</a>:
 * the relationship between two adjacent compound selectors in a complex selector.
 * <p>
 * Four combinators are modeled — descendant, child, and the two sibling combinators. The column
 * combinator ({@code ||}) also defined by Selectors Level 4 is not modeled here.
 */
public enum Combinator {
    /** Whitespace: matches any descendant. */
    DESCENDANT,
    /** {@code >}: matches a direct child. */
    CHILD,
    /** {@code +}: matches the immediately following sibling. */
    NEXT_SIBLING,
    /** {@code ~}: matches any subsequent sibling. */
    SUBSEQUENT_SIBLING
}
