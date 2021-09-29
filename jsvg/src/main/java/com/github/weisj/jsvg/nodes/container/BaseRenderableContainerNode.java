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
package com.github.weisj.jsvg.nodes.container;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.PaintContext;
import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class BaseRenderableContainerNode<E> extends BaseContainerNode<E> implements Renderable, HasContext {

    private boolean isVisible;
    private PaintContext paintContext;
    private FontRenderContext fontRenderContext;
    private AttributeFontSpec fontSpec;
    private FillRule fillRule;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        isVisible = parseIsVisible(attributeNode);
        paintContext = PaintContext.parse(attributeNode);
        fontRenderContext = FontRenderContext.parse(attributeNode);
        fontSpec = FontParser.parseFontSpec(attributeNode);
        fillRule = parseFillRule(attributeNode);
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible;
    }

    @Override
    public final @NotNull PaintContext paintContext() {
        return paintContext;
    }

    @Override
    public final @NotNull FontRenderContext fontRenderContext() {
        return fontRenderContext;
    }

    @Override
    public final @NotNull AttributeFontSpec fontSpec() {
        return fontSpec;
    }

    @Override
    public final @NotNull FillRule fillRule() {
        return fillRule;
    }
}
