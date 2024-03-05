/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jannis Weis
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

/*
 * $Id: BlendComposite.java,v 1.9 2007/02/28 01:21:29 gfx Exp $
 *
 * Dual-licensed under LGPL (Sun and Romain Guy) and BSD (Romain Guy).
 *
 * Copyright 2005 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A.
 * All rights reserved.
 *
 * Copyright (c) 2006 Romain Guy <romain.guy@mac.com> All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source code must retain the
 * above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions
 * in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.*;
import java.awt.image.*;

import org.jetbrains.annotations.NotNull;

/**
 * <p>A blend composite defines the rule according to which a drawing primitive
 * (known as the source) is mixed with existing graphics (know as the
 * destination.)</p>
 * <p><code>BlendComposite</code> is an implementation of the
 * {@link java.awt.Composite} interface and must therefore be set as a state on
 * a {@link java.awt.Graphics2D} surface.</p>
 * <p>Please refer to {@link java.awt.Graphics2D#setComposite(java.awt.Composite)}
 * for more information on how to use this class with a graphics surface.</p>
 * <h2>Blending Modes</h2>
 * <p>This class offers a certain number of blending modes, or compositing
 * rules. These rules are inspired from graphics editing software packages,
 * like <em>Adobe Photoshop</em> or <em>The GIMP</em>.</p>
 * <p>Given the wide variety of implemented blending modes and the difficulty
 * to describe them with words, please refer to those tools to visually see
 * the result of these blending modes.</p>
 *
 * @see java.awt.Graphics2D
 * @see java.awt.Composite
 * @see java.awt.AlphaComposite
 * @author Romain Guy <romain.guy@mac.com>
 */
public abstract class AbstractBlendComposite implements Composite {

    protected abstract @NotNull Blender blender();

    private static boolean isColorModelInvalid(ColorModel cm) {
        if (cm instanceof DirectColorModel && cm.getTransferType() == DataBuffer.TYPE_INT) {
            DirectColorModel directCM = (DirectColorModel) cm;

            return !(directCM.getRedMask() == 0x00FF0000
                    && directCM.getGreenMask() == 0x0000FF00
                    && directCM.getBlueMask() == 0x000000FF
                    && (directCM.getNumComponents() != 4 || directCM.getAlphaMask() == 0xFF000000));
        }
        return true;
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        if (isColorModelInvalid(srcColorModel) || isColorModelInvalid(dstColorModel)) {
            throw new RasterFormatException("Incompatible color models");
        }
        return new BlendingContext(blender());
    }

    private static final class BlendingContext implements CompositeContext {
        private final @NotNull Blender blender;

        private BlendingContext(@NotNull Blender blender) {
            this.blender = blender;
        }

        @Override
        public void dispose() {}

        @Override
        public void compose(@NotNull Raster src, @NotNull Raster dstIn, @NotNull WritableRaster dstOut) {
            int width = Math.min(src.getWidth(), dstIn.getWidth());
            int height = Math.min(src.getHeight(), dstIn.getHeight());

            int[] result = new int[4];
            int[] srcPixel = new int[4];
            int[] dstPixel = new int[4];
            int[] srcPixels = new int[width];
            int[] dstPixels = new int[width];

            for (int y = 0; y < height; y++) {
                src.getDataElements(0, y, width, 1, srcPixels);
                dstIn.getDataElements(0, y, width, 1, dstPixels);

                for (int x = 0; x < width; x++) {
                    // pixels are stored as INT_ARGB
                    // our arrays are [R, G, B, A]
                    int pixel = srcPixels[x];
                    srcPixel[0] = (pixel >> 16) & 0xFF;
                    srcPixel[1] = (pixel >> 8) & 0xFF;
                    srcPixel[2] = pixel & 0xFF;
                    srcPixel[3] = (pixel >> 24) & 0xFF;

                    pixel = dstPixels[x];
                    dstPixel[0] = (pixel >> 16) & 0xFF;
                    dstPixel[1] = (pixel >> 8) & 0xFF;
                    dstPixel[2] = pixel & 0xFF;
                    dstPixel[3] = (pixel >> 24) & 0xFF;

                    blender.blend(srcPixel, dstPixel, result);

                    dstPixels[x] = ((result[3] & 0xFF) << 24)
                            | ((result[0] & 0xFF) << 16)
                            | ((result[1] & 0xFF) << 8)
                            | (result[2] & 0xFF);
                }
                dstOut.setDataElements(0, y, width, 1, dstPixels);
            }
        }
    }

    @FunctionalInterface
    public interface Blender {
        void blend(int[] src, int[] dst, int[] result);
    }
}
