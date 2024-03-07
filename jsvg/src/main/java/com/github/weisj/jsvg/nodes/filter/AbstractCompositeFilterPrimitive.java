/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.RenderContext;

abstract class AbstractCompositeFilterPrimitive extends AbstractFilterPrimitive {
    private FilterChannelKey inputChannel2;

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        inputChannel2 = attributeNode.getFilterChannelKey("in2", DefaultFilterChannel.LastResult);
    }

    protected abstract @NotNull Composite composite();

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
        impl().saveLayoutResult(in.union(in2), filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        FilterPrimitiveBase impl = impl();
        BufferedImage dst = destinationChannel(impl, filterContext).toBufferedImageNonAliased(context);

        Image other = context.platformSupport().createImage(sourceChannel(impl, filterContext).producer());
        Graphics2D imgGraphics = GraphicsUtil.createGraphics(dst);
        imgGraphics.setComposite(computeComposite(filterContext, impl));
        imgGraphics.drawImage(other, null, context.platformSupport().imageObserver());
        imgGraphics.dispose();

        impl.saveResult(new ImageProducerChannel(dst.getSource()), filterContext);
    }

    private @NotNull Composite computeComposite(@NotNull FilterContext filterContext,
            @NotNull FilterPrimitiveBase impl) {
        Composite comp = composite();
        if (destinationChannel(impl, filterContext).isDefaultChannel(DefaultFilterChannel.SourceAlpha)) {
            comp = new AlphaChannelComposite(comp, true);
        } else if (sourceChannel(impl, filterContext).isDefaultChannel(DefaultFilterChannel.SourceAlpha)) {
            comp = new AlphaChannelComposite(comp, false);
        }
        return comp;
    }

    private static class AlphaChannelComposite implements Composite {
        private final @NotNull Composite composite;
        private final boolean useSrc;

        private AlphaChannelComposite(@NotNull Composite composite, boolean useSrc) {
            this.composite = composite;
            this.useSrc = useSrc;
        }

        @Override
        public @NotNull CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel,
                RenderingHints hints) {
            return new AlphaChannelCompositeContext(composite.createContext(srcColorModel, dstColorModel, hints),
                    useSrc);
        }

        private static class AlphaChannelCompositeContext implements CompositeContext {
            private final @NotNull CompositeContext context;
            private final boolean useSrc;

            private AlphaChannelCompositeContext(@NotNull CompositeContext context, boolean useSrc) {
                this.context = context;
                this.useSrc = useSrc;
            }

            @Override
            public void dispose() {
                context.dispose();
            }

            @Override
            public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                Raster effectiveSrc = useSrc ? src : dstIn;
                if (effectiveSrc.getNumBands() != 4 || dstOut.getNumBands() != 4) {
                    throw new IllegalArgumentException("Expected 4 bands");
                }
                WritableRaster effectiveDstOut = dstOut;
                if (effectiveSrc == effectiveDstOut) {
                    effectiveDstOut = dstOut.createCompatibleWritableRaster();
                }

                context.compose(src, dstIn, effectiveDstOut);

                int[] srcData = new int[1];
                for (int x = 0; x < dstOut.getWidth(); x++) {
                    for (int y = 0; y < dstOut.getHeight(); y++) {
                        effectiveSrc.getDataElements(x, y, srcData);
                        int a = effectiveDstOut.getSample(x, y, 3);
                        srcData[0] = (srcData[0] & 0x00FFFFFF) | (a << 24);
                        dstOut.setDataElements(x, y, srcData);
                    }
                }
            }
        }
    }
}
