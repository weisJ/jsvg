/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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
package com.github.weisj.jsvg.parser;

public class DocumentLimits {
    public static final int DEFAULT_MAX_USE_NESTING_DEPTH = 15;
    public static final int DEFAULT_MAX_NESTING_DEPTH = 30;
    public static final int DEFAULT_MAX_PATH_COUNT = 2000;

    public static final DocumentLimits DEFAULT = new DocumentLimits(
            DEFAULT_MAX_NESTING_DEPTH, DEFAULT_MAX_USE_NESTING_DEPTH, DEFAULT_MAX_PATH_COUNT);

    private final int maxNestingDepth;
    private final int maxUseNestingDepth;
    private final int maxPathCount;

    public DocumentLimits(int maxNestingDepth, int maxUseNestingDepth, int maxPathCount) {
        this.maxNestingDepth = maxNestingDepth;
        this.maxUseNestingDepth = maxUseNestingDepth;
        this.maxPathCount = maxPathCount;
    }

    /**
     * The maximal allowed nesting depth of elements in the document.
     *
     * @return The maximal nesting depth of elements
     */
    public int maxNestingDepth() {
        return maxNestingDepth;
    }

    /**
     * The maximal allowed depth of nested <use> elements.
     *
     * @return The maximal nesting depth of <use>
     */
    public int maxUseNestingDepth() {
        return maxUseNestingDepth;
    }

    /**
     * The maximal allowed count of rendered instances of elements. This also counts implicit nodes created by
     * markers or <use> elements.
     * @return The maximal allowed count.
     */
    public int maxPathCount() {
        return maxPathCount;
    }
}
