/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.output.Output;

public class CachedSurfaceSupplier {

    private final @NotNull BlittableImage.BufferSurfaceSupplier surfaceSupplier;
    private final @NotNull ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    public CachedSurfaceSupplier(BlittableImage.@NotNull BufferSurfaceSupplier surfaceSupplier) {
        this.surfaceSupplier = surfaceSupplier;
    }

    public boolean useCache(@NotNull Output output, @NotNull RenderContext renderContext) {
        return renderContext.platformSupport().isLongLived()
                && output.renderingHint(
                        SVGRenderingHints.KEY_CACHE_OFFSCREEN_IMAGE) != SVGRenderingHints.VALUE_NO_CACHE;
    }

    @NotNull
    public BlittableImage.BufferSurfaceSupplier surfaceSupplier(boolean useCache) {
        if (!useCache) {
            return surfaceSupplier;
        }
        return this::createBufferSurface;
    }

    public @NotNull BufferedImage createBufferSurface(@Nullable AffineTransform at, double width, double height) {
        if (at != null) {
            throw new UnsupportedOperationException("CachedSurfaceSupplier does not support transformations");
        }
        Cache c = cache.get();
        Iterator<CachedImage> it = c.images.iterator();
        while (it.hasNext()) {
            CachedImage cachedImage = it.next();
            BufferedImage img = cachedImage.image.get();
            if (img == null) {
                it.remove();
                continue;
            }
            if (!cachedImage.inUse && img.getWidth() >= width && img.getHeight() >= height) {
                cachedImage.inUse = true;
                c.lastIssuedCleaner = new ResourceCleaner(null, cachedImage::free);
                return img.getSubimage(0, 0, (int) width, (int) height);
            }
        }
        BufferedImage image = surfaceSupplier.createBufferSurface(null, width, height);
        CachedImage cachedImage = new CachedImage(image);
        c.images.add(cachedImage);
        c.lastIssuedCleaner = new ResourceCleaner(null, cachedImage::free);
        return image;
    }

    public @Nullable ResourceCleaner resourceCleaner(Object owner, boolean useCache) {
        if (!useCache) return null;
        ResourceCleaner cleaner = cache.get().lastIssuedCleaner;
        if (cleaner != null) {
            return cleaner.withOwner(owner);
        }
        return null;
    }

    public static class ResourceCleaner {
        private final @Nullable Object owner;
        private @Nullable Runnable cleaner;

        public ResourceCleaner(@Nullable Object owner, @Nullable Runnable cleaner) {
            this.owner = owner;
            this.cleaner = cleaner;
        }

        public void clean(Object owner) {
            if (this.owner == owner) {
                if (cleaner == null) {
                    throw new IllegalStateException("Resource already cleaned");
                }
                cleaner.run();
                cleaner = null;
            }
        }

        private @NotNull ResourceCleaner withOwner(Object owner) {
            return new ResourceCleaner(owner, cleaner);
        }
    }

    private static class Cache {
        private final List<CachedImage> images = new ArrayList<>();
        private @Nullable ResourceCleaner lastIssuedCleaner;
    }

    private static class CachedImage {
        private final @NotNull SoftReference<BufferedImage> image;
        private boolean inUse = true;

        private CachedImage(@NotNull BufferedImage image) {
            this.image = new SoftReference<>(image);
        }

        private void free() {
            this.inUse = false;
        }
    }
}
