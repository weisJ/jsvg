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
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.NodeRenderer;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.ShapeRenderer;

abstract class LinearTextContainer extends TextContainer {
    protected Length[] x;
    protected Length[] y;
    protected Length[] dx;
    protected Length[] dy;
    protected float[] rotate;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLengthList("x");
        y = attributeNode.getLengthList("y");
        dx = attributeNode.getLengthList("dx");
        dy = attributeNode.getLengthList("dy");
        rotate = attributeNode.getFloatList("rotate");
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        Cursor initialCursor = new Cursor(
                getXCursorForIndex(context.measureContext(), 0, 0),
                getYCursorForIndex(context.measureContext(), 0, 0));
        renderSegment(initialCursor, context, g);
    }

    @Override
    public void renderSegment(@NotNull Cursor cursor, @NotNull RenderContext context, @NotNull Graphics2D g) {
        // Todo: Determine whether it is more efficient to build the path as a whole
        // and do a single paint call instead.

        // Todo: textLength should be taken into consideration.
        SVGFont font = context.font(null);
        int localGlyphOffset = cursor.glyphOffset;
        cursor.glyphOffset = 0;
        for (TextSegment segment : children()) {
            RenderContext currentContext = context;
            if (segment instanceof Renderable) {
                if (!((Renderable) segment).isVisible(context)) continue;
                currentContext = NodeRenderer.setupRenderContext(segment, context);
            }
            if (segment instanceof StringTextSegment) {
                renderGlyphRun((StringTextSegment) segment, cursor, font, currentContext, g);
            } else if (segment instanceof RenderableSegment) {
                ((RenderableSegment) segment).renderSegment(cursor, currentContext, g);
            } else {
                throw new IllegalStateException("Can't render segment " + segment);
            }
        }
        cursor.glyphOffset = localGlyphOffset;
    }

    private void renderGlyphRun(@NotNull StringTextSegment segment, @NotNull Cursor cursor, @NotNull SVGFont font,
            @NotNull RenderContext context, @NotNull Graphics2D g) {
        MeasureContext measure = context.measureContext();
        FontRenderContext frc = context.fontRenderContext();
        float letterSpacing = frc.letterSpacing != null
                ? frc.letterSpacing.resolveLength(measure)
                : 0f;
        char[] codepoints = segment.codepoints();
        GlyphVector glyphVector = font.unicodeGlyphVector(g, codepoints);
        // Todo: Gradients for text are complicated. If possible computing the complete text bounds
        // should be avoided. Rather pass the current transform along to the gradient.
        Rectangle2D bounds = new Rectangle();

        // Todo: Skip unnecessary whitespace
        for (int i = 0, glyphCount = glyphVector.getNumGlyphs(); i < glyphCount; i++) {
            advanceCursor(cursor, measure);
            // Todo: Cache the individual Glyph shapes and metrics in the font
            GlyphMetrics gm = glyphVector.getGlyphMetrics(i);
            Shape glyph = glyphVector.getGlyphOutline(i);
            Point2D glyphPosition = glyphVector.getGlyphPosition(i);
            cursor.transform.translate(-glyphPosition.getX(), -glyphPosition.getY());
            Shape renderPath = cursor.transform.createTransformedShape(glyph);
            ShapeRenderer.renderShape(context, g, renderPath, bounds, true, true);

            cursor.x += gm.getAdvanceX() + letterSpacing;
            cursor.y += gm.getAdvanceY() + letterSpacing;
        }
    }

    private void advanceCursor(@NotNull Cursor cursor, @NotNull MeasureContext measure) {
        // The positions are specified for the whole recursive content of a span.
        // We need to account for any eventual text which occurred before.
        cursor.x = getXCursorForIndex(measure, cursor.x, cursor.glyphOffset);
        cursor.y = getYCursorForIndex(measure, cursor.y, cursor.glyphOffset);
        cursor.transform.setToTranslation(cursor.x, cursor.y);
        if (rotate != null && cursor.glyphOffset < rotate.length) {
            cursor.transform.rotate(rotate[cursor.glyphOffset]);
        }
        cursor.glyphOffset++;
    }

    private float getXCursorForIndex(@NotNull MeasureContext measure, float current, int index) {
        if (x != null && index < x.length) {
            current = x[index].resolveWidth(measure);
        } else if (dx != null && index < dx.length) {
            current += dx[index].resolveWidth(measure);
        }
        return current;
    }

    private float getYCursorForIndex(@NotNull MeasureContext measure, float current, int index) {
        if (y != null && index < y.length) {
            current = y[index].resolveHeight(measure);
        } else if (dy != null && index < dy.length) {
            current += dy[index].resolveHeight(measure);
        }
        return current;
    }

}
