/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
package com.github.weisj.jsvg.nodes.animation;

import java.awt.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.value.*;
import com.github.weisj.jsvg.attributes.paint.ColorValue;
import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.PredefinedPaints;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;

@ElementCategories(Category.Animation)
@PermittedContent(categories = {Category.Descriptive})
public final class Animate extends BaseAnimationNode {
    public static final String TAG = "animate";

    @Override
    public @NotNull String tagName() {
        return TAG;
    }


    public @Nullable AnimatedLength animatedLength(
            @NotNull LengthValue initial,
            PercentageDimension dimension,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;

        Length[] lengths = new Length[values.length];
        for (int i = 0; i < values.length; i++) {
            if (isPlaceholder(values[i])) {
                lengths[i] = Length.ZERO;
            } else {
                lengths[i] = attributeNode.parser().parseLength(values[i], null, dimension);
            }
            if (lengths[i] == null) return null;
        }
        return new AnimatedLength(track, initial, lengths);
    }

    public @Nullable AnimatedFloatList animatedFloatList(float @NotNull [] initial,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;

        float[][] lists = new float[values.length][];
        for (int i = 0; i < values.length; i++) {
            if (isPlaceholder(values[i])) {
                lists[i] = new float[0];
            } else {
                lists[i] = attributeNode.parser().parseFloatList(values[i]);
            }
        }
        return new AnimatedFloatList(track, initial, lists);
    }

    public @Nullable AnimatedPercentage animatedPercentage(@NotNull PercentageValue initial,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;
        float[] percentages = new float[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            if (isPlaceholder(values[i])) {
                percentages[i] = Percentage.ZERO.value();
            } else {
                Percentage p = attributeNode.parser().parsePercentage(this.values[i], null);
                if (p == null) return null;
                percentages[i] = p.value();
            }
        }
        return new AnimatedPercentage(track, initial, percentages, Percentage.ONE);
    }

    public @Nullable AnimatedPaint animatedPaint(@NotNull SVGPaint initial, @NotNull AttributeNode attributeNode) {
        if (track == null) return null;
        SVGPaint[] paints = new SVGPaint[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            if (isPlaceholder(values[i])) {
                paints[i] = PredefinedPaints.NONE;
            } else {
                SVGPaint p = attributeNode.parsePaint(this.values[i]);
                if (p == null) return null;
                paints[i] = p;
            }
        }
        return new AnimatedPaint(track, initial, paints);
    }

    public @Nullable AnimatedColor animatedColor(@NotNull Color initial, @NotNull AttributeNode attributeNode) {
        if (track == null) return null;
        ColorValue[] paints = new ColorValue[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            if (isPlaceholder(values[i])) {
                paints[i] = new ColorValue(PaintParser.DEFAULT_COLOR);
            } else {
                Color c = attributeNode.parser().paintParser().parseColor(this.values[i], attributeNode);
                if (c == null) return null;
                paints[i] = new ColorValue(c);
            }
        }
        return new AnimatedColor(track, new ColorValue(initial), paints);
    }
}
