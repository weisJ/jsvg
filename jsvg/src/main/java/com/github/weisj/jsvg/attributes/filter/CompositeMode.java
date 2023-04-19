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
package com.github.weisj.jsvg.attributes.filter;

import com.github.weisj.jsvg.attributes.Default;

public enum CompositeMode {
    /**
     * The source graphic defined by the in attribute (the MDN logo) is placed over
     * the destination graphic defined by the in2 attribute (the circle).
     * This is the default operation, which will be used if no operation or
     * an unsupported operation is specified.
     */
    @Default
    Over,
    /**
     * The parts of the source graphic defined by the in attribute that overlap
     * the destination graphic defined in the in2 attribute, replace the destination graphic.
     */
    In,
    /**
     * The parts of the source graphic defined by the in attribute that fall outside the destination
     * graphic defined in the in2 attribute, are displayed.
     */
    Out,
    /**
     * The parts of the source graphic defined in the in attribute, which overlap the destination graphic defined
     * in the in2 attribute, replace the destination graphic.
     * The parts of the destination graphic that do not overlap with the source graphic stay untouched.
     */
    Atop,
    /**
     * The non-overlapping regions of the source graphic defined in the in attribute and
     * the destination graphic defined in the in2 attribute are combined.
     */
    Xor,
    /**
     * The sum of the source graphic defined in the in attribute and the
     * destination graphic defined in the in2 attribute is displayed.
     */
    Lighter,
    /**
     * The arithmetic operation is useful for combining the output from the
     * feDiffuseLighting and feSpecularLighting filters with texture data.
     * If the arithmetic operation is chosen, each result pixel is computed using the following formula:
     * <p>
     * result = k1*i1*i2 + k2*i1 + k3*i2 + k4
     * <p>
     * where:
     * <ul>
     *     <li>i1 and i2 indicate the corresponding pixel channel values of the input image,
     *     which map to in and in2 respectively</li>
     *     <li>k1, k2, k3, and k4 indicate the values of the attributes with the same name.</li>
     * </ul>
     */
    Arithmetic
}
