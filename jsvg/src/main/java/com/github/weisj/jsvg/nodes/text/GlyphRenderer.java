/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.ShapeRenderer;

final class GlyphRenderer {
    private static final boolean DEBUG = false;

    private GlyphRenderer() {}

    static void prepareGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font,
            @NotNull RenderContext context) {
        MeasureContext measure = context.measureContext();

        Shape glyphRun = layoutGlyphRun(segment, cursor, font, measure, context.fontRenderContext());
        Rectangle2D bounds = glyphRun.getBounds2D();

        if (Length.isUnspecified((float) cursor.completeGlyphRunBounds.getX())) {
            cursor.completeGlyphRunBounds.setRect(bounds);
        } else {
            Rectangle2D.union(cursor.completeGlyphRunBounds, bounds, cursor.completeGlyphRunBounds);
        }

        segment.currentGlyphRun = glyphRun;
        segment.currentRenderContext = context;
    }

    static void renderGlyphRun(@NotNull PaintOrder paintOrder, @NotNull StringTextSegment segment,
            @NotNull Rectangle2D completeGlyphRunBounds,
            @NotNull Graphics2D g) {
        RenderContext context = segment.currentRenderContext;
        assert context != null;

        Shape glyphRun = segment.currentGlyphRun;
        assert glyphRun != null;

        // Use pathLengthFactor of 1 as pathLength isn't allowed on text
        // Otherwise we would have to do expensive computations for the length of a text outline.
        Stroke stroke = context.stroke(1f);

        for (PaintOrder.Phase phase : paintOrder.phases()) {
            switch (phase) {
                case FILL:
                    ShapeRenderer.renderShapeFill(context, g, glyphRun, completeGlyphRunBounds);
                    break;
                case STROKE:
                    ShapeRenderer.renderShapeStroke(context, g, glyphRun, completeGlyphRunBounds, stroke);
                    break;
                case MARKERS:
                    break;
            }
        }

        // Invalidate the glyphRun. Avoids holding onto the RenderContext, which may reference a JComponent.
        segment.currentRenderContext = null;
        segment.currentGlyphRun = null;
    }

    static Shape layoutGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font,
            @NotNull MeasureContext measure, @NotNull FontRenderContext fontRenderContext) {
        float letterSpacing = fontRenderContext.letterSpacing().resolveLength(measure);

        Path2D glyphPath = new Path2D.Float();

        for (char codepoint : segment.codepoints()) {
            Glyph glyph = font.codepointGlyph(codepoint);
            AffineTransform glyphTransform = cursor.advance(codepoint, measure, glyph, letterSpacing);
            // If null no more characters should be processed.
            if (glyphTransform == null) break;
            if (!glyph.isRendered()) continue;
            Shape glyphOutline = glyph.glyphOutline();
            Shape renderPath = glyphTransform.createTransformedShape(glyphOutline);
            glyphPath.append(renderPath, false);
            if (DEBUG) {
                glyphPath.append(glyphTransform.createTransformedShape(glyphOutline.getBounds2D()), false);
            }
        }

        return glyphPath;
    }
}
