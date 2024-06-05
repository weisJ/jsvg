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
package com.github.weisj.jsvg.renderer.awt;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

import org.jetbrains.annotations.NotNull;

public class AwtComponentPlatformSupport implements PlatformSupport {
    protected final @NotNull Component component;

    public AwtComponentPlatformSupport(@NotNull Component component) {
        this.component = component;
    }

    @Override
    public float fontSize() {
        Font font = component.getFont();
        if (font != null) return font.getSize2D();
        return PlatformSupport.super.fontSize();
    }

    @Override
    public @NotNull TargetSurface targetSurface() {
        return component::repaint;
    }

    @Override
    public boolean isLongLived() {
        return true;
    }

    @Override
    public @NotNull ImageObserver imageObserver() {
        return component;
    }

    @Override
    public @NotNull Image createImage(@NotNull ImageProducer imageProducer) {
        return component.createImage(imageProducer);
    }

    @Override
    public String toString() {
        return "AwtComponentSupport{" +
                "component=" + component +
                '}';
    }
}
