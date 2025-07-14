/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
package com.github.weisj.jsvg.attributes;

import java.awt.geom.AffineTransform;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;


public final class PreserveAspectRatio {

    private enum AlignType {
        Min {
            @Override
            double align(double size1, double size2) {
                return 0;
            }
        },
        Mid {
            @Override
            double align(double size1, double size2) {
                return (size1 - size2) / 2;
            }
        },
        Max {
            @Override
            double align(double size1, double size2) {
                return size1 - size2;
            }
        };

        abstract double align(double size1, double size2);
    }

    public enum Align {

        /**
         * Do not force uniform scaling.
         * Scale the graphic content of the given element non-uniformly if necessary such that the
         * element's bounding box exactly matches the viewport rectangle.
         * Note that if [align] is none, then the optional [meetOrSlice] value is ignored.
         */
        None(AlignType.Min, AlignType.Min),
        /**
         * Force uniform scaling.
         * Align the [min-x] of the element's viewBox with the smallest X value of the viewport.
         * Align the [min-y] of the element's viewBox with the smallest Y value of the viewport.
         */
        xMinYMin(AlignType.Min, AlignType.Min),
        /**
         * Force uniform scaling.
         * Align the midpoint X value of the element's viewBox with the midpoint X value of the viewport.
         * Align the [min-y] of the element's viewBox with the smallest Y value of the viewport.
         */
        xMidYMin(AlignType.Mid, AlignType.Min),
        /**
         * Force uniform scaling.
         * Align the [min-x]+[width] of the element's viewBox with the maximum X value of the viewport.
         * Align the [min-y] of the element's viewBox with the smallest Y value of the viewport.
         */
        xMaxYMin(AlignType.Max, AlignType.Min),
        /**
         * Force uniform scaling.
         * Align the [min-x] of the element's viewBox with the smallest X value of the viewport.
         * Align the midpoint Y value of the element's viewBox with the midpoint Y value of the viewport.
         */
        xMinYMid(AlignType.Min, AlignType.Mid),
        /**
         * Force uniform scaling.
         * Align the midpoint X value of the element's viewBox with the midpoint X value of the viewport.
         * Align the midpoint Y value of the element's viewBox with the midpoint Y value of the viewport.
         */
        @Default
        xMidYMid(AlignType.Mid, AlignType.Mid),
        /**
         * Force uniform scaling.
         * Align the [min-x]+[width] of the element's viewBox with the maximum X value of the viewport.
         * Align the midpoint Y value of the element's viewBox with the midpoint Y value of the viewport.
         */
        xMaxYMid(AlignType.Max, AlignType.Mid),
        /**
         * Force uniform scaling.
         * Align the [min-x] of the element's viewBox with the smallest X value of the viewport.
         * Align the [min-y]+[height] of the element's viewBox with the maximum Y value of the viewport.
         */
        xMinYMax(AlignType.Min, AlignType.Max),
        /**
         * Force uniform scaling.
         * Align the midpoint X value of the element's viewBox with the midpoint X value of the viewport.
         * Align the [min-y]+[height] of the element's viewBox with the maximum Y value of the viewport.
         */
        xMidYMax(AlignType.Mid, AlignType.Max),
        /**
         * Force uniform scaling.
         * Align the [min-x]+[width] of the element's viewBox with the maximum X value of the viewport.
         * Align the [min-y]+[height] of the element's viewBox with the maximum Y value of the viewport.
         */
        xMaxYMax(AlignType.Max, AlignType.Max);

        private final @NotNull AlignType xAlign;
        private final @NotNull AlignType yAlign;

        Align(@NotNull AlignType xAlign, @NotNull AlignType yAlign) {
            this.xAlign = xAlign;
            this.yAlign = yAlign;
        }

        @Override
        public String toString() {
            return name() + "{" + xAlign + ", " + yAlign + "}";
        }
    }

    public enum MeetOrSlice {
        /**
         * Scale the graphic such that:
         * <p>
         * - aspect ratio is preserved
         * - the entire viewBox is visible within the viewport
         * - the viewBox is scaled up as much as possible, while still meeting the other criteria
         * <p>
         * In this case, if the aspect ratio of the graphic does not match the viewport,
         * some viewport will extend beyond the bounds of the viewBox
         * (i.e., the area into which the viewBox will draw will be smaller than the viewport).
         */
        @Default
        Meet,
        /**
         * Scale the graphic such that:
         * <p>
         * - aspect ratio is preserved
         * - the entire viewport is covered by the viewBox
         * - the viewBox is scaled down as much as possible, while still meeting the other criteria
         * <p>
         * In this case, if the aspect ratio of the viewBox does not match the viewport, some
         * viewBox will extend beyond the bounds of the viewport
         * (i.e., the area into which the viewBox will draw is larger than the viewport).
         */
        Slice
    }

    public final @NotNull Align align;
    public final @NotNull MeetOrSlice meetOrSlice;

    private PreserveAspectRatio(@NotNull Align align, @NotNull MeetOrSlice meetOrSlice) {
        this.align = align;
        this.meetOrSlice = meetOrSlice;
    }

    public static @NotNull PreserveAspectRatio none() {
        return new PreserveAspectRatio(Align.None, MeetOrSlice.Meet);
    }

    public static @NotNull PreserveAspectRatio parse(@Nullable String preserveAspectRation,
            @NotNull AttributeParser parser) {
        return parse(preserveAspectRation, null, parser);
    }

    public static @NotNull PreserveAspectRatio parse(@Nullable String preserveAspectRation,
            @Nullable PreserveAspectRatio fallback, @NotNull AttributeParser parser) {
        Align align = Align.xMidYMid;
        MeetOrSlice meetOrSlice = MeetOrSlice.Meet;
        if (preserveAspectRation == null) {
            return fallback != null ? fallback : new PreserveAspectRatio(align, meetOrSlice);
        }
        String[] components =
                parser.parseStringList(preserveAspectRation, SeparatorMode.COMMA_AND_WHITESPACE);
        if (components.length < 1 || components.length > 2)
            throw new IllegalArgumentException("Too many arguments specified: " + preserveAspectRation);
        align = parser.parseEnum(components[0], align);
        if (components.length > 1)
            meetOrSlice = parser.parseEnum(components[1], meetOrSlice);
        return new PreserveAspectRatio(align, meetOrSlice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreserveAspectRatio)) return false;
        PreserveAspectRatio that = (PreserveAspectRatio) o;
        return align == that.align && meetOrSlice == that.meetOrSlice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(align, meetOrSlice);
    }

    // https://www.w3.org/TR/SVG2/coords.html#ComputingAViewportsTransform
    public @NotNull AffineTransform computeViewportTransform(@NotNull FloatSize size, @NotNull ViewBox viewBox) {
        AffineTransform viewTransform = new AffineTransform();
        if (align == Align.None) {
            viewTransform.scale(size.width / viewBox.width, size.height / viewBox.height);
        } else {
            double xScale = size.width / viewBox.width;
            double yScale = size.height / viewBox.height;

            switch (meetOrSlice) {
                case Meet:
                    xScale = yScale = Math.min(xScale, yScale);
                    break;
                case Slice:
                    xScale = yScale = Math.max(xScale, yScale);
                    break;
                default:
                    throw new IllegalStateException();
            }

            viewTransform.translate(
                    align.xAlign.align(size.width, viewBox.width * xScale),
                    align.yAlign.align(size.height, viewBox.height * yScale));
            viewTransform.scale(xScale, yScale);
        }
        viewTransform.translate(-viewBox.x, -viewBox.y);
        return viewTransform;
    }

    @Override
    public String toString() {
        return "PreserveAspectRatio{" +
                "align=" + align +
                ", meetOrSlice=" + meetOrSlice +
                '}';
    }
}
