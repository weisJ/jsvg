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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.renderer.FontRenderContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.ShapeRenderer;

final class GlyphRenderer {
    private static final boolean DEBUG = false;

    private GlyphRenderer() {}

    // Static to avoid using member variables.
    static void renderGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font,
            @NotNull RenderContext context, @NotNull Graphics2D g) {
        MeasureContext measure = context.measureContext();
        FontRenderContext frc = context.fontRenderContext();
        float letterSpacing = frc.letterSpacing != null
                ? frc.letterSpacing.resolveLength(measure)
                : 0f;
        // Todo: Gradients for text are complicated. If possible computing the complete text bounds
        // should be avoided. Rather pass the current transform along to the gradient.
        Rectangle2D bounds = new Rectangle();

        char[] codepointsBuffer = new char[1];
        for (char codepoint : segment.codepoints()) {
            codepointsBuffer[0] = codepoint;
            GlyphVector glyphVector = font.unicodeGlyphVector(g, codepointsBuffer);
            GlyphMetrics gm = glyphVector.getGlyphMetrics(0);
            AffineTransform glyphTransform = cursor.advance(codepoint, measure, gm, letterSpacing);
            if (glyphTransform == null) break;
            if (gm.isWhitespace()) continue; // Todo: this doesn't reliably detect whitespace. Move into cache
            // Todo: Cache the individual Glyph shapes and metrics in the font
            Shape glyph = glyphVector.getGlyphOutline(0);
            Shape renderPath = glyphTransform.createTransformedShape(glyph);
            ShapeRenderer.renderShape(context, g, renderPath, bounds, true, true);

            if (DEBUG) paintDebugGlyph(g, glyphTransform, glyph);
        }
    }

    private static void paintDebugGlyph(@NotNull Graphics2D g, @NotNull AffineTransform glyphTransform,
            @NotNull Shape glyph) {
        g.setColor(Color.MAGENTA);
        g.setStroke(new BasicStroke(0.5f));
        g.draw(glyphTransform.createTransformedShape(glyph.getBounds()));
    }
}
