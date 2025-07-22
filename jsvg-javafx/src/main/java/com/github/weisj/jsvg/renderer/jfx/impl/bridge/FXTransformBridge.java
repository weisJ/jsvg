/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.impl.bridge;

import java.awt.geom.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

import org.jetbrains.annotations.NotNull;

/**
 * The bridge between JavaFX and AWT.
 */
public class FXTransformBridge {

    public static void setTransform(@NotNull GraphicsContext ctx, @NotNull AffineTransform awtTransform) {
        ctx.setTransform(awtTransform.getScaleX(), awtTransform.getShearY(), awtTransform.getShearX(),
                awtTransform.getScaleY(), awtTransform.getTranslateX(), awtTransform.getTranslateY());
    }

    public static void applyTransform(@NotNull GraphicsContext ctx, @NotNull AffineTransform awtTransform) {
        ctx.transform(awtTransform.getScaleX(), awtTransform.getShearY(), awtTransform.getShearX(),
                awtTransform.getScaleY(), awtTransform.getTranslateX(), awtTransform.getTranslateY());
    }

    public static Affine convertAffineTransform(AffineTransform awtTransform) {
        return new Affine(awtTransform.getScaleX(), awtTransform.getShearY(), awtTransform.getShearX(),
                awtTransform.getScaleY(), awtTransform.getTranslateX(), awtTransform.getTranslateY());
    }

    public static AffineTransform convertAffine(Affine jfxAffine) {
        return new AffineTransform(jfxAffine.getMxx(), jfxAffine.getMyx(), jfxAffine.getMxy(), jfxAffine.getMyy(),
                jfxAffine.getTx(), jfxAffine.getTy());
    }

}
