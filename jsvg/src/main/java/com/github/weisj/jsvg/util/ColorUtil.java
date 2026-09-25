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
package com.github.weisj.jsvg.util;

import java.awt.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ColorUtil {
    private ColorUtil() {}

    public static int div255(int x) {
        // https://docs.google.com/document/d/1tNrMWShq55rfltcZxAx1N-6f82Dt7MWLDHm-5GQVEnE/edit
        x += 128;
        return (x + (x >> 8)) >> 8;
    }

    /** Converts a packed straight-alpha ARGB color to premultiplied ARGB. */
    public static int premultiply(int argb) {
        int alpha = argb >>> 24;
        if (alpha == 0) return 0;
        if (alpha == 255) return argb;
        int red = div255(((argb >>> 16) & 0xff) * alpha);
        int green = div255(((argb >>> 8) & 0xff) * alpha);
        int blue = div255((argb & 0xff) * alpha);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** Converts a packed premultiplied ARGB color to straight-alpha ARGB. */
    public static int unpremultiply(int argb) {
        int alpha = argb >>> 24;
        if (alpha == 0 || alpha == 255) return argb;
        int red = unpremultiply((argb >>> 16) & 0xff, alpha);
        int green = unpremultiply((argb >>> 8) & 0xff, alpha);
        int blue = unpremultiply(argb & 0xff, alpha);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int unpremultiply(int component, int alpha) {
        return toRgbRange((component * 255.0) / alpha);
    }

    public static int computeLuminance(int r, int g, int b) {
        return toRgbRange(0.2125 * r + 0.7164 * g + 0.0712 * b);
    }

    public static int toRgbRange(double value) {
        return (int) clampColor(Math.round(value));
    }

    /**
     * Creates an sRGB color from CSS HSL components.
     *
     * @param hueDegrees hue in degrees; values wrap around the color wheel
     * @param saturation saturation in the range {@code [0, 1]}; out-of-range values are clamped
     * @param lightness lightness in the range {@code [0, 1]}; out-of-range values are clamped
     * @param alpha alpha in the range {@code [0, 1]}; out-of-range values are clamped
     */
    public static @NotNull Color fromHsl(double hueDegrees, double saturation, double lightness, double alpha) {
        double hue = ((hueDegrees % 360) + 360) % 360 / 360;
        saturation = clampUnit(saturation);
        lightness = clampUnit(lightness);
        alpha = clampUnit(alpha);

        double red = lightness;
        double green = lightness;
        double blue = lightness;
        if (saturation != 0) {
            double max = lightness < 0.5
                    ? lightness * (1 + saturation)
                    : lightness + saturation - lightness * saturation;
            double min = 2 * lightness - max;
            red = hueToRgb(min, max, hue + 1.0 / 3.0);
            green = hueToRgb(min, max, hue);
            blue = hueToRgb(min, max, hue - 1.0 / 3.0);
        }
        return new Color(
                toRgbRange(red * 255),
                toRgbRange(green * 255),
                toRgbRange(blue * 255),
                toRgbRange(alpha * 255));
    }

    private static double hueToRgb(double min, double max, double hue) {
        if (hue < 0) hue += 1;
        if (hue > 1) hue -= 1;
        if (hue * 6 < 1) return min + (max - min) * hue * 6;
        if (hue * 2 < 1) return max;
        if (hue * 3 < 2) return min + (max - min) * (2.0 / 3.0 - hue) * 6;
        return min;
    }

    private static double clampUnit(double value) {
        return Math.min(1, Math.max(0, value));
    }

    public static Color withAlpha(@NotNull Color c, float alpha) {
        int a = clampColor((int) (alpha * 255));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static String toString(@Nullable Color c) {
        if (c == null) return "null";
        return String.format("Color[%d,%d,%d,%d]", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    /** The CSS compositing Lum function; input and result use the same component scale. */
    public static double luminosity(double red, double green, double blue) {
        return 0.3 * red + 0.59 * green + 0.11 * blue;
    }

    public static void convertRGBPretoHSL(int r, int g, int b, int a, float @NotNull [] hsl) {
        if (r < 0)
            r = 0;
        else if (r > 255) r = 255;
        if (g < 0)
            g = 0;
        else if (g > 255) g = 255;
        if (b < 0)
            b = 0;
        else if (b > 255) b = 255;

        float componentR = r / (float) a;
        float componentG = g / (float) a;
        float componentB = b / (float) a;

        float minComponent;
        float maxComponent;

        if (componentR > componentG) {
            minComponent = componentG;
            maxComponent = componentR;
        } else {
            minComponent = componentR;
            maxComponent = componentG;
        }
        if (componentB > maxComponent) {
            maxComponent = componentB;
        }
        if (componentB < minComponent) {
            minComponent = componentB;
        }

        float deltaMax = maxComponent - minComponent;

        float h;
        float s;
        float l;
        l = (maxComponent + minComponent) / 2f;

        if (deltaMax - 0.01f <= 0.0f) {
            h = 0;
            s = 0;
        } else {
            if (l < 0.5f) {
                assert maxComponent + minComponent != 0;
                s = deltaMax / (maxComponent + minComponent);
            } else {
                s = deltaMax / (2 - maxComponent - minComponent);
            }

            assert deltaMax > 0;
            float deltaR = (((maxComponent - componentR) / 6f) + (deltaMax / 2f)) / deltaMax;
            float deltaG = (((maxComponent - componentG) / 6f) + (deltaMax / 2f)) / deltaMax;
            float deltaB = (((maxComponent - componentB) / 6f) + (deltaMax / 2f)) / deltaMax;

            if (componentR == maxComponent) {
                h = deltaB - deltaG;
            } else if (componentG == maxComponent) {
                h = (1 / 3f) + deltaR - deltaB;
            } else {
                h = (2 / 3f) + deltaG - deltaR;
            }
            if (h < 0) {
                h += 1;
            }
            if (h > 1) {
                h -= 1;
            }
        }

        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;
    }

    public static void convertHSLtoRGB(float h, float s, float l, int @NotNull [] rgb) {
        Color color = fromHsl(h * 360, s, l, 1);
        rgb[0] = color.getRed();
        rgb[1] = color.getGreen();
        rgb[2] = color.getBlue();
    }

    /**
     * Color space conversion lookup tables.
     */
    private static final int[][] SRGBtoLinearRGBPre = new int[256][256];
    private static final int[] SRGBtoLinearRGB = new int[256];
    private static final int[][] LinearRGBtoSRGBPre = new int[256][256];
    private static final int[] LinearRGBtoSRGB = new int[256];

    static {
        // build the tables
        for (int k = 0; k < 256; k++) {
            SRGBtoLinearRGB[k] = convertSRGBtoLinearRGB(k, 1);
            LinearRGBtoSRGB[k] = convertLinearRGBtoSRGB(k, 1);
        }
        SRGBtoLinearRGBPre[255] = SRGBtoLinearRGB;
        LinearRGBtoSRGBPre[255] = LinearRGBtoSRGB;
        for (int i = 0; i < 255; i++) {
            int[] sRGBtoLinear = new int[256];
            int[] linearTosRGB = new int[256];
            float alpha = i / 255f;
            for (int k = 0; k < 256; k++) {
                sRGBtoLinear[k] = convertSRGBtoLinearRGB(k, alpha);
                linearTosRGB[k] = convertLinearRGBtoSRGB(k, alpha);
            }
            SRGBtoLinearRGBPre[i] = sRGBtoLinear;
            LinearRGBtoSRGBPre[i] = linearTosRGB;
        }
    }

    public static void sRGBtoLinearRGBinPlace(int @NotNull [] argb) {
        argb[0] = SRGBtoLinearRGB[argb[0]];
        argb[1] = SRGBtoLinearRGB[argb[1]];
        argb[2] = SRGBtoLinearRGB[argb[2]];
    }

    public static void linearRGBtoSRGBinPlace(int @NotNull [] argb) {
        argb[0] = LinearRGBtoSRGB[argb[0]];
        argb[1] = LinearRGBtoSRGB[argb[1]];
        argb[2] = LinearRGBtoSRGB[argb[2]];
    }

    public static void sRGBtoLinearRGBPreInPlace(int @NotNull [] argb) {
        int alpha = argb[3];
        int[] table = SRGBtoLinearRGBPre[alpha];
        argb[0] = table[argb[0]];
        argb[1] = table[argb[1]];
        argb[2] = table[argb[2]];
    }

    public static void linearRGBtoSRGBPreInPlace(int @NotNull [] argb) {
        int alpha = argb[3];
        int[] table = LinearRGBtoSRGBPre[alpha];
        argb[0] = table[argb[0]];
        argb[1] = table[argb[1]];
        argb[2] = table[argb[2]];
    }

    /** Converts packed premultiplied sRGB to premultiplied linearRGB, preserving alpha. */
    public static int sRGBtoLinearRGBPre(int argb) {
        int[] table = SRGBtoLinearRGBPre[argb >>> 24];
        return (argb & 0xff000000) | (table[(argb >>> 16) & 0xff] << 16)
                | (table[(argb >>> 8) & 0xff] << 8) | table[argb & 0xff];
    }

    /** Converts packed premultiplied linearRGB to premultiplied sRGB, preserving alpha. */
    public static int linearRGBtoSRGBPre(int argb) {
        int[] table = LinearRGBtoSRGBPre[argb >>> 24];
        return (argb & 0xff000000) | (table[(argb >>> 16) & 0xff] << 16)
                | (table[(argb >>> 8) & 0xff] << 8) | table[argb & 0xff];
    }

    public static int sRGBtoLinearRGBBand(int value) {
        return SRGBtoLinearRGB[value];
    }

    public static int sRGBtoLinearRGB(int argb) {
        int a = argb >>> 24;
        int r = SRGBtoLinearRGB[(argb >> 16) & 0xff];
        int g = SRGBtoLinearRGB[(argb >> 8) & 0xff];
        int b = SRGBtoLinearRGB[argb & 0xff];
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    public static int linearRGBtoSRGBBand(int value) {
        return LinearRGBtoSRGB[value];
    }

    public static int linearRGBtoSRGB(int argb) {
        int a = argb >>> 24;
        int r = LinearRGBtoSRGB[(argb >> 16) & 0xff];
        int g = LinearRGBtoSRGB[(argb >> 8) & 0xff];
        int b = LinearRGBtoSRGB[argb & 0xff];
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    /**
     * Helper function to convert a color component in sRGB space to linear
     * RGB space.  Used to build a static lookup table.
     */
    private static int convertSRGBtoLinearRGB(int color, float alpha) {
        float factor = (255f * alpha);
        float input = color / factor;
        float output;
        if (input <= 0.04045f) {
            output = input / 12.92f;
        } else {
            output = (float) Math.pow((input + 0.055) / 1.055, 2.4);
        }

        return Math.round(output * factor);
    }

    /**
     * Helper function to convert a color component in linear RGB space to
     * SRGB space.  Used to build a static lookup table.
     */
    private static int convertLinearRGBtoSRGB(int color, float alpha) {
        float factor = (255f * alpha);
        float input = color / factor;
        float output;
        if (input <= 0.0031308) {
            output = input * 12.92f;
        } else {
            output = (1.055f * ((float) Math.pow(input, (1.0 / 2.4)))) - 0.055f;
        }

        return Math.round(output * factor);
    }

    public static int clampColor(int v) {
        return Math.max(Math.min(255, v), 0);
    }

    public static double clampColor(double v) {
        return Math.max(Math.min(255, v), 0);
    }
}
