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
package com.github.weisj.jsvg.nodes;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import com.github.weisj.jsvg.nodes.container.CommonInnerViewContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;

@ElementCategories({Category.Container, Category.Structural})
@PermittedContent(
    categories = {Category.Animation, Category.Descriptive, Category.Shape, Category.Structural, Category.Gradient},
    /*
     * <altGlyphDef>, <color-profile>, <cursor>, <filter>, <font>, <font-face>, <foreignObject>, <mask>,
     * <script>, <switch>
     */
    anyOf = {Anchor.class, ClipPath.class, Image.class, Marker.class, Pattern.class, Style.class, Text.class,
            View.class}
)
public final class SVG extends CommonInnerViewContainer {
    public static final String TAG = "svg";

    private static final float FALLBACK_WIDTH = 300;
    private static final float FALLBACK_HEIGHT = 150;

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    public @NotNull FloatSize sizeForTopLevel(float em, float ex) {
        MeasureContext topLevelContext =
                MeasureContext.createInitial(new FloatSize(FALLBACK_WIDTH, FALLBACK_HEIGHT), em, ex);
        return new FloatSize(
                width.orElseIfUnspecified(viewBox != null ? viewBox.width : FALLBACK_WIDTH)
                        .resolveWidth(topLevelContext),
                height.orElseIfUnspecified(viewBox != null ? viewBox.height : FALLBACK_HEIGHT)
                        .resolveHeight(topLevelContext));
    }
}
