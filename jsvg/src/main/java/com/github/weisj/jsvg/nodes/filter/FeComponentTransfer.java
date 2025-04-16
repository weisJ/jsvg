/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import java.awt.image.*;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.filter.TransferFunctionType;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.impl.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(
    categories = {Category.TransferFunctionElement},
    anyOf = {Animate.class, Set.class}
)
public class FeComponentTransfer extends ContainerNode implements FilterPrimitive {
    public static final String TAG = "fecomponenttransfer";

    private FilterPrimitiveBase filterPrimitiveBase;
    private ByteLookupTable sRGBlookupTable;
    private ByteLookupTable linearRGBlookupTable;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        filterPrimitiveBase = new FilterPrimitiveBase(attributeNode);

        List<TransferFunctionElement> nodes = childrenOfType(TransferFunctionElement.class);
        boolean redValid = false;
        boolean greenValid = false;
        boolean blueValid = false;
        boolean alphaValid = false;

        byte[][] tables = {
                TransferFunctionElement.IDENTITY_LOOKUP_TABLE,
                TransferFunctionElement.IDENTITY_LOOKUP_TABLE,
                TransferFunctionElement.IDENTITY_LOOKUP_TABLE,
                TransferFunctionElement.IDENTITY_LOOKUP_TABLE
        };

        for (TransferFunctionElement node : nodes) {
            switch (node.channel()) {
                case Red:
                    redValid = node.type() != TransferFunctionType.Identity;
                    tables[0] = node.lookupTable();
                    break;
                case Green:
                    greenValid = node.type() != TransferFunctionType.Identity;
                    tables[1] = node.lookupTable();
                    break;
                case Blue:
                    blueValid = node.type() != TransferFunctionType.Identity;
                    tables[2] = node.lookupTable();
                    break;
                case Alpha:
                    alphaValid = node.type() != TransferFunctionType.Identity;
                    tables[3] = node.lookupTable();
                    break;
            }
        }

        if (redValid || greenValid || blueValid || alphaValid) {
            sRGBlookupTable = new ByteLookupTable(0, tables);
        }
        children().clear();
    }

    @Override
    public @NotNull Length x() {
        return filterPrimitiveBase.x;
    }

    @Override
    public @NotNull Length y() {
        return filterPrimitiveBase.y;
    }

    @Override
    public @NotNull Length width() {
        return filterPrimitiveBase.width;
    }

    @Override
    public @NotNull Length height() {
        return filterPrimitiveBase.height;
    }

    @Override
    public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
        return filterPrimitiveBase.colorInterpolation(filterContext);
    }

    @Override
    public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
        LayoutBounds bounds = filterPrimitiveBase
                .layoutInput(filterLayoutContext)
                .withFlags(new LayoutBounds.ComputeFlags(true));
        filterPrimitiveBase.saveLayoutResult(bounds, filterLayoutContext);
    }

    private @Nullable LookupTable lookupTable(@NotNull FilterContext filterContext) {
        if (sRGBlookupTable == null) return null;
        if (filterPrimitiveBase.colorInterpolation(filterContext) != ColorInterpolation.LinearRGB) {
            return sRGBlookupTable;
        }
        if (linearRGBlookupTable == null) {
            byte[][] tables = sRGBlookupTable.getTable();
            for (int j = 0; j < tables.length; j++) {
                byte[] table = tables[j];
                if (table == TransferFunctionElement.IDENTITY_LOOKUP_TABLE) continue;
                byte[] lRGBtable = new byte[table.length];
                for (int i = 0; i < table.length; i++) {
                    lRGBtable[i] = (byte) ColorUtil.linearRGBtoSRGBBand(table[ColorUtil.sRGBtoLinearRGBBand(i)] & 0xff);
                }
                tables[j] = lRGBtable;
            }
            linearRGBlookupTable = new ByteLookupTable(0, tables);
        }
        return linearRGBlookupTable;
    }

    @Override
    public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
        LookupTable lookup = lookupTable(filterContext);
        if (lookup == null) {
            filterPrimitiveBase.noop(filterContext);
            return;
        }
        ImageFilter f = new BufferedImageFilter(new LookupOp(lookup, filterContext.renderingHints()));
        filterPrimitiveBase.saveResult(filterPrimitiveBase.inputChannel(filterContext).applyFilter(f), filterContext);
    }
}
