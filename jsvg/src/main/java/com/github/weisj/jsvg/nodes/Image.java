/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.UIFuture;
import com.github.weisj.jsvg.parser.ValueUIFuture;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.RenderContext;

@ElementCategories({Category.Graphic, Category.GraphicsReferencing})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive}
)
public final class Image extends RenderableSVGNode {
    private static final Logger LOGGER = Logger.getLogger(Image.class.getName());

    public static final String TAG = "image";
    private Length x;
    private Length y;
    private Length width;
    private Length height;
    private PreserveAspectRatio preserveAspectRatio;

    private UIFuture<BufferedImage> imgResource;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return imgResource != null && super.isVisible(context);
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", 0);
        y = attributeNode.getLength("y", 0);
        width = attributeNode.getLength("width", Length.UNSPECIFIED);
        height = attributeNode.getLength("height", Length.UNSPECIFIED);
        preserveAspectRatio = PreserveAspectRatio.parse(
                attributeNode.getValue("preserveAspectRatio"), attributeNode.parser());
        String url = attributeNode.parser().parseUrl(attributeNode.getHref());
        if (url != null) {
            try {
                imgResource = attributeNode.resourceLoader().loadImage(new URL(url));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
                imgResource = null; // Image didn't load. Maybe we should show a missing image instead.
            }
        }
    }

    private @Nullable BufferedImage fetchImage(@NotNull RenderContext context) {
        if (imgResource == null) return null;
        if (imgResource instanceof ValueUIFuture) return imgResource.get();
        if (!imgResource.checkIfReady(context.targetComponent())) return null;
        BufferedImage img = imgResource.get();
        if (img != null) imgResource = new ValueUIFuture<>(img);
        return img;
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Graphics2D g) {
        BufferedImage img = fetchImage(context);
        if (img == null) return;

        MeasureContext measure = context.measureContext();
        int imgWidth = img.getWidth(context.targetComponent());
        int imgHeight = img.getWidth(context.targetComponent());
        if (imgWidth == 0 || imgHeight == 0) return;

        float viewWidth = width.orElseIfUnspecified(imgWidth).resolveWidth(measure);
        float viewHeight = height.orElseIfUnspecified(imgHeight).resolveHeight(measure);
        ViewBox viewBox = new ViewBox(imgWidth, imgHeight);

        g.translate(x.resolveWidth(measure), y.resolveHeight(measure));
        AffineTransform imgTransform = preserveAspectRatio.computeViewPortTransform(
                new FloatSize(viewWidth, viewHeight), viewBox);
        Object imageAntialiasing = g.getRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING);
        if (imageAntialiasing == SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_OFF) {
            g.drawImage(img, imgTransform, context.targetComponent());
        } else {
            g.transform(imgTransform);
            Rectangle imgRect = new Rectangle(0, 0, imgWidth, imgHeight);
            // Painting using a TexturePaint allows for anti-aliased edges with a nontrivial transform
            GraphicsUtil.safelySetPaint(g, new TexturePaint(img, imgRect));
            g.fill(imgRect);
        }
    }
}
