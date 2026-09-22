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
        None(AlignType.Min, AlignType.Min),
        xMinYMin(AlignType.Min, AlignType.Min),
        xMidYMin(AlignType.Mid, AlignType.Min),
        xMaxYMin(AlignType.Max, AlignType.Min),
        xMinYMid(AlignType.Min, AlignType.Mid),
        @Default
        xMidYMid(AlignType.Mid, AlignType.Mid),
        xMaxYMid(AlignType.Max, AlignType.Mid),
        xMinYMax(AlignType.Min, AlignType.Max),
        xMidYMax(AlignType.Mid, AlignType.Max),
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
        @Default
        Meet,
        Slice
    }

    public final @NotNull Align align;
    public final @NotNull MeetOrSlice meetOrSlice;

    private PreserveAspectRatio(@NotNull Align align, @NotNull MeetOrSlice meetOrSlice) {
        this.align = align;
        this.meetOrSlice = meetOrSlice;
    }

    public static @NotNull PreserveAspectRatio of(@NotNull Align align, @NotNull MeetOrSlice meetOrSlice) {
        return new PreserveAspectRatio(align, meetOrSlice);
    }

    public static @NotNull PreserveAspectRatio none() {
        return new PreserveAspectRatio(Align.None, MeetOrSlice.Meet);
    }

    public static @NotNull PreserveAspectRatio parse(@Nullable String value, @NotNull AttributeParser parser) {
        return parse(value, null, parser);
    }

    @SuppressWarnings("javabugs:S2259") // parseStringList returns a non-null array for this overload.
    public static @NotNull PreserveAspectRatio parse(@Nullable String value,
            @Nullable PreserveAspectRatio fallback, @NotNull AttributeParser parser) {
        Align align = Align.xMidYMid;
        MeetOrSlice meetOrSlice = MeetOrSlice.Meet;
        if (value == null) {
            return fallback != null ? fallback : new PreserveAspectRatio(align, meetOrSlice);
        }
        String[] components = parser.parseStringList(value, SeparatorMode.COMMA_AND_WHITESPACE);
        if (components.length < 1 || components.length > 2) {
            throw new IllegalArgumentException("Too many arguments specified: " + value);
        }
        align = parser.parseEnum(components[0], align);
        if (components.length > 1) {
            meetOrSlice = parser.parseEnum(components[1], meetOrSlice);
        }
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
