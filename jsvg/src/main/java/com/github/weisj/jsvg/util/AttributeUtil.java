/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.util;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.AttributeNode;

public final class AttributeUtil {
    private AttributeUtil() {}

    public static <T> @NotNull T notNullOrElse(@Nullable T value, @NotNull T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static @NotNull AxisPair parseAxisPair(
            @NotNull AttributeNode node,
            @NotNull String xAttr, @NotNull String yAttr,
            @NotNull Length fallback,
            Function<@NotNull LengthValue, @Nullable LengthValue> validator) {
        LengthValue initialRx = node.getLength(xAttr, PercentageDimension.WIDTH, Length.UNSPECIFIED, Animatable.NO);
        initialRx = validator.apply(initialRx);
        if (initialRx == null) initialRx = Length.UNSPECIFIED;

        LengthValue initialRy = node.getLength(yAttr, PercentageDimension.HEIGHT, initialRx, Animatable.NO);
        initialRy = validator.apply(initialRy);
        if (initialRy == null) initialRy = Length.UNSPECIFIED;

        LengthValue rx;
        LengthValue ry;

        if (initialRx == Length.UNSPECIFIED && initialRy == Length.UNSPECIFIED) {
            // Neither rx nor ry is specified.
            // First try if either is specified through animation.
            // Then fallback to the other if not.
            rx = notNullOrElse(
                    node.getAnimatedLength(xAttr, fallback, PercentageDimension.WIDTH), initialRx);
            ry = notNullOrElse(
                    node.getAnimatedLength(yAttr, fallback, PercentageDimension.HEIGHT), initialRy);
            if (rx == Length.UNSPECIFIED) {
                rx = ry;
            } else if (ry == Length.UNSPECIFIED) {
                ry = rx;
            }
        } else if (initialRx == Length.UNSPECIFIED) {
            // rx is unspecified, but ry is specified.
            // Use ry as value for rx.
            ry = notNullOrElse(
                    node.getAnimatedLength(yAttr, initialRy, PercentageDimension.HEIGHT), initialRy);
            rx = notNullOrElse(
                    node.getAnimatedLength(xAttr, ry, PercentageDimension.WIDTH), ry);
        } else if (initialRy == Length.UNSPECIFIED) {
            // ry is unspecified, but rx is specified.
            // Use rx as value for ry.
            rx = notNullOrElse(
                    node.getAnimatedLength(xAttr, initialRx, PercentageDimension.WIDTH), initialRx);
            ry = notNullOrElse(
                    node.getAnimatedLength(yAttr, rx, PercentageDimension.HEIGHT), rx);
        } else {
            // Both rx and ry are specified.
            rx = notNullOrElse(
                    node.getAnimatedLength(xAttr, initialRx, PercentageDimension.WIDTH), initialRx);
            ry = notNullOrElse(
                    node.getAnimatedLength(yAttr, initialRy, PercentageDimension.HEIGHT), initialRy);
        }

        if (rx == Length.UNSPECIFIED) rx = fallback;
        if (ry == Length.UNSPECIFIED) ry = fallback;

        return new AxisPair(rx, ry);
    }

    public static final class AxisPair {
        private final @NotNull LengthValue xAxis;
        private final @NotNull LengthValue yAxis;

        public AxisPair(@NotNull LengthValue xAxis, @NotNull LengthValue yAxis) {
            this.xAxis = xAxis;
            this.yAxis = yAxis;
        }

        public @NotNull LengthValue xAxis() {
            return xAxis;
        }

        public @NotNull LengthValue yAxis() {
            return yAxis;
        }
    }
}
