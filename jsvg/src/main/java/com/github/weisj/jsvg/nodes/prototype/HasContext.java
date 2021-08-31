/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
package com.github.weisj.jsvg.nodes.prototype;

import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.PaintContext;

/**
 * Informs the renderer that an element provides styling information which can be inherited by its children.
 * This should only be applied to nodes which do not paint something themselves but rather delegate painting
 * to one of their children or properties (e.g. a group or use element).
 */
public interface HasContext {

    // Todo: Make this something that knows how to mutate the RenderContext itself
    // Todo: Make the current StyleContext an implementation of this.
    @Nullable
    PaintContext paintContext();

    @Nullable
    AttributeFontSpec fontSpec();

    @Nullable
    FontRenderContext fontRenderContext();
}
