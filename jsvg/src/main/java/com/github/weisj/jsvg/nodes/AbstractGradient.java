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
package com.github.weisj.jsvg.nodes;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.Percentage;
import com.github.weisj.jsvg.attributes.SpreadMethod;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.nodes.container.ContainerNode;

abstract class AbstractGradient<Self extends AbstractGradient<Self>> extends ContainerNode implements SVGPaint {
    protected AffineTransform gradientTransform;
    protected UnitType gradientUnits;
    protected SpreadMethod spreadMethod;

    private @NotNull Color[] colors;
    private @Percentage float[] offsets;

    public @Percentage float[] offsets() {
        return offsets;
    }

    public @NotNull Color[] colors() {
        return colors;
    }

    @Override
    public final void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        Self template = parseTemplate(attributeNode);

        gradientUnits = attributeNode.getEnum("gradientUnits",
                template != null ? template.gradientUnits : UnitType.ObjectBoundingBox);
        spreadMethod = attributeNode.getEnum("spreadMethod",
                template != null ? template.spreadMethod : SpreadMethod.Pad);

        gradientTransform = attributeNode.parseTransform("gradientTransform");
        if (gradientTransform == null && template != null)
            gradientTransform = template.gradientTransform;


        List<Stop> stops = childrenOfType(Stop.class);
        if (stops.size() == 0 && template != null) {
            colors = template.colors();
            offsets = template.offsets();
        } else {
            parseStops(stops);
        }

        buildGradient(attributeNode, template);
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
            }

            if (i > 0 && stopOffset <= offsets[i - 1]) {
                // The awt gradient implementations really don't like it if
                // two offsets are equal. Hence we use the next possible float value instead as it will produce
                // the same effect as if the equal values were used.
                stopOffset = Math.nextAfter(stopOffset, Double.MAX_VALUE);
            }

            offsets[i] = stopOffset;
            colors[i] = stopColor;
        }

        if (!realGradient && colors.length > 0) {
            colors = new Color[] {colors[0]};
            offsets = new float[] {0f};
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Self parseTemplate(@NotNull AttributeNode attributeNode) {
        return (Self) attributeNode.getElementByHref(getClass(), attributeNode.getHref());
    }

    protected abstract void buildGradient(@NotNull AttributeNode attributeNode, @Nullable Self template);

    @Override
    public final @NotNull Paint paintForBounds(@NotNull Rectangle2D bounds) {
        Color[] gradColors = colors();
        if (gradColors.length == 0) return PaintParser.DEFAULT_COLOR;
        if (gradColors.length == 1) return gradColors[0];
        return gradientForBounds(bounds, offsets(), gradColors);
    }

    protected abstract @NotNull Paint gradientForBounds(@NotNull Rectangle2D bounds,
            @Percentage float[] gradOffsets, @NotNull Color[] gradColors);

    protected @NotNull AffineTransform computeViewTransform(@NotNull Rectangle2D bounds) {
        AffineTransform viewTransform = new AffineTransform();

        if (gradientUnits == UnitType.ObjectBoundingBox) {
            viewTransform.setToTranslation(bounds.getX(), bounds.getY());
            viewTransform.scale(bounds.getWidth(), bounds.getHeight());
        }
        if (gradientTransform != null) viewTransform.concatenate(gradientTransform);
        return viewTransform;
    }
}
