/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;

final class LinearTextLayoutGroup implements TextLayoutGroup {
    private final TextContainer<?> parent;
    private final @NotNull List<@NotNull TextSegment> segments;
    private final LayoutGroupSegment asSegment;

    LinearTextLayoutGroup(@NotNull TextContainer<?> parent) {
        this(parent, new ArrayList<>());
    }

    LinearTextLayoutGroup(@NotNull TextContainer<?> parent, @NotNull List<@NotNull TextSegment> segments) {
        this.parent = parent;
        this.segments = segments;
        this.asSegment = new LayoutGroupSegment(parent, this);
    }

    public @NotNull TextSegment.RenderableSegment asSegment() {
        return asSegment;
    }

    public @NotNull List<@NotNull TextSegment> segments() {
        return segments;
    }

    @Override
    public @Nullable Length fixedLength() {
        if (parent.textLength.isSpecified()) return parent.textLength;
        return null;
    }

    private @NotNull GlyphCursor createCursor() {
        return new GlyphCursor(0, 0, new AffineTransform());
    }

    @Override
    public void renderText(@NotNull RenderContext context, @NotNull Output output) {
        GlyphCursor cursor = createCursor();
        TextOutput textOutput = output.textOutput();
        textOutput.beginText();
        asSegment().prepareSegmentForRendering(cursor, context, textOutput);

        double offset = textAnchorOffset(parent.textAnchor(context), cursor.completeGlyphRunMetrics);
        context.translate(output, -offset, 0);
        asSegment().renderSegmentWithoutLayout(cursor, context, output);
        context.translate(output, offset, 0);

        textOutput.endText();
    }

    @Override
    public @NotNull Shape glyphShape(@NotNull RenderContext context) {
        MutableGlyphRun glyphRun = new MutableGlyphRun();
        asSegment().appendTextShape(createCursor(), glyphRun, context);
        double offset = textAnchorOffset(parent.textAnchor(context), glyphRun.metrics());
        if (GeometryUtil.approximatelyEqual(offset, 0)) return glyphRun.shape();
        return glyphRun.shape().createTransformedShape(AffineTransform.getTranslateInstance(-offset, 0));
    }

    private double textAnchorOffset(@NotNull TextAnchor textAnchor, @NotNull AbstractGlyphRun.Metrics metrics) {
        switch (textAnchor) {
            case Start:
                return 0;
            case Middle:
                return metrics.layoutBounds.getWidth() / 2f;
            case End:
                return metrics.layoutBounds.getWidth();
            default:
                throw new IllegalStateException("Unexpected value: " + textAnchor);
        }
    }

}
