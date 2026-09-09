/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.filter;

import static com.github.weisj.jsvg.util.ColorUtil.div255;

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.BlendMode;
import com.github.weisj.jsvg.util.ColorUtil;

public final class BlendModeComposite extends AbstractBlendComposite {

    private final @NotNull Blender blender;

    private BlendModeComposite(BlendMode blendMode) {
        this.blender = createBlender(blendMode);
    }

    public static @NotNull Composite create(BlendMode mode) {
        if (mode == BlendMode.Normal) return AlphaComposite.SrcOver;
        return new BlendModeComposite(mode);
    }

    @Override
    protected @NotNull Blender blender() {
        return blender;
    }

    /**
     * <pre>
     * Small letters: Premultiplied values
     * Capital letters: Non-premultiplied values
     *
     * Cx = cx / Ax
     *
     * Alpha Compositing:
     * Ao = As + Ab - (As * Ab)
     * co = (1-Ab)*cs + (1-As)*cb + As*Ab*B(Cb, Cs)
     *    = (1-Ab)*cs + (1-As)*cb + As*Ab*B(cb/Ab, cs/As)
     * </pre>
     *
     * @param blendMode the blend mode
     */
    private static @NotNull Blender createBlender(BlendMode blendMode) {
        switch (blendMode) {
            case Normal:
                throw new IllegalStateException("Use AlphaComposite.SrcOver instead");
            case Multiply:
                return BlendModeComposite::blendMultiply;
            case Screen:
                return BlendModeComposite::blendScreen;
            case Overlay:
                return BlendModeComposite::blendOverlay;
            case Darken:
                return BlendModeComposite::blendDarken;
            case Lighten:
                return BlendModeComposite::blendLighten;
            case ColorDodge:
                return BlendModeComposite::blendColorDodge;
            case ColorBurn:
                return BlendModeComposite::blendColorBurn;
            case HardLight:
                return BlendModeComposite::blendHardLight;
            case SoftLight:
                return BlendModeComposite::blendSoftLight;
            case Difference:
                return BlendModeComposite::blendDifference;
            case Exclusion:
                return BlendModeComposite::blendExclusion;
            case Hue:
            case Saturation:
            case Color:
            case Luminosity:
                return (src, dst, result) -> blendNonseparable(src, dst, result, blendMode);
        }
        throw new IllegalStateException("Mode not recognized " + blendMode);
    }

    /**
     * <pre>
     *      B(Cb, Cs) = Cb * Cs
     * </pre
     */
    private static void blendMultiply(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = div255(src[0] * srcM + dst[0] * dstM + src[0] * dst[0]);
        result[1] = div255(src[1] * srcM + dst[1] * dstM + src[1] * dst[1]);
        result[2] = div255(src[2] * srcM + dst[2] * dstM + src[2] * dst[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     *     B(Cb, Cs) = 1 - (1 - Cb) * (1 - Cs) = Cb + Cs - (Cb*Cs)
     * </pre
     */
    private static void blendScreen(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        result[0] = src[0] + dst[0] - div255(src[0] * dst[0]);
        result[1] = src[1] + dst[1] - div255(src[1] * dst[1]);
        result[2] = src[2] + dst[2] - div255(src[2] * dst[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     *      B(Cb, Cs) = HardLight(Cs, Cb)
     *
     *      if(Cb <= 0.5)
     *          B(Cb, Cs) = Multiply(2 x Cb, Cs)
     *      else
     *          B(Cb, Cs) = Screen(2 x Cb - 1, Cs)
     * </pre
     */
    private static void blendOverlay(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        blendHardLight(dst, src, result);
    }

    /**
     * <pre>
     *     B(Cb, Cs) = min(Cb, Cs)
     * </pre
     */
    private static void blendDarken(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = Math.min(
                div255(srcM * src[0]) + dst[0],
                div255(dstM * dst[0]) + src[0]);
        result[1] = Math.min(
                div255(srcM * src[1]) + dst[1],
                div255(dstM * dst[1]) + src[1]);
        result[2] = Math.min(
                div255(srcM * src[2]) + dst[2],
                div255(dstM * dst[2]) + src[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     *     B(Cb, Cs) = max(Cb, Cs)
     * </pre
     */
    private static void blendLighten(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = Math.max(
                div255(srcM * src[0]) + dst[0],
                div255(dstM * dst[0]) + src[0]);
        result[1] = Math.max(
                div255(srcM * src[1]) + dst[1],
                div255(dstM * dst[1]) + src[1]);
        result[2] = Math.max(
                div255(srcM * src[2]) + dst[2],
                div255(dstM * dst[2]) + src[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     *      if(Cb == 0)
     *          B(Cb, Cs) = 0
     *      else if(Cs == 1)
     *          B(Cb, Cs) = 1
     *      else
     *          B(Cb, Cs) = min(1, Cb / (1 - Cs))
     * </pre
     */
    private static void blendColorDodge(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = colorDodge(src[0], dst[0], srcM, srcA, dstM, dstA);
        result[1] = colorDodge(src[1], dst[1], srcM, srcA, dstM, dstA);
        result[2] = colorDodge(src[2], dst[2], srcM, srcA, dstM, dstA);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    private static int colorDodge(int src, int dst, int srcM, int srcA, int dstM, int dstA) {
        int base = srcM * src + dstM * dst;
        if (dst == 0) return div255(base);
        if (src == srcA) return div255(base + srcA * dstA);
        double blended = Math.min(srcA * dstA, (double) srcA * srcA * dst / (srcA - src));
        return ColorUtil.toRgbRange((base + blended) / 255);
    }

    /**
     * <pre>
     *      if(Cb == 1)
     *          B(Cb, Cs) = 1
     *      else if(Cs == 0)
     *          B(Cb, Cs) = 0
     *      else
     *          B(Cb, Cs) = 1 - min(1, (1 - Cb) / Cs)
     * </pre
     */
    private static void blendColorBurn(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = colorBurn(src[0], dst[0], srcM, srcA, dstM, dstA);
        result[1] = colorBurn(src[1], dst[1], srcM, srcA, dstM, dstA);
        result[2] = colorBurn(src[2], dst[2], srcM, srcA, dstM, dstA);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    private static int colorBurn(int src, int dst, int srcM, int srcA, int dstM, int dstA) {
        int base = srcM * src + dstM * dst;
        if (dst == dstA) return div255(base + srcA * dstA);
        if (src == 0) return div255(base);
        double blended = Math.max(0, srcA * dstA - (double) srcA * srcA * (dstA - dst) / src);
        return ColorUtil.toRgbRange((base + blended) / 255);
    }

    /**
     * <pre>
     *      if(Cs <= 0.5)
     *          B(Cb, Cs) = Multiply(Cb, 2 x Cs)
     *      else
     *          B(Cb, Cs) = Screen(Cb, 2 x Cs -1)
     * </pre
     */
    private static void blendHardLight(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        result[0] = hardLight(src[0], dst[0], srcA, dstA);
        result[1] = hardLight(src[1], dst[1], srcA, dstA);
        result[2] = hardLight(src[2], dst[2], srcA, dstA);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    private static int hardLight(int src, int dst, int srcA, int dstA) {
        int base = (255 - dstA) * src + (255 - srcA) * dst;
        int blended = 2 * src <= srcA ? 2 * src * dst
                : srcA * dstA - 2 * (srcA - src) * (dstA - dst);
        return div255(base + blended);
    }

    /**
     * <pre>
     *      if(Cs <= 0.5)
     *          B(Cb, Cs) = Cb - (1 - 2 x Cs) x Cb x (1 - Cb)
     *      else
     *          B(Cb, Cs) = Cb + (2 x Cs - 1) x (D(Cb) - Cb)
     *
     * with
     *
     *      if(Cb <= 0.25)
     *          D(Cb) = ((16 * Cb - 12) x Cb + 4) x Cb
     *      else
     *          D(Cb) = sqrt(Cb)
     * </pre
     */
    private static void blendSoftLight(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = softLight(src[0], dst[0], srcM, srcA, dstM, dstA);
        result[1] = softLight(src[1], dst[1], srcM, srcA, dstM, dstA);
        result[2] = softLight(src[2], dst[2], srcM, srcA, dstM, dstA);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    private static int softLight(int src, int dst, int srcM, int srcA, int dstM, int dstA) {
        float srcC = src / (float) srcA;
        float dstC = dst / (float) dstA;
        float b;
        if (srcC <= 0.5) {
            b = dstC - (1 - 2 * srcC) * dstC * (1 - dstC);
        } else {
            float d;
            if (dstC <= 0.25) {
                d = ((16 * dstC - 12) * dstC + 4) * dstC;
            } else {
                d = (float) Math.sqrt(dstC);
            }
            b = dstC + (2 * srcC - 1) * (d - dstC);
        }
        int bb = Math.round(b * 255);
        return div255(srcM * src + dstM * dst) + div255(div255(srcA * dstA) * bb);
    }


    /**
     * <pre>
     *     B(Cb, Cs) = | Cb - Cs |
     * </pre
     */
    private static void blendDifference(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = div255(srcM * src[0] + dstM * dst[0] + Math.abs(dstA * src[0] - srcA * dst[0]));
        result[1] = div255(srcM * src[1] + dstM * dst[1] + Math.abs(dstA * src[1] - srcA * dst[1]));
        result[2] = div255(srcM * src[2] + dstM * dst[2] + Math.abs(dstA * src[2] - srcA * dst[2]));
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     *     B(Cb, Cs) = Cb + Cs - 2 x Cb x Cs
     * </pre
     */
    private static void blendExclusion(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        result[0] = div255(255 * (src[0] + dst[0]) - 2 * src[0] * dst[0]);
        result[1] = div255(255 * (src[1] + dst[1]) - 2 * src[1] * dst[1]);
        result[2] = div255(255 * (src[2] + dst[2]) - 2 * src[2] * dst[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    private static void blendNonseparable(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result,
            @NotNull BlendMode mode) {
        int srcA = src[3];
        int dstA = dst[3];
        if (srcA == 0 || dstA == 0) {
            result[0] = src[0] + dst[0];
            result[1] = src[1] + dst[1];
            result[2] = src[2] + dst[2];
            result[3] = srcA + dstA;
            return;
        }

        boolean sourceColor = mode == BlendMode.Hue || mode == BlendMode.Color;
        int[] color = sourceColor ? src : dst;
        double alpha = sourceColor ? srcA : dstA;
        double red = color[0] / alpha;
        double green = color[1] / alpha;
        double blue = color[2] / alpha;

        if (mode == BlendMode.Hue || mode == BlendMode.Saturation) {
            int[] saturationColor = mode == BlendMode.Hue ? dst : src;
            double saturationAlpha = mode == BlendMode.Hue ? dstA : srcA;
            double saturation = (Math.max(saturationColor[0], Math.max(saturationColor[1], saturationColor[2]))
                    - Math.min(saturationColor[0], Math.min(saturationColor[1], saturationColor[2]))) / saturationAlpha;
            double min = Math.min(red, Math.min(green, blue));
            double max = Math.max(red, Math.max(green, blue));
            // SetSat is an affine scaling from [min, max] to [0, saturation].
            double scale = max > min ? saturation / (max - min) : 0;
            red = (red - min) * scale;
            green = (green - min) * scale;
            blue = (blue - min) * scale;
        }

        int[] luminosityColor = mode == BlendMode.Luminosity ? src : dst;
        double luminosityAlpha = mode == BlendMode.Luminosity ? srcA : dstA;
        double luminosity =
                ColorUtil.luminosity(luminosityColor[0], luminosityColor[1], luminosityColor[2]) / luminosityAlpha;
        double delta = luminosity - ColorUtil.luminosity(red, green, blue);
        red += delta;
        green += delta;
        blue += delta;

        // ClipColor preserves luminosity when SetLum pushes components outside [0, 1].
        double min = Math.min(red, Math.min(green, blue));
        double max = Math.max(red, Math.max(green, blue));
        if (min < 0) {
            double scale = luminosity / (luminosity - min);
            red = luminosity + (red - luminosity) * scale;
            green = luminosity + (green - luminosity) * scale;
            blue = luminosity + (blue - luminosity) * scale;
        }
        if (max > 1) {
            double scale = (1 - luminosity) / (max - luminosity);
            red = luminosity + (red - luminosity) * scale;
            green = luminosity + (green - luminosity) * scale;
            blue = luminosity + (blue - luminosity) * scale;
        }

        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        int overlap = srcA * dstA;
        result[0] = ColorUtil.toRgbRange((srcM * src[0] + dstM * dst[0] + overlap * red) / 255);
        result[1] = ColorUtil.toRgbRange((srcM * src[1] + dstM * dst[1] + overlap * green) / 255);
        result[2] = ColorUtil.toRgbRange((srcM * src[2] + dstM * dst[2] + overlap * blue) / 255);
        result[3] = srcA + dstA - div255(overlap);
    }

}
