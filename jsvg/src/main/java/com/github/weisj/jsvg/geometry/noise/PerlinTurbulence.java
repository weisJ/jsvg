/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.geometry.noise;

import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.Nullable;

/*
 * Results are in the range [1, 2**31 - 2].
 *
 * Algorithm is: r = (a * r) mod m where a = 16807 and m = 2**31 - 1 = 2147483647
 *
 * See [Park & Miller], CACM vol. 31 no. 10 p. 1195, Oct. 1988
 *
 * To test: the algorithm should produce the result 1043618065 as the 10,000th generated number if
 * the original seed is 1.
 */
public final class PerlinTurbulence {
    private static final int RAND_m = 2147483647; /* 2**31 - 1 */
    private static final int RAND_a = 16807; /* 7**5; primitive root of m */
    private static final int RAND_q = 127773; /* m / a */
    private static final int RAND_r = 2836; /* m % a */

    private static final int BSize = 0x100;
    private static final int BM = 0xff;
    private static final double PerlinN = 0x1000;

    private final int[] uLatticeSelector = new int[BSize + 1];
    private final double[] fGradient = new double[(BSize + 1) * 8];
    private final int numOctaves;
    private final double xFrequency;
    private final double yFrequency;

    public PerlinTurbulence(int seed, int numOctaves, double xFrequency, double yFrequency) {
        this.numOctaves = numOctaves;
        this.xFrequency = xFrequency;
        this.yFrequency = yFrequency;
        init(seed);
    }

    public static class StitchInfo {
        private int width;
        private int height;
        private int wrapX;
        private int wrapY;
    }

    private static int setupSeed(int seed) {
        if (seed <= 0) seed = -(seed % (RAND_m - 1)) + 1;
        if (seed > RAND_m - 1) seed = RAND_m - 1;
        return seed;
    }

    private static int random(int seed) {
        int result = RAND_a * (seed % RAND_q) - RAND_r * (seed / RAND_q);
        if (result <= 0) result += RAND_m;
        return result;
    }

    private void init(int seed) {
        int lSeed = setupSeed(seed);

        int i, j, k;

        for (k = 0; k < 4; k++) {
            for (i = 0; i < BSize; i++) {
                double u, v;
                do {
                    u = ((lSeed = random(lSeed)) % (BSize + BSize)) - (double) BSize;
                    v = ((lSeed = random(lSeed)) % (BSize + BSize)) - (double) BSize;
                } while (u == 0 && v == 0);

                double s = Math.sqrt(u * u + v * v);
                double si = 1 / s;
                fGradient[i * 8 + k * 2] = u * si;
                fGradient[i * 8 + k * 2 + 1] = v * si;
            }
        }

        for (i = 0; i < BSize; i++)
            uLatticeSelector[i] = i;

        while (--i > 0) {
            k = uLatticeSelector[i];
            j = (lSeed = random(lSeed)) % BSize;
            uLatticeSelector[i] = uLatticeSelector[j];
            uLatticeSelector[j] = k;

            // Now we apply the lattice to the gradient array, this
            // lets us avoid one of the lattice lookups.
            int s1 = i << 3;
            int s2 = j << 3;
            for (j = 0; j < 8; j++) {
                double s = fGradient[s1 + j];
                fGradient[s1 + j] = fGradient[s2 + j];
                fGradient[s2 + j] = s;
            }
        }
        uLatticeSelector[BSize] = uLatticeSelector[0];
        for (j = 0; j < 8; j++)
            fGradient[(BSize * 8) + j] = fGradient[j];
    }

    private static double curve(double t) {
        return t * t * (3. - 2. * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private void noise2(double[] noiseChannels, double vec0, double vec1, @Nullable StitchInfo stitchInfo) {
        double t = vec0 + PerlinN;
        int bx0 = (int) t;
        int bx1 = bx0 + 1;
        final double rx0 = t - bx0;
        final double rx1 = rx0 - 1.0f;
        final double sx = curve(rx0);

        t = vec1 + PerlinN;
        int by0 = (int) t;
        int by1 = by0 + 1;
        final double ry0 = t - (int) t;
        final double ry1 = ry0 - 1.0f;
        final double sy = curve(ry0);

        // If stitching, adjust lattice points accordingly.
        if (stitchInfo != null) {
            if (bx0 >= stitchInfo.wrapX)
                bx0 -= stitchInfo.width;
            if (bx1 >= stitchInfo.wrapX)
                bx1 -= stitchInfo.width;

            if (by0 >= stitchInfo.wrapY)
                by0 -= stitchInfo.height;
            if (by1 >= stitchInfo.wrapY)
                by1 -= stitchInfo.height;
        }

        bx0 &= BM;
        bx1 &= BM;
        by0 &= BM;
        by1 &= BM;

        final int i = uLatticeSelector[bx0];
        final int j = uLatticeSelector[bx1];

        final int b00 = ((i + by0) & BM) << 3;
        final int b10 = ((j + by0) & BM) << 3;
        final int b01 = ((i + by1) & BM) << 3;
        final int b11 = ((j + by1) & BM) << 3;

        for (int channelIndex = 0; channelIndex < noiseChannels.length; channelIndex++) {
            int offset = 2 * channelIndex;
            noiseChannels[channelIndex] =
                    lerp(sy,
                            lerp(sx,
                                    rx0 * fGradient[b00 + offset] + ry0 * fGradient[b00 + offset + 1],
                                    rx1 * fGradient[b10 + offset] + ry0 * fGradient[b10 + offset + 1]),
                            lerp(sx,
                                    rx0 * fGradient[b01 + offset] + ry1 * fGradient[b01 + offset + 1],
                                    rx1 * fGradient[b11 + offset] + ry1 * fGradient[b11 + offset + 1]));
        }
    }

    public void turbulence(double[] turbulenceChannels, double pointX, double pointY,
            boolean fractalSum, @Nullable StitchInfo stitchInfo, @Nullable Rectangle2D.Double tile) {
        double baseFrequencyX = xFrequency;
        double baseFrequencyY = yFrequency;
        if (stitchInfo != null) {
            assert tile != null;
            if (baseFrequencyX != 0) {
                baseFrequencyX = adjustFrequency(baseFrequencyX, tile.width);
            }
            if (baseFrequencyY != 0) {
                baseFrequencyY = adjustFrequency(baseFrequencyY, tile.height);
            }

            stitchInfo.width = (int) (tile.width * baseFrequencyX + 0.5f);
            stitchInfo.wrapX = (int) (tile.x * baseFrequencyX + PerlinN + stitchInfo.width);

            stitchInfo.height = (int) (tile.height * baseFrequencyY + 0.5f);
            stitchInfo.wrapY = (int) (tile.y * baseFrequencyY + PerlinN + stitchInfo.height);
        }

        final double[] fSum = fractalSum
                ? new double[] {127.5, 127.5, 127.5, 127.5}
                : new double[] {0, 0, 0, 0};

        double vec0 = pointX * baseFrequencyX;
        double vec1 = pointY * baseFrequencyY;

        double ratio = fractalSum ? 127.5 : 255;

        for (int nOctave = 0; nOctave < numOctaves; nOctave++) {
            noise2(turbulenceChannels, vec0, vec1, stitchInfo);
            if (fractalSum) {
                for (int i = 0; i < turbulenceChannels.length; i++) {
                    fSum[i] += turbulenceChannels[i] * ratio;
                }
            } else {
                for (int i = 0; i < turbulenceChannels.length; i++) {
                    fSum[i] += Math.abs(turbulenceChannels[i]) * ratio;
                }
            }
            vec0 *= 2;
            vec1 *= 2;
            ratio *= 0.5;
            if (stitchInfo != null) {
                // Update stitch values. Subtracting PerlinN before the multiplication and
                // adding it afterward simplifies to subtracting it once.
                stitchInfo.width *= 2;
                stitchInfo.wrapX *= 2;
                stitchInfo.wrapX += (int) PerlinN;

                stitchInfo.height *= 2;
                stitchInfo.wrapY *= 2;
                stitchInfo.wrapY += (int) PerlinN;
            }
        }

        System.arraycopy(fSum, 0, turbulenceChannels, 0, fSum.length);
    }

    private double adjustFrequency(double frequency, double tileSize) {
        double fLoFreq = Math.floor(tileSize * frequency) / tileSize;
        double fHiFreq = Math.ceil(tileSize * frequency) / tileSize;
        if (frequency / fLoFreq < fHiFreq / frequency) {
            return fLoFreq;
        } else {
            return fHiFreq;
        }
    }
}
