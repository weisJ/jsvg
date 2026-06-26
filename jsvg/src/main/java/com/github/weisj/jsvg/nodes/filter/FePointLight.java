/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;

@ElementCategories(Category.None)
@PermittedContent
public class FePointLight extends AbstractSVGNode implements LightSource {
    public static final String TAG = "fepointlight";

    protected double lightX;
    protected double lightY;
    protected double lightZ;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        lightX = attributeNode.getFloat("x", 0);
        lightY = attributeNode.getFloat("y", 0);
        lightZ = attributeNode.getFloat("z", 0);
    }

    @Override
    public @NotNull Light lightAt(double x, double y, double z) {
        return computePointLight(x, y, z, 1);
    }

    protected final @NotNull Light computePointLight(double x, double y, double z, double intensity) {
        double dx = lightX - x;
        double dy = lightY - y;
        double dz = lightZ - z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) return new Light(0, 0, 0, 0);
        return new Light(dx / length, dy / length, dz / length, intensity);
    }
}
