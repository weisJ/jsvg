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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.*;
import com.github.weisj.jsvg.nodes.prototype.impl.HasGeometryContextImpl;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

public abstract class RenderableSVGNode extends AbstractSVGNode
        implements Renderable, Transformable, HasClip, HasFilter {

    private boolean isVisible;
    private HasGeometryContext geometryContext;

    @Override
    public @Nullable ClipPath clipPath() {
        return geometryContext.clipPath();
    }

    @Override
    public @Nullable Mask mask() {
        return geometryContext.mask();
    }

    @Override
    public @Nullable Filter filter() {
        return geometryContext.filter();
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return isVisible && (context.rawOpacity() > 0);
    }

    @Override
    public @Nullable AffineTransform transform() {
        return geometryContext.transform();
    }

    @Override
    public @NotNull Point2D transformOrigin(@NotNull MeasureContext context) {
        return geometryContext.transformOrigin(context);
    }

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        isVisible = parseIsVisible(attributeNode);
        geometryContext = HasGeometryContextImpl.parse(attributeNode);
    }
}
