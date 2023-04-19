/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jannis Weis
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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.BlendMode;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeBlend extends AbstractFilterPrimitive {
    public static final String TAG = "feblend";

    private Object inputChannel2;
    private BlendModeComposite composite;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        inputChannel2 = attributeNode.getValue("in2");
        if (inputChannel2 == null) inputChannel2 = DefaultFilterChannel.LastResult;
        BlendMode blendMode = attributeNode.getEnum("mode", BlendMode.Normal);
        composite = new BlendModeComposite(blendMode);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context,
            @NotNull FilterContext filterContext) {
        FilterPrimitiveBase impl = impl();
        Channel in = impl.inputChannel(filterContext);
        Channel in2 = impl.channel(inputChannel2, filterContext);
        BufferedImage dst = in.toBufferedImageNonAliased(context);

        Graphics2D imgGraphics = (Graphics2D) dst.getGraphics();
        imgGraphics.setComposite(composite);
        imgGraphics.drawImage(context.createImage(in2.producer()), null, context.targetComponent());
        imgGraphics.dispose();

        impl.saveResult(new ImageProducerChannel(dst.getSource()), filterContext);
    }
}
