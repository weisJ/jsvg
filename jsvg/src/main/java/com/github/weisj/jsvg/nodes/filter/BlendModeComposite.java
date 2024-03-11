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
package com.github.weisj.jsvg.nodes.filter;

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

    private static int div255(int x) {
        // https://docs.google.com/document/d/1tNrMWShq55rfltcZxAx1N-6f82Dt7MWLDHm-5GQVEnE/edit
        x += 128;
        return (x + (x >> 8)) >> 8;
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
                return BlendModeComposite::blendHue;
            case Saturation:
                return BlendModeComposite::blendSaturation;
            case Color:
                return BlendModeComposite::blendColor;
            case Luminosity:
                return BlendModeComposite::blendLuminosity;
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
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        int dstComp = (dstA * 255) / 2;
        result[0] = (src[0] <= dstComp)
                ? div255((src[0] * srcM + 2 * dst[0] * dstM + 2 * src[0] * dst[0]))
                : src[0] + dst[0]
                        + div255(dstA * src[0] + srcA * dst[0] - srcA * dstA - 2 * src[0] * dst[0]);
        result[1] = (src[1] <= dstComp)
                ? div255((src[1] * srcM + 2 * dst[1] * dstM + 2 * src[1] * dst[1]))
                : src[1] + dst[1]
                        + div255(dstA * src[1] + srcA * dst[1] - srcA * dstA - 2 * src[1] * dst[1]);
        result[2] = (src[2] <= dstComp)
                ? div255((src[2] * srcM + 2 * dst[2] * dstM + 2 * src[2] * dst[2]))
                : src[2] + dst[2]
                        + div255(dstA * src[2] + srcA * dst[2] - srcA * dstA - 2 * src[2] * dst[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
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
        if (dst == 0) return div255(srcM * src);
        if (src == srcA) return div255(srcM * srcA + dstM * dst + srcA * dstA);
        return Math.min(
                div255(srcM * src + dstM * dst + srcA * dstA),
                div255(srcM * src + dstM * dst) + Math.round(div255(dstA * src) / (255 - (float) dst / dstA)));
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
        if (dst == dstA) return div255(srcM * src + dstM * dstA + srcA * dstA);
        if (src == 0) return div255(dstM * dst);
        float srcC = src / (float) srcA;
        float dstC = dst / (float) dstA;
        int b = Math.round(Math.min(255f, (255f - dstC) / srcC));
        return div255(srcM * src + dstM * dst) + div255(div255(srcA * dstA) * b);
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
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        int srcComp = (srcA * 255) / 2;
        result[0] = (src[0] <= srcComp)
                ? div255((2 * src[0] * srcM + dst[0] * dstM + 2 * src[0] * dst[0]))
                : src[0] + dst[0]
                        + div255(dstA * src[0] + srcA * dst[0] - srcA * dstA - 2 * src[0] * dst[0]);
        result[1] = (src[1] <= srcComp)
                ? div255((2 * src[1] * srcM + dst[1] * dstM + 2 * src[1] * dst[1]))
                : src[1] + dst[1]
                        + div255(dstA * src[1] + srcA * dst[1] - srcA * dstA - 2 * src[1] * dst[1]);
        result[2] = (src[2] <= srcComp)
                ? div255((2 * src[2] * srcM + dst[2] * dstM + 2 * src[2] * dst[2]))
                : src[2] + dst[2]
                        + div255(dstA * src[2] + srcA * dst[2] - srcA * dstA - 2 * src[2] * dst[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
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
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        result[0] = div255(srcM * src[0] + dstM * dst[0] + dstA * src[0] + srcA * dst[0]
                - 2 * div255(src[0] * dst[0]));
        result[1] = div255(srcM * src[1] + dstM * dst[1] + dstA * src[1] + srcA * dst[1]
                - 2 * div255(src[1] * dst[1]));
        result[2] = div255(srcM * src[2] + dstM * dst[2] + dstA * src[2] + srcA * dst[2]
                - 2 * div255(src[2] * dst[2]));
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     * </pre
     */
    private static void blendHue(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        float[] srcHSL = new float[3];
        ColorUtil.RGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
        float[] dstHSL = new float[3];
        ColorUtil.RGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);

        ColorUtil.HSLtoRGB(srcHSL[0], dstHSL[1], dstHSL[2], result);

        result[0] = div255(srcM * src[0] + dstM * dst[0] + div255(srcA * dstA) * result[0]);
        result[1] = div255(srcM * src[1] + dstM * dst[1] + div255(srcA * dstA) * result[1]);
        result[2] = div255(srcM * src[2] + dstM * dst[2] + div255(srcA * dstA) * result[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     * </pre
     */
    private static void blendSaturation(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        float[] srcHSL = new float[3];
        ColorUtil.RGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
        float[] dstHSL = new float[3];
        ColorUtil.RGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);

        ColorUtil.HSLtoRGB(dstHSL[0], srcHSL[1], dstHSL[2], result);

        result[0] = div255(srcM * src[0] + dstM * dst[0] + div255(srcA * dstA) * result[0]);
        result[1] = div255(srcM * src[1] + dstM * dst[1] + div255(srcA * dstA) * result[1]);
        result[2] = div255(srcM * src[2] + dstM * dst[2] + div255(srcA * dstA) * result[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     * </pre
     */
    private static void blendColor(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        float[] srcHSL = new float[3];
        ColorUtil.RGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
        float[] dstHSL = new float[3];
        ColorUtil.RGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);

        ColorUtil.HSLtoRGB(srcHSL[0], srcHSL[1], dstHSL[2], result);

        result[0] = div255(srcM * src[0] + dstM * dst[0] + div255(srcA * dstA) * result[0]);
        result[1] = div255(srcM * src[1] + dstM * dst[1] + div255(srcA * dstA) * result[1]);
        result[2] = div255(srcM * src[2] + dstM * dst[2] + div255(srcA * dstA) * result[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }

    /**
     * <pre>
     * </pre
     */
    private static void blendLuminosity(int @NotNull [] src, int @NotNull [] dst, int @NotNull [] result) {
        int srcA = src[3];
        int dstA = dst[3];
        int srcM = 255 - dstA;
        int dstM = 255 - srcA;
        float[] srcHSL = new float[3];
        ColorUtil.RGBPretoHSL(src[0], src[1], src[2], srcA, srcHSL);
        float[] dstHSL = new float[3];
        ColorUtil.RGBPretoHSL(dst[0], dst[1], dst[2], dstA, dstHSL);

        ColorUtil.HSLtoRGB(dstHSL[0], dstHSL[1], srcHSL[2], result);

        result[0] = div255(srcM * src[0] + dstM * dst[0] + div255(srcA * dstA) * result[0]);
        result[1] = div255(srcM * src[1] + dstM * dst[1] + div255(srcA * dstA) * result[1]);
        result[2] = div255(srcM * src[2] + dstM * dst[2] + div255(srcA * dstA) * result[2]);
        result[3] = srcA + dstA - div255(srcA * dstA);
    }
}
