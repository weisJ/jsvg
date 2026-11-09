/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.SpreadMethod;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.AttributeNode.ElementRelation;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;

@SuppressWarnings("java:S119") // Generic name Self is intentional
abstract class AbstractGradient<Self extends AbstractGradient<Self>> extends ContainerNode implements SVGPaint {
    protected TransformValue gradientTransform;
    protected UnitType gradientUnits;
    protected SpreadMethod spreadMethod;

    private @NotNull Stop[] stops;
    private Percentage[] offsets;

    private Color[] tmpColors;
    private float[] tmpFractions;

    public final Percentage[] offsets() {
        return offsets;
    }

    private @NotNull Stop[] stops() {
        return stops;
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


        List<Stop> stopsList = childrenOfType(Stop.class);
        if (stopsList.isEmpty() && template != null) {
            stops = template.stops();
            offsets = template.offsets();
        } else {
            parseStops(stopsList);
        }

        // noinspection unchecked
        Self selfTemplate = getClass().isInstance(template) ? (Self) template : null;
        buildGradient(attributeNode, selfTemplate);
        children().clear();
    }

    private void parseStops(@NotNull List<Stop> stopsList) {
        List<Stop> newStopsList = new ArrayList<>();
        List<Percentage> offsetsList = new ArrayList<>();

        float previousOffset = 0;
        for (Stop stop : stopsList) {
            // Preserve document order and clamp decreasing offsets to the preceding offset.
            float stopOffset = Math.max(previousOffset, Math.min(1, stop.offset().value()));
            previousOffset = stopOffset;

            boolean isFirstStop = offsetsList.isEmpty();
            boolean effectiveStop = isFirstStop
                    || stopOffset > offsetsList.get(offsetsList.size() - 1).value()
                    || stop.hasAnimatedColor()
                    || newStopsList.get(newStopsList.size() - 1).hasAnimatedColor()
                    || !stop.color().equals(newStopsList.get(newStopsList.size() - 1).color());

            if (isFirstStop && stopOffset != 0) {
                // If the first stop is not at 0, we need to extend its color to the beginning.
                offsetsList.add(Percentage.ZERO);
                newStopsList.add(stop);
            }

            if (effectiveStop) {
                offsetsList.add(new Percentage(stopOffset));
                newStopsList.add(stop);
            }
        }

        if (!offsetsList.isEmpty() && offsetsList.get(offsetsList.size() - 1).value() != 1f) {
            // If the last stop is not at 1, we need to extend its color to the end.
            offsetsList.add(Percentage.ONE);
            newStopsList.add(newStopsList.get(newStopsList.size() - 1));
        }

        stops = newStopsList.toArray(new Stop[0]);
        offsets = offsetsList.toArray(new Percentage[0]);
        makeStrictlyIncreasing(offsets);
    }

    private static void makeStrictlyIncreasing(@NotNull Percentage @NotNull [] offsets) {
        // Round up equal values to the next float value.
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i].value() <= offsets[i - 1].value()) {
                offsets[i] = new Percentage(Math.nextUp(offsets[i - 1].value()));
            }
        }

        // Values have possibly exceeded 1 now. Clamp them back down.
        for (int i = offsets.length - 1; i >= 0; i--) {
            if (offsets[i].value() >= 1) {
                offsets[i] = Percentage.ONE;
            }
        }

        // Now round values down as in step one but from top to bottom.
        // Note: As there are 126 x 2^23 + 1 floats between 0 and 1 and we don't expect input array to be
        // that large we can safely assume that now all values are distinct (as there will be a gap with
        // enough space to shift all offsets to).
        for (int i = offsets.length - 2; i >= 0; i--) {
            if (offsets[i].value() >= offsets[i + 1].value()) {
                offsets[i] = new Percentage(Math.nextDown(offsets[i + 1].value()));
            }
        }
    }

    private @Nullable AbstractGradient<?> parseTemplate(@NotNull AttributeNode attributeNode) {
        AbstractGradient<?> template = attributeNode.getElementByHref(AbstractGradient.class, attributeNode.getHref(),
                ElementRelation.TEMPLATE);
        return template != this ? template : null;
    }

    protected abstract void buildGradient(@NotNull AttributeNode attributeNode, @Nullable Self template);

    @Override
    public void fillShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        output.setPaint(() -> paintForBounds(context, b));
        output.fillShape(shape);
    }

    @Override
    public void drawShape(@NotNull Output output, @NotNull RenderContext context, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Rectangle2D b = bounds != null ? bounds : shape.getBounds2D();
        output.setPaint(() -> paintForBounds(context, b));
        output.drawShape(shape);
    }

    private @NotNull Paint paintForBounds(@NotNull RenderContext context, @NotNull Rectangle2D bounds) {
        Color[] gradColors = resolveColors(context.measureContext());
        if (gradColors.length == 0) return new Color(0, true);
        if (allColorsEqual(gradColors)) return gradColors[0];
        return gradientForBounds(gradientUnits.deriveMeasure(context.measureContext()), bounds, offsets(), gradColors);
    }

    private @NotNull Color[] resolveColors(@NotNull MeasureContext context) {
        Color[] colors = tmpColors != null && tmpColors.length == stops.length
                ? tmpColors
                : new Color[stops.length];
        for (int i = 0; i < stops.length; i++) {
            colors[i] = stops[i].color(context);
        }
        tmpColors = colors;
        return colors;
    }

    private static boolean allColorsEqual(@NotNull Color[] colors) {
        if (colors.length <= 1) return true;
        Color first = colors[0];
        for (int i = 1; i < colors.length; i++) {
            if (!first.equals(colors[i])) return false;
        }
        return true;
    }

    protected float[] offsetsToFractions(Percentage[] gradOffsets) {
        // NOTE: We need to recompute the fractions if a stop is animated.
        if (tmpFractions == null || tmpFractions.length != gradOffsets.length) {
            tmpFractions = new float[gradOffsets.length];
            for (int i = 0; i < tmpFractions.length; i++) {
                tmpFractions[i] = gradOffsets[i].value();
            }
        }
        return tmpFractions;
    }

    protected abstract @NotNull Paint gradientForBounds(@NotNull MeasureContext measure, @NotNull Rectangle2D bounds,
            Percentage[] gradOffsets, @NotNull Color[] gradColors);

    protected final @NotNull AffineTransform computeViewTransform(@NotNull MeasureContext measure,
            @NotNull Rectangle2D bounds) {
        AffineTransform viewTransform = gradientUnits.viewTransform(bounds);
        if (gradientTransform != null) viewTransform.concatenate(gradientTransform.get(measure));
        return viewTransform;
    }
}
