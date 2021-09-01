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
package com.github.weisj.jsvg.nodes.text;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.text.LengthAdjust;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.BaseRenderableContainerNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.CharData;

abstract class TextContainer extends BaseRenderableContainerNode<TextSegment> implements TextSegment.RenderableSegment {
    private final List<TextSegment> segments = new ArrayList<>();

    protected AttributeFontSpec fontSpec;
    protected LengthAdjust lengthAdjust;
    protected Length textLength;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        fontSpec = FontParser.parseFontSpec(attributeNode);
        lengthAdjust = attributeNode.getEnum("lengthAdjust", LengthAdjust.Spacing);
        textLength = attributeNode.getLength("textLength");
    }

    @Override
    public void renderSegment(@NotNull Cursor cursor, @NotNull RenderContext context, @NotNull Graphics2D g) {

    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof TextSegment;
    }

    @Override
    protected void doAdd(@NotNull SVGNode node) {
        segments.add((TextSegment) node);
    }

    @Override
    public final void addContent(char[] content, int start, int length) {
        boolean hasPrecedingSpace = !segments.isEmpty() && lastChar() == ' ';
        char[] data = CharData.getAddressableCharacters(content, start, length, !hasPrecedingSpace);
        System.out.println(Arrays.toString(data));
        if (data.length == 0) return;
        segments.add(new StringTextSegment(data));
    }

    @Override
    public List<@NotNull ? extends TextSegment> children() {
        return segments;
    }

    @Override
    public char lastChar() {
        return segments.get(segments.size() - 1).lastChar();
    }
}
