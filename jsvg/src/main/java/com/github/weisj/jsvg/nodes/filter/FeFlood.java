/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jannis Weis
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

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.paint.AwtSVGPaint;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.impl.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.impl.GraphicsUtil;
import com.github.weisj.jsvg.renderer.impl.RenderContext;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class FeFlood extends AbstractFilterPrimitive {
    public static final String TAG = "feflood";

    private SVGPaint floodColor;
    private PercentageValue floodOpacity;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        floodColor = attributeNode.getPaint("flood-color", new AwtSVGPaint(Color.BLACK),
                Inherited.NO, Animatable.YES);
        floodOpacity = attributeNode.getPercentage("flood-opacity", Percentage.ONE,
                Inherited.NO, Animatable.YES);
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds layoutBounds = new LayoutBounds(
                filterLayoutContext.filterPrimitiveRegion(context.measureContext(), this),
                new FloatInsets());
        impl().saveLayoutResult(layoutBounds, filterLayoutContext);
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        // Todo: We should be able to optimize this heavily by implementing a custom image producer.
        // and even then filters like feBlend could benefit from knowing that this is a constant color.
        Filter.FilterInfo info = filterContext.info();
        BufferedImage img = new BufferedImage(info.imageWidth, info.imageHeight, BufferedImage.TYPE_INT_ARGB);
        float opacity = floodOpacity.get(context.measureContext());
        if (opacity != 0) {
            Graphics2D graphics = GraphicsUtil.createGraphics(img);
            graphics.setComposite(AlphaComposite.Src.derive(opacity));
            Rectangle rect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
            floodColor.fillShape(new Graphics2DOutput(graphics), context, rect, rect);
            graphics.dispose();
        }
        impl().saveResult(new ImageProducerChannel(img.getSource()), filterContext);
    }

}
