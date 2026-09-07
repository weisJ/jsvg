/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.filter;

import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.ParsedElement;

public final class FilterPrimitiveBase {

    final @NotNull Length x;
    final @NotNull Length y;
    final @NotNull Length width;
    final @NotNull Length height;

    private final @NotNull FilterChannelKey inputChannel;
    private final @NotNull FilterChannelKey resultChannel;
    private final ColorInterpolation colorInterpolation;

    public FilterPrimitiveBase(@NotNull AttributeNode attributeNode) {
        // The parent filter's attributes are prepared before its children, but its node is built after
        // them.
        ParsedElement parent = attributeNode.element().parent();
        UnitType primitiveUnits = parent != null
                ? parent.attributeNode().getEnum("primitiveUnits", UnitType.UserSpaceOnUse)
                : UnitType.UserSpaceOnUse;
        x = attributeNode.getLength("x", PercentageDimension.WIDTH, Length.UNSPECIFIED)
                .coercePercentageToCorrectUnit(primitiveUnits, PercentageDimension.WIDTH);
        y = attributeNode.getLength("y", PercentageDimension.HEIGHT, Length.UNSPECIFIED)
                .coercePercentageToCorrectUnit(primitiveUnits, PercentageDimension.HEIGHT);
        width = attributeNode.getLength("width", PercentageDimension.WIDTH, Length.UNSPECIFIED)
                .coercePercentageToCorrectUnit(primitiveUnits, PercentageDimension.WIDTH);
        height = attributeNode.getLength("height", PercentageDimension.HEIGHT, Length.UNSPECIFIED)
                .coercePercentageToCorrectUnit(primitiveUnits, PercentageDimension.HEIGHT);

        inputChannel = attributeNode.getFilterChannelKey("in", DefaultFilterChannel.LastResult);
        resultChannel = attributeNode.getFilterChannelKey("result", DefaultFilterChannel.LastResult);

        colorInterpolation = attributeNode.getEnum("color-interpolation-filters", ColorInterpolation.Inherit);
    }

    public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
        return filterContext.colorInterpolation(colorInterpolation);
    }

    public @NotNull Channel channel(@NotNull FilterChannelKey key, @NotNull FilterContext context) {
        return context.getChannel(key);
    }

    public @NotNull Channel inputChannel(@NotNull FilterContext context) {
        return channel(inputChannel, context);
    }

    public @NotNull LayoutBounds layoutInput(@NotNull FilterLayoutContext context) {
        return context.resultChannels().get(inputChannel);
    }

    public void noop(@NotNull FilterContext context) {
        saveResult(inputChannel(context), context);
    }

    public void saveLayoutResult(@NotNull LayoutBounds outputBounds, @NotNull FilterLayoutContext filterLayoutContext) {
        saveLayoutResult(outputBounds, outputBounds.region(), filterLayoutContext);
    }

    public void saveLayoutResult(@NotNull FilterLayoutContext filterLayoutContext) {
        // The default regions is the filters region.
        saveLayoutResult(filterLayoutContext.filterRegion(), filterLayoutContext);
    }

    public void saveLayoutResult(@NotNull Rectangle2D defaultRegion, @NotNull FilterLayoutContext filterLayoutContext) {
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(this, defaultRegion);
        saveLayoutResultImpl(new LayoutBounds(region, new FloatInsets(), region), filterLayoutContext);
    }

    public void saveLayoutResult(@NotNull LayoutBounds outputBounds, @NotNull Rectangle2D defaultRegion,
            @NotNull FilterLayoutContext filterLayoutContext) {
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(this, defaultRegion);
        LayoutBounds result = outputBounds.withRegion(region);
        saveLayoutResultImpl(result, filterLayoutContext);
    }

    private void saveLayoutResultImpl(@NotNull LayoutBounds bounds, @NotNull FilterLayoutContext context) {
        context.saveResult(this, bounds);
        saveResultImpl(bounds, context.resultChannels());
    }

    public void saveResult(@NotNull Channel output, @NotNull FilterContext filterContext) {
        saveResultImpl(output.clip(filterContext.region(this), filterContext), filterContext.resultChannels());
    }

    private <T> void saveResultImpl(@NotNull T value, @NotNull ChannelStorage<T> storage) {
        storage.addResult(resultChannel, value);
        if (resultChannel != DefaultFilterChannel.LastResult) {
            storage.addResult(DefaultFilterChannel.LastResult, value);
        }
    }
}
