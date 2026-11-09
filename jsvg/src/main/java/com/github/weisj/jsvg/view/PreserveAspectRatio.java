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
package com.github.weisj.jsvg.view;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Controls how an SVG view box is fitted into its viewport.
 *
 * @see <a href="https://www.w3.org/TR/SVG2/coords.html#PreserveAspectRatioAttribute">
 *      SVG 2 preserveAspectRatio</a>
 */
public final class PreserveAspectRatio {

    public enum Align {

        /**
         * Do not force uniform scaling.
         * Scale the graphic content of the given element non-uniformly if necessary such that the
         * element's bounding box exactly matches the viewport rectangle.
         * Note that if {@code align} is {@code none}, then the optional {@code meetOrSlice} value is ignored.
         */
        None,
        /**
         * Force uniform scaling.
         * Align the {@code min-x} of the element's viewBox with the smallest X value of the viewport.
         * Align the {@code min-y} of the element's viewBox with the smallest Y value of the viewport.
         */
        xMinYMin,
        /**
         * Force uniform scaling.
         * Align the midpoint X value of the element's viewBox with the midpoint X value of the viewport.
         * Align the {@code min-y} of the element's viewBox with the smallest Y value of the viewport.
         */
        xMidYMin,
        /**
         * Force uniform scaling.
         * Align the {@code min-x+width} of the element's viewBox with the maximum X value of the viewport.
         * Align the {@code min-y} of the element's viewBox with the smallest Y value of the viewport.
         */
        xMaxYMin,
        /**
         * Force uniform scaling.
         * Align the {@code min-x} of the element's viewBox with the smallest X value of the viewport.
         * Align the midpoint Y value of the element's viewBox with the midpoint Y value of the viewport.
         */
        xMinYMid,
        /**
         * Force uniform scaling.
         * Align the midpoint X value of the element's viewBox with the midpoint X value of the viewport.
         * Align the midpoint Y value of the element's viewBox with the midpoint Y value of the viewport.
         */
        xMidYMid,
        /**
         * Force uniform scaling.
         * Align the {@code min-x+width} of the element's viewBox with the maximum X value of the viewport.
         * Align the midpoint Y value of the element's viewBox with the midpoint Y value of the viewport.
         */
        xMaxYMid,
        /**
         * Force uniform scaling.
         * Align the {@code min-x} of the element's viewBox with the smallest X value of the viewport.
         * Align the {@code min-y+height} of the element's viewBox with the maximum Y value of the viewport.
         */
        xMinYMax,
        /**
         * Force uniform scaling.
         * Align the midpoint X value of the element's viewBox with the midpoint X value of the viewport.
         * Align the {@code min-y+height} of the element's viewBox with the maximum Y value of the viewport.
         */
        xMidYMax,
        /**
         * Force uniform scaling.
         * Align the {@code min-x+width} of the element's viewBox with the maximum X value of the viewport.
         * Align the {@code min-y+height} of the element's viewBox with the maximum Y value of the viewport.
         */
        xMaxYMax
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

    /** Creates an aspect-ratio mapping from its SVG alignment and scaling values. */
    public static @NotNull PreserveAspectRatio of(@NotNull Align align, @NotNull MeetOrSlice meetOrSlice) {
        return new PreserveAspectRatio(Objects.requireNonNull(align), Objects.requireNonNull(meetOrSlice));
    }

    /** Creates the {@code none} mapping, which permits non-uniform scaling. */
    public static @NotNull PreserveAspectRatio none() {
        return of(Align.None, MeetOrSlice.Meet);
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

    @Override
    public String toString() {
        return "PreserveAspectRatio{" +
                "align=" + align +
                ", meetOrSlice=" + meetOrSlice +
                '}';
    }
}
