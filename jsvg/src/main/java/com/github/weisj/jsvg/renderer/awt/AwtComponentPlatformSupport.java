package com.github.weisj.jsvg.renderer.awt;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

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
