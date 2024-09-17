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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.animation.value.AnimatedFloatList;
import com.github.weisj.jsvg.animation.value.AnimatedLength;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.animation.value.AnimatedPercentage;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.MetaSVGNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.SeparatorMode;

@ElementCategories(Category.Animation)
@PermittedContent(categories = {Category.Descriptive})
public final class Animate extends MetaSVGNode {
    public static final String TAG = "animate";

    private String[] values;
    private @Nullable Track track;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    public static @Nullable String attributeName(@NotNull AttributeNode attributeNode) {
        return attributeNode.getValue("attributeName");
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        String from = attributeNode.getValue("from");
        String to = attributeNode.getValue("to");
        String by = attributeNode.getValue("by");

        if (to != null) by = null;

        if (attributeNode.getValue("values") != null) {
            values = attributeNode.getStringList("values", SeparatorMode.SEMICOLON_ONLY);
        } else {
            if (from != null && to != null) {
                values = new String[] {from, to};
            } else if (from != null && by != null) {
                // TODO: from -> from + to, needs intermediate representation for ValueList
            } else if (by != null) {
                // TODO: initial -> initial + by, needs intermediate representation for ValueList
            } else if (to != null) {
                // TODO: initial -> to, needs intermediate representation for ValueList
            }
        }

        track = Track.parse(attributeNode);
    }

    public @Nullable Track track() {
        return track;
    }

    public @Nullable AnimatedLength animatedLength(@NotNull LengthValue initial,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;

        Length[] lengths = new Length[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = attributeNode.parser().parseLength(values[i], null);
            if (lengths[i] == null) return null;
        }
        return new AnimatedLength(track, initial, lengths);
    }

    public @Nullable AnimatedFloatList animatedFloatList(float @NotNull [] initial,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;

        float[][] lists = new float[values.length][];
        for (int i = 0; i < values.length; i++) {
            lists[i] = attributeNode.parser().parseFloatList(values[i]);
        }
        return new AnimatedFloatList(track, initial, lists);
    }

    public @Nullable AnimatedPercentage animatedPercentage(@NotNull Percentage initial,
            @NotNull AttributeNode attributeNode) {
        if (track == null) return null;
        float[] percentages = new float[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            Percentage p = attributeNode.parser().parsePercentage(this.values[i], null);
            if (p == null) return null;
            percentages[i] = p.value();
        }
        return new AnimatedPercentage(track, initial.value(), percentages);
    }

    public @Nullable AnimatedPaint animatedPaint(@NotNull SVGPaint initial, @NotNull AttributeNode attributeNode) {
        if (track == null) return null;
        SVGPaint[] paints = new SVGPaint[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            SVGPaint p = attributeNode.parsePaint(this.values[i]);
            if (p == null) return null;
            paints[i] = p;
        }
        return new AnimatedPaint(track, initial, paints);
    }
}
