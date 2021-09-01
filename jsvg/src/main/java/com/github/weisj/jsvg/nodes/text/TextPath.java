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
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.text.GlyphRenderMethod;
import com.github.weisj.jsvg.attributes.text.Side;
import com.github.weisj.jsvg.attributes.text.Spacing;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.PathUtil;

@ElementCategories({Category.Graphic, Category.TextContent, Category.TextContentChild})
@PermittedContent(
    categories = {Category.Descriptive},
    anyOf = {Anchor.class, TextSpan.class /* <altGlyph>, <animate>, <animateColor>, <set>, <tref> */},
    charData = true
)
public final class TextPath extends TextContainer {
    public static final String TAG = "textpath";

    private HasShape pathShape;

    private Spacing spacing;
    private GlyphRenderMethod renderMethod;
    private Side side;

    private Length startOffset;

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        renderMethod = attributeNode.getEnum("method", GlyphRenderMethod.Align);
        side = attributeNode.getEnum("side", Side.Left);
        spacing = attributeNode.getEnum("spacing", Spacing.Auto);
        // Todo: Needs to be resolved w.r.t to the paths coordinate system
        startOffset = attributeNode.getLength("startOffset", 0);

        String pathData = attributeNode.getValue("path");
        if (pathData != null) {
            pathShape = PathUtil.parseFromPathData(pathData, FillRule.EvenOdd);
        } else {
            String href = attributeNode.getHref();
            pathShape = attributeNode.getElementByHref(HasShape.class, Category.BasicShape, href);
        }
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        renderSegment(new PathGlyphCursor(createPathIterator(context), new AffineTransform()), context, g);
    }

    private @NotNull PathIterator createPathIterator(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        Shape path = pathShape.computeShape(measureContext);
        // For fonts this is a good enough approximation
        float flatness = measureContext.ex() / 2f;
        return path.getPathIterator(null, flatness);
    }

    @Override
    protected GlyphCursor createLocalCursor(@NotNull RenderContext context, @NotNull GlyphCursor current) {
        // Todo: Incorporate TextSpans. their x/dx properties move along the path, though y/dy doesn't
        return new PathGlyphCursor(current, createPathIterator(context));
    }

    @Override
    protected void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local) {
        current.x = local.x;
        current.y = local.y;
    }
}
