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
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.text.GlyphRenderMethod;
import com.github.weisj.jsvg.attributes.text.Side;
import com.github.weisj.jsvg.attributes.text.Spacing;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.geometry.util.ReversePathIterator;
import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.NotImplemented;
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
    private static final boolean DEBUG = false;

    private SVGShape pathShape;

    private @NotImplemented Spacing spacing;
    private @NotImplemented GlyphRenderMethod renderMethod;
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
            HasShape shaped =
                    attributeNode.getElementByHref(HasShape.class, Category.Shape /* BasicShape or Path */, href);
            if (shaped != null) {
                pathShape = shaped.shape();
            }
        }
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return pathShape != null && super.isVisible(context);
    }

    @Override
    public @NotNull Shape shape(@NotNull RenderContext context, boolean validate) {
        GeneralPath textPath = new GeneralPath();
        appendTextShape(createCursor(context), textPath, context);
        return textPath;
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        renderSegment(createCursor(context), context, g);
    }

    private @NotNull PathGlyphCursor createCursor(@NotNull RenderContext context) {
        return new PathGlyphCursor(
                createPathIterator(context),
                startOffset.resolveLength(context.measureContext()));
    }

    @Override
    public void renderSegment(@NotNull GlyphCursor cursor, @NotNull RenderContext context, @NotNull Graphics2D g) {
        super.renderSegment(cursor, context, g);
        if (DEBUG) paintDebugPath(context, g);
    }

    private void paintDebugPath(@NotNull RenderContext context, @NotNull Graphics2D g) {
        PathIterator pathIterator = createPathIterator(context);
        float startX = 0;
        float startY = 0;
        float curX = 0;
        float curY = 0;
        g.setStroke(new BasicStroke(0.5f));
        float[] cord = new float[2];
        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(cord)) {
                case PathIterator.SEG_LINETO:
                    g.setColor(Color.MAGENTA);
                    g.draw(new Line2D.Float(curX, curY, cord[0], cord[1]));
                    g.setColor(Color.RED);
                    g.fillRect((int) curX - 2, (int) curY - 2, 4, 4);
                    g.fillRect((int) cord[0] - 2, (int) cord[1] - 2, 4, 4);
                    curX = cord[0];
                    curY = cord[1];
                    break;
                case PathIterator.SEG_MOVETO:
                    curX = cord[0];
                    curY = cord[1];
                    startX = curX;
                    startY = curY;
                    break;
                case PathIterator.SEG_CLOSE:
                    g.setColor(Color.MAGENTA);
                    g.draw(new Line2D.Float(curX, curY, startX, startY));
                    g.setColor(Color.RED);
                    g.fillRect((int) curX - 2, (int) curY - 2, 4, 4);
                    g.fillRect((int) startX - 2, (int) startY - 2, 4, 4);
                    curX = startX;
                    curY = startY;
                    break;
            }
            pathIterator.next();
        }
    }

    private @NotNull PathIterator createPathIterator(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        Shape path = pathShape.shape(context);
        // For fonts this is a good enough approximation
        float flatness = 0.1f * measureContext.ex();
        switch (side) {
            case Left:
                return path.getPathIterator(null, flatness);
            case Right:
                return new ReversePathIterator(path.getPathIterator(null, flatness));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    protected GlyphCursor createLocalCursor(@NotNull RenderContext context, @NotNull GlyphCursor current) {
        return new PathGlyphCursor(current,
                startOffset.resolveLength(context.measureContext()),
                createPathIterator(context));
    }

    @Override
    protected void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local) {
        current.updateFrom(local);
    }
}
