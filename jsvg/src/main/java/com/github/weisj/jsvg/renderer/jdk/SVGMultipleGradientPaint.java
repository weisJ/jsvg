/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jdk;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.lang.ref.SoftReference;

import org.jetbrains.annotations.NotNull;

/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR
 * REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License version 2 only, as published by the Free Software Foundation. Oracle
 * designates this particular file as subject to the "Classpath" exception as provided by Oracle in
 * the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com
 * if you need additional information or have any questions.
 */

/**
 * This is the superclass for Paints which use a multiple color
 * gradient to fill in their raster.  It provides storage for variables and
 * enumerated values common to
 * {@code LinearGradientPaint} and {@code RadialGradientPaint}.
 *
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 * @since 1.6
 */
public abstract class SVGMultipleGradientPaint implements Paint {

    /** The transparency of this paint object. */
    final int transparency;

    /** Gradient keyframe values in the range 0 to 1. */
    final float @NotNull [] fractions;

    /** Gradient colors. */
    final @NotNull Color @NotNull [] colors;

    /** Transform to apply to gradient. */
    final @NotNull AffineTransform gradientTransform;

    /** The method to use when painting outside the gradient bounds. */
    final @NotNull MultipleGradientPaint.CycleMethod cycleMethod;

    /** The color space in which to perform the gradient interpolation. */
    final @NotNull MultipleGradientPaint.ColorSpaceType colorSpace;

    /**
     * The following fields are used only by MultipleGradientPaintContext
     * to cache certain values that remain constant and do not need to be
     * recalculated for each context created from this paint instance.
     */
    ColorModel model;
    float[] normalizedIntervals;
    boolean isSimpleLookup;
    SoftReference<int[][]> gradients;
    SoftReference<int[]> gradient;
    int fastGradientArraySize;

    /**
     * Package-private constructor.
     *
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors corresponding to each fractional value
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     * @param colorSpace which color space to use for interpolation,
     *                   either {@code SRGB} or {@code LINEAR_RGB}
     * @param gradientTransform transform to apply to the gradient
     *
     * @throws NullPointerException
     * if {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code gradientTransform} is null,
     * or {@code cycleMethod} is null,
     * or {@code colorSpace} is null
     * @throws IllegalArgumentException
     * if {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    SVGMultipleGradientPaint(float @NotNull [] fractions,
            @NotNull Color @NotNull [] colors,
            @NotNull MultipleGradientPaint.CycleMethod cycleMethod,
            @NotNull MultipleGradientPaint.ColorSpaceType colorSpace,
            @NotNull AffineTransform gradientTransform) {
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions must have equal size");
        }

        if (colors.length < 2) {
            throw new IllegalArgumentException("User must specify at least 2 colors");
        }

        // check that values are in the proper range and progress
        // in increasing order from 0 to 1
        float previousFraction = -1.0f;
        for (float currentFraction : fractions) {
            if (currentFraction < 0f || currentFraction > 1f) {
                throw new IllegalArgumentException("Fraction values must be in the range 0 to 1: " + currentFraction);
            }

            if (currentFraction <= previousFraction) {
                throw new IllegalArgumentException("Keyframe fractions must be increasing: " + currentFraction);
            }

            previousFraction = currentFraction;
        }

        if (fractions[0] != 0f) {
            throw new IllegalStateException("Gradient start point must be equal to zero");
        }

        if (fractions[fractions.length - 1] != 1f) {
            throw new IllegalStateException("Gradient end point must be equal to one");
        }

        this.fractions = fractions;
        this.colors = colors;

        // copy some flags
        this.colorSpace = colorSpace;
        this.cycleMethod = cycleMethod;

        // copy the gradient transform
        this.gradientTransform = gradientTransform;

        // determine transparency
        boolean opaque = true;
        for (Color color : colors) {
            opaque = opaque && (color.getAlpha() == 0xff);
        }
        this.transparency = opaque ? OPAQUE : TRANSLUCENT;
    }

    @Override
    public int getTransparency() {
        return transparency;
    }
}
