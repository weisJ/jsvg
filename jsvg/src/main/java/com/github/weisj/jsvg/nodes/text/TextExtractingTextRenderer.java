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
package com.github.weisj.jsvg.nodes.text;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class TextExtractingTextRenderer implements TextRenderer {

    @Override
    public void render(@NotNull Text text, @NotNull RenderContext context, @NotNull Output output) {
        processContainer(text, context);
    }

    private void processContainer(@NotNull TextContainer textContainer, @NotNull RenderContext context) {
        for (TextSegment segment : textContainer.segments()) {
            if (!segment.isValid(context)) continue;
            if (segment instanceof StringTextSegment) {
                processSegment((StringTextSegment) segment);
            } else if (segment instanceof TextContainer) {
                processContainer((TextContainer) segment, context);
            }
        }
    }

    private void processSegment(@NotNull StringTextSegment segment) {
        processText(String.copyValueOf(segment.codepoints()));
    }

    public abstract void processText(@NotNull String text);
}
