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

import java.awt.image.RGBImageFilter;

public abstract class ColorSpaceAwareRGBImageFilter extends RGBImageFilter {

    private final int[] tmp = new int[4];
    private boolean convertToLinear;

    protected int[] getRGB(int rgb) {
        tmp[3] = (rgb >> 24) & 0xFF;
        tmp[2] = (rgb >> 16) & 0xFF;
        tmp[1] = (rgb >> 8) & 0xFF;
        tmp[0] = rgb & 0xFF;
        if (convertToLinear) ColorUtil.sRGBtoLinearRGBinPlace(tmp);
        return tmp;
    }

    protected int pack(int[] argb) {
        if (convertToLinear) ColorUtil.linearRGBtoSRGBinPlace(argb);
        return ((argb[3] & 0xFF) << 24) |
                ((argb[2] & 0xFF) << 16) |
                ((argb[1] & 0xFF) << 8) |
                (argb[0] & 0xFF);
    }

    public void setConvertToLinear(boolean convertToLinear) {
        this.convertToLinear = convertToLinear;
    }
}
