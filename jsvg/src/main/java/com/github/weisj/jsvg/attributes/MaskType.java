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


import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.supplier.ImmutableSupplier;

/**
 * Specifies whether the mask is treated as a luminance mask or as an alpha mask.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/mask-type">mask-type (MDN)</a>
 * @see <a href="https://drafts.fxtf.org/css-masking-1/#the-mask-type">CSS Masking Level 1 – mask-type</a>
 */
public enum MaskType {
    /**
     * The luminance values of the mask content determine the mask.
     * The effective mask value for a pixel is computed as {@code luminance(color) * alpha}.
     *
     * @see <a href="https://drafts.fxtf.org/css-masking-1/#valdef-mask-type-luminance">luminance</a>
     */
    @Default
    Luminance(() -> ImageUtil::createLuminosityBuffer),
    /**
     * The alpha values of the mask content determine the mask.
     * The effective mask value for a pixel equals its alpha value.
     *
     * @see <a href="https://drafts.fxtf.org/css-masking-1/#valdef-mask-type-alpha">alpha</a>
     */
    Alpha(() -> ImageUtil::createCompatibleTransparentImage);

    private final @NotNull ImmutableSupplier<BlittableImage.@NotNull BufferSurfaceSupplier> bufferSurfaceSupplier;

    MaskType(@NotNull ImmutableSupplier<BlittableImage.@NotNull BufferSurfaceSupplier> bufferSurfaceSupplier) {
        this.bufferSurfaceSupplier = bufferSurfaceSupplier;
    }

    public @NotNull BlittableImage.BufferSurfaceSupplier bufferSurface() {
        return bufferSurfaceSupplier.get();
    }
}
