/*
 * MIT License
 *
 * Copyright (c) 2025-2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.impl;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.TextContent;

public class TextContentImpl implements TextContent {
    private final @NotNull ParsedElement parent;
    private final @NotNull List<@NotNull List<Segment>> contentLists = new ArrayList<>();
    private boolean hasNonWhitespaceText = false;

    public TextContentImpl(@NotNull ParsedElement parent) {
        this.parent = parent;
        for (int i = 0; i <= parent.children().size(); i++) {
            // One before each child and one after the last child.
            contentLists.add(new ArrayList<>());
        }
    }

    /** Appends a segment to the current content list, tracking whether any non-whitespace text exists. */
    public void addSegmentToCurrentContentList(@NotNull Segment segment) {
        currentContentList().add(segment);
        if (!hasNonWhitespaceText && !segment.text().trim().isEmpty()) {
            hasNonWhitespaceText = true;
        }
    }

    /** Whether any non-whitespace text has been added (for the {@code :empty} pseudo-class). */
    public boolean hasNonWhitespaceText() {
        return hasNonWhitespaceText;
    }

    private void ensureSize() {
        while (contentLists.size() < parent.children().size() + 1) {
            contentLists.add(new ArrayList<>());
        }
    }

    public @NotNull List<@NotNull List<Segment>> contentLists() {
        return contentLists;
    }

    /** Copies this text content onto a new owner element (for {@code <use>} shadow-tree cloning). */
    @NotNull
    TextContentImpl copyFor(@NotNull ParsedElement newParent) {
        TextContentImpl copy = new TextContentImpl(newParent);
        copy.contentLists.clear();
        for (List<Segment> contentList : contentLists) {
            // Segments are immutable, so a shallow copy of each list is sufficient.
            copy.contentLists.add(new ArrayList<>(contentList));
        }
        copy.hasNonWhitespaceText = hasNonWhitespaceText;
        return copy;
    }

    public @NotNull List<Segment> currentContentList() {
        ensureSize();
        return contentLists.get(contentLists.size() - 1);
    }

    public void addContentList() {
        contentLists.add(new ArrayList<>());
    }

    @Override
    public @NotNull List<@NotNull Segment> contentAfterChildIndex(int childIndex) {
        ensureSize();
        return contentLists.get(childIndex + 1);
    }
}
