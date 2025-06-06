/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ShapeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import com.github.weisj.jsvg.util.ShapeUtil;

final class GlyphRenderer {
    private static final boolean DEBUG = false;

    private GlyphRenderer() {}

    static void prepareGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor, @NotNull SVGFont font,
            @NotNull RenderContext context, @NotNull TextOutput textOutput) {
        GlyphRun glyphRun = layoutGlyphRun(segment, cursor, font, context, textOutput);

        cursor.completeGlyphRunMetrics.union(glyphRun.metrics());
        segment.currentGlyphRun = glyphRun;
        segment.currentRenderContext = context;
    }

    static void renderGlyphRun(@NotNull Output output, @NotNull PaintOrder paintOrder,
            @NotNull Set<VectorEffect> vectorEffects, @NotNull StringTextSegment segment) {
        RenderContext context = segment.currentRenderContext;
        assert context != null;

        GlyphRun glyphRun = segment.currentGlyphRun;
        assert glyphRun != null;

        AbstractGlyphRun.Metrics metrics = glyphRun.metrics();

        // Use pathLengthFactor of 1 as pathLength isn't allowed on text
        // Otherwise we would have to do expensive computations for the length of a text outline.
        Stroke stroke = context.stroke(1f);

        // Todo: Vector-Effects
        ShapeRenderer.renderWithPaintOrder(output, true, paintOrder,
                new ShapeRenderer.ShapePaintContext(context, vectorEffects, stroke, null),
                new ShapeRenderer.PaintShape(glyphRun.shape(), metrics.paintBounds),
                null);

        // Experimental Emoji rendering
        SVGFont font = RenderContextAccessor.instance().font(context);
        Output.SafeState safeState = output.safeState();
        for (AbstractGlyphRun.PaintableEmoji emoji : glyphRun.emojis()) {
            emoji.render(output, font);
            safeState.restore();
        }

        // Invalidate the glyphRun. Avoids holding onto the RenderContext, which may reference a JComponent.
        segment.currentRenderContext = null;
        segment.currentGlyphRun = null;
    }

    static @NotNull GlyphRun layoutGlyphRun(@NotNull StringTextSegment segment, @NotNull GlyphCursor cursor,
            @NotNull SVGFont font, @NotNull RenderContext context,
            @NotNull TextOutput textOutput) {
        MeasureContext measure = context.measureContext();
        FontRenderContext fontRenderContext = RenderContextAccessor.instance().fontRenderContext(context);
        float letterSpacing = fontRenderContext.letterSpacing().resolve(measure);

        Path2D glyphPath = new Path2D.Float();
        Point2D.Float layoutStart = cursor.currentLocation(measure);
        List<AbstractGlyphRun.PaintableEmoji> emojis = null;

        boolean isLastSegment = segment.isLastSegmentInParent();
        boolean shouldSkipLastSpacing = isLastSegment && cursor.advancement().shouldSkipLastSpacing();

        textOutput.glyphRunBreak();

        List<String> codepoints = segment.codepoints();
        for (int i = 0, count = codepoints.size(); i < count; i++) {
            String codepoint = codepoints.get(i);

            boolean lastCodepoint = i == count - 1;

            Glyph glyph = font.codepointGlyph(codepoint);

            if (i > 0 && !cursor.isCurrentGlyphAutoLayout()) {
                textOutput.glyphRunBreak();
            }

            AffineTransform glyphTransform = cursor.advance(measure, glyph);

            boolean skipSpacing = lastCodepoint && shouldSkipLastSpacing;
            if (!skipSpacing) cursor.advanceSpacing(letterSpacing);

            if (!cursor.shouldRenderCurrentGlyph()) continue;

            // If null no more characters should be processed.
            if (glyphTransform == null) break;

            if (glyph.isRendered()) {
                float baselineOffset = computeBaselineOffset(font, fontRenderContext);
                glyphTransform.translate(0, -baselineOffset);

                if (glyph instanceof EmojiGlyph) {
                    if (emojis == null) {
                        emojis = new ArrayList<>();
                    }
                    emojis.add(new AbstractGlyphRun.PaintableEmoji(
                            (EmojiGlyph) glyph,
                            new AffineTransform(glyphTransform)));
                } else {
                    Shape glyphOutline = glyph.glyphOutline();
                    Shape renderPath = ShapeUtil.transformShape(glyphOutline, glyphTransform);
                    glyphPath.append(renderPath, false);

                    if (DEBUG) {
                        glyphPath.append(
                                ShapeUtil.transformShape(glyphOutline.getBounds2D(), glyphTransform), false);
                    }
                }
            }

            textOutput.codepoint(codepoint, glyphTransform, context);
        }

        Rectangle2D paintBounds = glyphPath.getBounds2D();
        Point2D.Float layoutEnd = cursor.currentLocation(measure);
        Rectangle2D layoutBounds = new Rectangle2D.Double(
                layoutStart.x, paintBounds.getY(),
                layoutEnd.x - layoutStart.x, paintBounds.getHeight());
        return new GlyphRun(glyphPath,
                new AbstractGlyphRun.Metrics(paintBounds, layoutBounds),
                emojis != null ? emojis : Collections.emptyList());
    }

    private static float computeBaselineOffset(@NotNull SVGFont font, @NotNull FontRenderContext fontRenderContext) {
        switch (fontRenderContext.dominantBaseline()) {
            default:
            case Auto:
                // TODO: If text is in vertical mode this should be 'central'
            case Alphabetic:
                return font.romanBaseline();
            case Hanging:
                return font.hangingBaseline();
            case Central:
                return font.centerBaseline();
            case Middle:
                return font.middleBaseline();
            case Mathematical:
                return font.mathematicalBaseline();
            case Ideographic:
            case TextAfterEdge:
            case TextBottom:
                return font.textUnderBaseline();
            case TextBeforeEdge:
            case TextTop:
                return font.textOverBaseline();
        }
    }
}
