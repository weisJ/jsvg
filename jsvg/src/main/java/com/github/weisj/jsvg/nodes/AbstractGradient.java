/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.nodes;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.attributes.SpreadMethod;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.RenderContext;

@SuppressWarnings("java:S119") // Generic name Self is intentional
abstract class AbstractGradient<Self extends AbstractGradient<Self>> extends ContainerNode implements SVGPaint {
    protected AffineTransform gradientTransform;
    protected UnitType gradientUnits;
    protected SpreadMethod spreadMethod;

    private @NotNull Color[] colors;
    private @Percentage float[] offsets;

    public final @Percentage float[] offsets() {
        return offsets;
    }

    public final @NotNull Color[] colors() {
        return colors;
    }

    @Override
    public final void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        AbstractGradient<?> template = parseTemplate(attributeNode);

        gradientUnits = attributeNode.getEnum("gradientUnits",
                template != null ? template.gradientUnits : UnitType.ObjectBoundingBox);
        spreadMethod = attributeNode.getEnum("spreadMethod",
                template != null ? template.spreadMethod : SpreadMethod.Pad);

        gradientTransform = attributeNode.parseTransform("gradientTransform");
        if (gradientTransform == null && template != null)
            gradientTransform = template.gradientTransform;


        List<Stop> stops = childrenOfType(Stop.class);
        if (stops.isEmpty() && template != null) {
            colors = template.colors();
            offsets = template.offsets();
        } else {
            parseStops(stops);
        }

        // noinspection unchecked
        Self selfTemplate = getClass().isInstance(template) ? (Self) template : null;
        buildGradient(attributeNode, selfTemplate);
        children().clear();
    }

    private void parseStops(@NotNull List<Stop> stops) {
        stops.sort((s1, s2) -> Float.compare(s1.offset(), s2.offset()));
        colors = new Color[stops.size()];
        offsets = new float[stops.size()];

        boolean realGradient = false;
        for (int i = 0; i < offsets.length; i++) {
            Stop stop = stops.get(i);
            // Clamp the offset
            float stopOffset = Math.max(0, Math.min(1, stop.offset()));
            Color stopColor = stop.color();

            if (i > 0) {
                // Keep track whether the provided colors and offsets are actually different.
                realGradient = realGradient
                        || stopOffset > stops.get(i - 1).offset()
                        || !stopColor.equals(colors[i - 1]);

                if (stopOffset <= offsets[i - 1]) {
                    // The awt gradient implementations really don't like it if
                    // two offsets are equal. Hence, we use the next possible float value instead as it will produce
                    // the same effect as if the equal values were used.
                    stopOffset = Math.nextAfter(offsets[i - 1], Double.MAX_VALUE);
                }
            }

            offsets[i] = stopOffset;
            colors[i] = stopColor;
        }

        // Rebalance if nextAfter pushed us out of range.
        if (offsets[offsets.length - 1] > 1f) {
            float diff = offsets[offsets.length - 1] - 1f;
            offsets[offsets.length - 1] = 1f;
            int i = offsets.length - 2;
            while (i >= 0 && offsets[i] >= offsets[i + 1]) {
                offsets[i] -= diff;
            }
        }

        if (!realGradient && colors.length > 0) {
            // If all stops are equal, we can just use the first color.
            // This never gets passed to the radial gradient hence we don't need to provide a second stop.
            colors = new Color[] {colors[0]};
            offsets = new float[] {0f};
        } else {
            // To avoid copying the arrays, we just make sure that the first and last stop are 0 and 1
            // respectively here instead of in the gradient implementation.
            int offsetLength = offsets.length;
            int off = 0;

            boolean fixFirst = false;
            boolean fixLast = false;

            if (offsets[0] != 0f) {
                // first stop is not equal to zero, fix this condition
                fixFirst = true;
                offsetLength++;
                off++;
            }
            if (offsets[offsets.length - 1] != 1f) {
                // last stop is not equal to one, fix this condition
                fixLast = true;
                offsetLength++;
            }

            float[] oldOffsets = offsets;
            Color[] oldColors = colors;

            offsets = new float[offsetLength];
            colors = new Color[offsetLength];

            System.arraycopy(oldOffsets, 0, offsets, off, oldOffsets.length);
            System.arraycopy(oldColors, 0, colors, off, oldColors.length);

            if (fixFirst) {
                offsets[0] = 0f;
                colors[0] = oldColors[0];
            }
            if (fixLast) {
                offsets[offsetLength - 1] = 1f;
                colors[offsetLength - 1] = oldColors[oldColors.length - 1];
            }
        }
    }

    private @Nullable AbstractGradient<?> parseTemplate(@NotNull AttributeNode attributeNode) {
        AbstractGradient<?> template = attributeNode.getElementByHref(AbstractGradient.class, attributeNode.getHref());
        return template != this ? template : null;
    }

    protected abstract void buildGradient(@NotNull AttributeNode attributeNode, @Nullable Self template);

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        output.setPaint(() -> paintForBounds(context.measureContext(), b));
        output.fillShape(shape);
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        output.setPaint(() -> paintForBounds(context.measureContext(), b));
        output.drawShape(shape);
    }

    private @NotNull Paint paintForBounds(@NotNull MeasureContext context, @NotNull Rectangle2D bounds) {
        Color[] gradColors = colors();
        if (gradColors.length == 0) return PaintParser.DEFAULT_COLOR;
        if (gradColors.length == 1) return gradColors[0];
        return gradientForBounds(gradientUnits.deriveMeasure(context), bounds, offsets(), gradColors);
    }

    protected abstract @NotNull Paint gradientForBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds,
            @Percentage float[] gradOffsets, @NotNull Color[] gradColors);

    protected final @NotNull AffineTransform computeViewTransform(@NotNull Rectangle2D bounds) {
        AffineTransform viewTransform = gradientUnits.viewTransform(bounds);
        if (gradientTransform != null) viewTransform.concatenate(gradientTransform);
        return viewTransform;
    }
}
