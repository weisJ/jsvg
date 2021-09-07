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
import java.awt.geom.GeneralPath;
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

    static void renderGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font,
            @NotNull RenderContext context, @NotNull Graphics2D g) {
        MeasureContext measure = context.measureContext();

        // Use pathLengthFactor of 1 as pathLength isn't allowed on text
        // Otherwise we would have to do expensive computations for the length of a text outline.
        Stroke stroke = context.stroke(1f);

        // Todo: Gradients for text are complicated. If possible computing the complete text bounds
        // should be avoided. Rather pass the current transform along to the gradient.
        Rectangle2D bounds = new Rectangle();

        Shape glyphRun = layoutGlyphRun(segment, cursor, font, measure);
        ShapeRenderer.renderShape(context, g, glyphRun, bounds, stroke, true, true);
    }

    static Shape layoutGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font,
            @NotNull MeasureContext measure) {
        FontRenderContext frc = measure.fontRenderContext();
        float letterSpacing = frc.letterSpacing != null
                ? frc.letterSpacing.resolveLength(measure)
                : 0f;

        GeneralPath glyphPath = new GeneralPath();

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
