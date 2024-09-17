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

import java.awt.geom.AffineTransform;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class GlyphRunTextOutput implements TextOutput {

    private final StringBuilder codepoints = new StringBuilder();
    private @Nullable AffineTransform glyphTransform;
    private @Nullable RenderContext context;

    protected abstract void glyphRun(@NotNull String codepoints, @NotNull AffineTransform glyphTransform,
            @NotNull RenderContext context);

    protected abstract void onTextStart();

    protected abstract void onTextEnd();

    @Override
    public void codepoint(@NotNull String codepoint, @NotNull AffineTransform glyphTransform,
            @NotNull RenderContext context) {
        if (this.context == null) {
            this.context = context;
        }
        if (this.glyphTransform == null) {
            this.glyphTransform = glyphTransform;
        }
        codepoints.append(codepoint);
    }

    private void reset() {
        context = null;
        codepoints.setLength(0);
        glyphTransform = null;
    }

    @Override
    public final void beginText() {
        reset();
        onTextStart();
    }

    @Override
    public void glyphRunBreak() {
        if (codepoints.length() == 0) return;
        glyphRun(codepoints.toString(),
                Objects.requireNonNull(glyphTransform),
                Objects.requireNonNull(context));
        reset();
    }

    @Override
    public final void endText() {
        glyphRunBreak();
        onTextEnd();
    }
}
