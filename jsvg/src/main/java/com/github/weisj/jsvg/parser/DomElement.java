/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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

import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DomElement {

    @Nullable
    String id();

    @NotNull
    String tagName();

    @NotNull
    List<@NotNull String> classNames();

    @NotNull
    DomDocument document();

    @NotNull
    List<? extends @NotNull DomElement> children();

    @Nullable
    String attribute(@NotNull String name);

    default @NotNull String attribute(@NotNull String name, @NotNull String fallback) {
        String value = attribute(name);
        return value != null ? value : fallback;
    }

    void setAttribute(@NotNull String name, @Nullable String value);

    @ApiStatus.Experimental
    @NotNull
    TextContent textContent();

    @Nullable
    DomElement parent();

    @ApiStatus.Experimental
    interface TextContent {
        /**
         * Returns the list of text fragments after the child element at the given index.
         * Returns the content before the first child if index is -1.
         *
         * @param childIndex the index of the child element
         * @return the list of text fragments
         */
        @ApiStatus.Experimental
        @NotNull
        List<@NotNull String> contentAfterChildIndex(int childIndex);
    }
}
