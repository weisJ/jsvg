/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.awt;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.renderer.PlatformSupport;

/**
 * @deprecated Use {@link AwtComponentPlatformSupport} instead.
 */
@Deprecated
public final class JComponentPlatformSupport implements PlatformSupport {
    private final @NotNull AwtComponentPlatformSupport impl;

    public JComponentPlatformSupport(@NotNull JComponent component) {
        impl = new AwtComponentPlatformSupport(component);
    }

    @Override
    public float fontSize() {
        return impl.fontSize();
    }

    @Override
    public @NotNull TargetSurface targetSurface() {
        return impl.targetSurface();
    }

    @Override
    public boolean isLongLived() {
        return impl.isLongLived();
    }

    @Override
    public @NotNull ImageObserver imageObserver() {
        return impl.imageObserver();
    }

    @Override
    public @NotNull Image createImage(@NotNull ImageProducer imageProducer) {
        return impl.createImage(imageProducer);
    }

    @Override
    public String toString() {
        return "JComponentPlatformSupport{" +
                "component=" + imageObserver() +
                '}';
    }
}
