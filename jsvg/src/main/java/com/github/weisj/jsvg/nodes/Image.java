/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.UIFuture;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.ValueUIFuture;
import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.parser.resources.impl.MissingImageResource;
import com.github.weisj.jsvg.renderer.Output;
import com.github.weisj.jsvg.renderer.impl.RenderContext;

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
    private Overflow overflow;

    private UIFuture<RenderableResource> imgResource;


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
        x = attributeNode.getLength("x", PercentageDimension.WIDTH, 0);
        y = attributeNode.getLength("y", PercentageDimension.HEIGHT, 0);
        width = attributeNode.getLength("width", PercentageDimension.WIDTH, Length.UNSPECIFIED);
        height = attributeNode.getLength("height", PercentageDimension.HEIGHT, Length.UNSPECIFIED);
        preserveAspectRatio = PreserveAspectRatio.parse(
                attributeNode.getValue("preserveAspectRatio"), attributeNode.parser());
        overflow = attributeNode.getEnum("overflow", Overflow.Hidden);

        String url = attributeNode.parser().parseUrl(attributeNode.getHref());
        if (url == null) return;
        URI resolvedUri = attributeNode.resolveResourceURI(url);
        if (resolvedUri == null) return;

        try {
            imgResource = attributeNode.resourceLoader().loadImage(attributeNode.document(), resolvedUri);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            imgResource = null;
        }
    }

    private @Nullable RenderableResource fetchImage(@NotNull RenderContext context) {
        if (imgResource == null) return null;
        if (imgResource instanceof ValueUIFuture) return imgResource.get();
        if (!imgResource.checkIfReady(context.platformSupport())) return null;
        RenderableResource resource = imgResource.get();
        if (resource != null) imgResource = new ValueUIFuture<>(resource);
        return resource;
    }

    @Override
    public void render(@NotNull RenderContext context, @NotNull Output output) {
        RenderableResource resource = fetchImage(context);
        if (resource == null) {
            resource = new MissingImageResource();
        }

        MeasureContext measure = context.measureContext();
        FloatSize intrinsicResourceSize = resource.intrinsicSize(context);
        float resourceWidth = intrinsicResourceSize.width;
        float resourceHeight = intrinsicResourceSize.height;
        if (resourceWidth == 0 || resourceHeight == 0) return;

        float viewWidth = width.orElseIfUnspecified(resourceWidth).resolve(measure);
        float viewHeight = height.orElseIfUnspecified(resourceHeight).resolve(measure);

        output.translate(x.resolve(measure), y.resolve(measure));

        if (overflow.establishesClip()) output.applyClip(new ViewBox(viewWidth, viewHeight));
        // Todo: Vector Effects

        AffineTransform imgTransform = preserveAspectRatio.computeViewPortTransform(
                new FloatSize(viewWidth, viewHeight),
                new ViewBox(resourceWidth, resourceHeight));

        resource.render(output, context, imgTransform);
    }
}
