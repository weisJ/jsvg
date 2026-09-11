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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds.CoversWholeRegion;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;

abstract class AbstractCompositeFilterPrimitive extends AbstractFilterPrimitive {
    private FilterChannelKey inputChannel2;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        inputChannel2 = attributeNode.getFilterChannelKey("in2", DefaultFilterChannel.LastResult);
    }

    protected abstract @NotNull Composite composite();

    protected boolean affectsTransparentBlack() {
        return false;
    }

    private @NotNull Channel sourceChannel(@NotNull FilterPrimitiveBase impl, @NotNull FilterContext filterContext) {
        return impl.inputChannel(filterContext);
    }

    private @NotNull Channel destinationChannel(@NotNull FilterPrimitiveBase impl,
            @NotNull FilterContext filterContext) {
        return impl.channel(inputChannel2, filterContext);
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds in = impl().layoutInput(filterLayoutContext);
        LayoutBounds in2 = filterLayoutContext.resultChannels().get(inputChannel2);
        LayoutBounds bounds = in.union(in2);
        Rectangle2D region = filterLayoutContext.filterPrimitiveRegion(impl(), bounds.region());
        bounds = bounds.withRegion(region,
                affectsTransparentBlack() ? CoversWholeRegion.YES : CoversWholeRegion.NO);
        impl().saveLayoutResult(bounds, filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        FilterPrimitiveBase impl = impl();
        Channel source = sourceChannel(impl, filterContext);
        Channel destination = destinationChannel(impl, filterContext);
        Composite composite = computeComposite(filterContext);
        if (source instanceof ConstantColorChannel && destination instanceof ConstantColorChannel) {
            impl.saveResult(((ConstantColorChannel) destination).composite((ConstantColorChannel) source, composite),
                    filterContext);
            return;
        }
        BufferedImage dst = destination.toBufferedImageNonAliased(context);

        Graphics2D imgGraphics = GraphicsUtil.createGraphics(dst);
        imgGraphics.setComposite(composite);
        source.paint(imgGraphics, context);
        imgGraphics.dispose();

        impl.saveResult(new ImageProducerChannel(dst.getSource()), filterContext);
    }

    private @NotNull Composite computeComposite(@NotNull FilterContext filterContext) {
        return CompositeModeComposite.inColorSpace(composite(), colorInterpolation(filterContext));
    }
}
