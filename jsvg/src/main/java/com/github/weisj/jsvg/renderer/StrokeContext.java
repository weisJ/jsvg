/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.renderer;

import java.util.Arrays;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.stroke.LineCap;
import com.github.weisj.jsvg.attributes.stroke.LineJoin;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.parser.AttributeNode;

public final class StrokeContext {
    public final @Nullable LengthValue strokeWidth;
    public final @Nullable LineCap lineCap;
    public final @Nullable LineJoin lineJoin;
    public final float miterLimit;

    public final Length @Nullable [] dashPattern;
    public final @Nullable LengthValue dashOffset;

    public StrokeContext(@Nullable LengthValue strokeWidth, @Nullable LineCap lineCap, @Nullable LineJoin lineJoin,
            float miterLimit, @NotNull Length[] dashPattern, @Nullable LengthValue dashOffset) {
        this.strokeWidth = strokeWidth;
        this.lineCap = lineCap;
        this.lineJoin = lineJoin;
        this.miterLimit = miterLimit;
        this.dashPattern = validateDashPattern(dashPattern);
        this.dashOffset = dashOffset;
    }

    @Contract("null -> null")
    private static Length @Nullable [] validateDashPattern(Length @Nullable [] pattern) {
        if (pattern == null) return null;
        if (pattern.length == 0) return pattern;
        for (Length length : pattern) {
            if (length.raw() < 0) {
                // Dash length is negative. Bail
                return new Length[0];
            }
            if (!length.isZero()) return pattern;
        }
        // All values are zero. Bail.
        return new Length[0];
    }

    public @NotNull StrokeContext derive(@Nullable StrokeContext context) {
        if (context == null) return this;
        if (context.isTrivial()) return this;
        return new StrokeContext(
                LengthValue.derive(strokeWidth, context.strokeWidth),
                context.lineCap != null ? context.lineCap : lineCap,
                context.lineJoin != null ? context.lineJoin : lineJoin,
                Length.isSpecified(context.miterLimit) ? context.miterLimit : miterLimit,
                context.dashPattern != null ? context.dashPattern : dashPattern,
                context.dashOffset != null ? context.dashOffset : dashOffset);
    }

    public boolean isTrivial() {
        return strokeWidth == null
                && lineCap == null
                && lineJoin == null
                && Length.isUnspecified(miterLimit)
                && dashPattern == null
                && dashOffset == null;
    }

    public static @NotNull StrokeContext createDefault() {
        return new StrokeContext(Unit.RAW.valueOf(1), LineCap.Butt, LineJoin.Miter, 4f, new Length[0], Length.ZERO);
    }

    public static @NotNull StrokeContext parse(@NotNull AttributeNode attributeNode) {
        return new StrokeContext(
                attributeNode.getLength("stroke-width", PercentageDimension.LENGTH, Inherited.YES, Animatable.YES),
                attributeNode.getEnumNullable("stroke-linecap", LineCap.class),
                attributeNode.getEnumNullable("stroke-linejoin", LineJoin.class),
                attributeNode.getNonNegativeFloat("stroke-miterlimit", Length.UNSPECIFIED_RAW),
                attributeNode.getLengthList("stroke-dasharray", null, PercentageDimension.LENGTH),
                attributeNode.getLength("stroke-dashoffset", PercentageDimension.CUSTOM,
                        Inherited.YES, Animatable.YES));
    }

    @Override
    public String toString() {
        return "StrokeContext{" +
                "strokeWidth=" + strokeWidth +
                ", lineCap=" + lineCap +
                ", lineJoin=" + lineJoin +
                ", miterLimit=" + miterLimit +
                ", dashPattern=" + Arrays.toString(dashPattern) +
                ", dashOffset=" + dashOffset +
                '}';
    }
}
