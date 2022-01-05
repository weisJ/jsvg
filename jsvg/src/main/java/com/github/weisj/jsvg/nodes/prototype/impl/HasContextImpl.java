/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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
package com.github.weisj.jsvg.nodes.prototype.impl;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.PaintContext;

public final class HasContextImpl implements HasContext {

    private final @NotNull PaintContext paintContext;
    private final @NotNull FontRenderContext fontRenderContext;
    private final @NotNull AttributeFontSpec fontSpec;
    private final @NotNull FillRule fillRule;

    private HasContextImpl(@NotNull PaintContext paintContext, @NotNull FontRenderContext fontRenderContext,
            @NotNull AttributeFontSpec fontSpec, @NotNull FillRule fillRule) {
        this.paintContext = paintContext;
        this.fontRenderContext = fontRenderContext;
        this.fontSpec = fontSpec;
        this.fillRule = fillRule;
    }

    public static @NotNull HasContext parse(@NotNull AttributeNode attributeNode) {
        return new HasContextImpl(
                PaintContext.parse(attributeNode),
                FontRenderContext.parse(attributeNode),
                FontParser.parseFontSpec(attributeNode),
                FillRule.parse(attributeNode));
    }

    @Override
    public @NotNull FillRule fillRule() {
        return fillRule;
    }

    @Override
    public @NotNull Mutator<MeasurableFontSpec> fontSpec() {
        return fontSpec;
    }

    @Override
    public @NotNull FontRenderContext fontRenderContext() {
        return fontRenderContext;
    }

    @Override
    public @NotNull Mutator<PaintContext> paintContext() {
        return paintContext;
    }
}
