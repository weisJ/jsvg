/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedPercentage;
import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.path.BezierPathCommand;
import com.github.weisj.jsvg.geometry.path.PathParser;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.paint.impl.RGBColor;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.util.ColorUtil;
import com.github.weisj.jsvg.view.FloatSize;

@ElementCategories(Category.Gradient)
@PermittedContent(
    anyOf = {Animate.class, Set.class}
)
public final class Stop extends AbstractSVGNode {
    public static final String TAG = "stop";
    private static final MeasureContext INITIAL_MEASURE_CONTEXT =
            MeasureContext.createInitial(new FloatSize(0, 0), 0, 0, AnimationState.NO_ANIMATION);

    private @NotNull ColorValue color = RGBColor.DEFAULT;
    private @NotNull PercentageValue opacity = Percentage.ONE;
    private Percentage offset;
    private @Nullable BezierPathCommand path;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public @NotNull Color color(@NotNull MeasureContext context) {
        Color resolved = color.get(context);
        float combinedAlpha = resolved.getAlpha() / 255f * opacity.get(context);
        return ColorUtil.withAlpha(resolved, combinedAlpha);
    }

    /** The unanimated color, retained for mesh gradients and API compatibility. */
    public @NotNull Color color() {
        return color(INITIAL_MEASURE_CONTEXT);
    }

    public boolean hasAnimatedColor() {
        return color instanceof AnimatedColor || opacity instanceof AnimatedPercentage;
    }

    public @NotNull Percentage offset() {
        return offset;
    }

    public @Nullable BezierPathCommand bezierCommand() {
        return path;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        color = attributeNode.getColorValue("stop-color", Color.BLACK, Animatable.YES);
        opacity = attributeNode.getPercentage("stop-opacity", Percentage.ONE,
                Inherited.NO, Animatable.YES);
        offset = attributeNode.getPercentage("offset", Percentage.ZERO);
        String pathData = attributeNode.getValue("path");
        path = pathData != null ? new PathParser(pathData).parseMeshCommand() : null;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "color=" + color +
                ", offset=" + offset +
                '}';
    }
}
