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

import static com.github.weisj.jsvg.geometry.util.GeometryUtil.lerp;

import java.awt.*;
import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.mesh.CoonPatch;
import com.github.weisj.jsvg.geometry.mesh.CoonValues;
import com.github.weisj.jsvg.geometry.mesh.Subdivided;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.Stop;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories({ /* None */})
@PermittedContent(
    categories = Category.Descriptive,
    anyOf = {Stop.class /* <script> */}
)
public final class MeshPatch extends ContainerNode {
    public static final String TAG = "meshpatch";
    private static final int MAX_DEPTH = 10;

    Color north;
    Color east;
    Color south;
    Color west;
    final @NotNull CoonPatch coonPatch = CoonPatch.createUninitialized();

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public void renderPath(@NotNull Graphics2D g) {
        AffineTransform at = g.getTransform();
        float scaleX = (float) GeometryUtil.scaleYOfTransform(at);
        float scaleY = (float) GeometryUtil.scaleYOfTransform(at);
        int depth = Math.max(
                Math.max(coonPatch.north.estimateStepCount(scaleX, scaleY),
                        coonPatch.east.estimateStepCount(scaleX, scaleY)),
                Math.max(coonPatch.south.estimateStepCount(scaleX, scaleY),
                        coonPatch.west.estimateStepCount(scaleX, scaleY)));
        renderPath(g, coonPatch, scaleX, scaleY, Math.min(MAX_DEPTH, depth));
    }

    private void renderPath(@NotNull Graphics2D g, @NotNull CoonPatch patch, float scaleX, float scaleY, int depth) {
        CoonValues weights = patch.coonValues;
        // Check if we have reached the limit of discernible colors. This happens if our color weights
        // spectrum allows for less that approximately (1/255)^3, which is our "relative color-depth".
        if (depth == 0 || GeometryUtil.distanceSquared(weights.north, weights.south, scaleX, scaleY)
                * GeometryUtil.distanceSquared(weights.east, weights.west, scaleX, scaleY) < 0.000001) {
            float u = (weights.north.x + weights.east.x + weights.south.x + weights.west.x) / 4;
            float v = (weights.north.y + weights.east.y + weights.south.y + weights.west.y) / 4;
            g.setColor(bilinearInterpolation(u, v));
            Shape s = patch.toShape();
            g.fill(s.getBounds2D());
        } else {
            Subdivided<CoonPatch> patchSubdivided = patch.subdivide();
            renderPath(g, patchSubdivided.northWest, scaleX, scaleY, depth - 1);
            renderPath(g, patchSubdivided.northEast, scaleX, scaleY, depth - 1);
            renderPath(g, patchSubdivided.southEast, scaleX, scaleY, depth - 1);
            renderPath(g, patchSubdivided.southWest, scaleX, scaleY, depth - 1);
        }
    }

    private @NotNull Color bilinearInterpolation(float dx, float dy) {
        float r = lerp(dy, lerp(dx, north.getRed(), east.getRed()), lerp(dx, west.getRed(), south.getRed()));
        float g = lerp(dy, lerp(dx, north.getGreen(), east.getGreen()), lerp(dx, west.getGreen(), south.getGreen()));
        float b = lerp(dy, lerp(dx, north.getBlue(), east.getBlue()), lerp(dx, west.getBlue(), south.getBlue()));
        float a = lerp(dy, lerp(dx, north.getAlpha(), east.getAlpha()), lerp(dx, west.getAlpha(), south.getAlpha()));
        return new Color(clampColor(r), clampColor(g), clampColor(b), clampColor(a));
    }

    private int clampColor(float v) {
        return Math.max(Math.min(255, (int) v), 0);
    }
}
