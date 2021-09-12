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
package com.github.weisj.jsvg.nodes.mesh;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.NotImplemented;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories(Category.Gradient)
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {MeshRow.class /* <animate>, <animateTransform>, <script>, <set> */}
)
public final class MeshGradient extends ContainerNode implements SVGPaint {
    public static final String TAG = "meshgradient";

    private Length x;
    private Length y;

    private @NotImplemented UnitType gradientUnits;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        x = attributeNode.getLength("x", 0);
        y = attributeNode.getLength("y", 0);
        gradientUnits = attributeNode.getEnum("gradientUnits", UnitType.ObjectBoundingBox);
        MeshBuilder.buildMesh(this);
        // Todo: type bilinear|bicubic
        // Todo: href template
        // Todo: transform
    }

    public void renderMesh(@NotNull MeasureContext measure, @NotNull Graphics2D g) {
        Graphics2D meshGraphics = (Graphics2D) g.create();
        meshGraphics.translate(x.resolveWidth(measure), y.resolveHeight(measure));

        meshGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (SVGNode child : children()) {
            MeshRow row = (MeshRow) child;
            for (SVGNode node : row.children()) {
                MeshPatch patch = (MeshPatch) node;
                patch.renderPath(g);
            }
        }
        meshGraphics.dispose();
    }

    @Override
    public void fillShape(@NotNull Graphics2D g, @NotNull MeasureContext measure, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Shape clip = g.getClip();
        g.setClip(shape);
        renderMesh(measure, g);
        g.setClip(clip);
    }

    @Override
    public void drawShape(@NotNull Graphics2D g, @NotNull MeasureContext measure, @NotNull Shape shape,
            @Nullable Rectangle2D bounds) {
        Shape clip = g.getClip();
        g.setClip(g.getStroke().createStrokedShape(shape));
        renderMesh(measure, g);
        g.setClip(clip);
    }
}
