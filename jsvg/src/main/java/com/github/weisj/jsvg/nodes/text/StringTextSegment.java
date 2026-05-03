/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.text;

import java.text.BreakIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.supplier.ConstantSupplier;

final class StringTextSegment implements TextSegment {
    private final Supplier<List<String>> codepoints;
    private final TextContainer<?> parent;
    private final int index;

    @Nullable
    GlyphRun currentGlyphRun = null;
    @Nullable
    RenderContext currentRenderContext = null;

    public StringTextSegment(@NotNull TextContainer<?> parent, int index, @NotNull TextContent.Segment content) {
        this.parent = parent;
        this.index = index;

        if (content.isConstant()) {
            codepoints = new ConstantSupplier<>(segmentCodepoints(content.text()));
        } else {
            codepoints = new CachedCodepoints(content);
        }
    }

    @Override
    public boolean isSegmentVisible(@NotNull RenderContext currentContext) {
        return parent.isVisible(currentContext);
    }

    public @NotNull List<@NotNull String> codepoints() {
        return codepoints.get();
    }

    public boolean isLastSegmentInParent() {
        return index == parent.children().size() - 1;
    }

    private static @NotNull List<@NotNull String> segmentCodepoints(String text) {
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(new StringCharacterIterator(text));
        int start = it.first();
        List<String> characters = new ArrayList<>();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            characters.add(text.substring(start, end));
        }
        return characters;
    }

    private static class CachedCodepoints implements Supplier<List<String>> {
        private final @NotNull TextContent.Segment segment;
        private List<@NotNull String> codepoints;
        private @Nullable String lastText;

        private CachedCodepoints(@NotNull TextContent.Segment segment) {
            this.segment = segment;
        }

        @Override
        public List<String> get() {
            String text = segment.text();
            if (Objects.equals(text, lastText)) {
                return codepoints;
            }
            lastText = text;
            codepoints = segmentCodepoints(text);
            return codepoints;
        }
    }
}
