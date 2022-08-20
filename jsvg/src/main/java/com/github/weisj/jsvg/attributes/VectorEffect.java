/*
 * MIT License
 *
 * Copyright (c) 2022 Jannis Weis
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.GraphicsUtil;
import com.github.weisj.jsvg.renderer.RenderContext;


public enum VectorEffect implements HasMatchName {
    None(0),
    NonScalingStroke("non-scaling-stroke", 0),
    NonScalingSize("non-scaling-size", Flags.NON_SCALING_SIZE),
    NonRotation("non-rotation", Flags.NON_ROTATING),
    FixedPosition("fixed-position", Flags.FIXED_POSITION);


    private final @NotNull String matchName;
    private final int flag;

    VectorEffect(int flag) {
        this.matchName = name();
        this.flag = flag;
    }

    VectorEffect(@NotNull String matchName, int flag) {
        this.matchName = matchName;
        this.flag = flag;
    }

    public static @NotNull Set<VectorEffect> parse(@NotNull AttributeNode attributeNode) {
        @NotNull String[] vectorEffectsRaw = attributeNode.getStringList("vector-effect");
        EnumSet<VectorEffect> vectorEffects = EnumSet.noneOf(VectorEffect.class);
        for (String effect : vectorEffectsRaw) {
            vectorEffects.add(attributeNode.parser().parseEnum(effect, VectorEffect.None));
        }
        return vectorEffects;
    }

    @Override
    public @NotNull String matchName() {
        return matchName;
    }

    private static int flags(@NotNull Set<VectorEffect> effects) {
        int flag = 0;
        for (VectorEffect effect : effects) {
            flag |= effect.flag;
        }
        return flag;
    }

    public static void applyEffects(@NotNull Set<VectorEffect> effects, @NotNull Graphics2D g,
            @NotNull RenderContext context, @Nullable AffineTransform elementTransform) {
        int flags = flags(effects);
        if (flags == 0) return;

        AffineTransform baseTransform = context.rootTransform();
        AffineTransform shapeTransform = GraphicsUtil.tryCreateInverse(baseTransform);
        if (shapeTransform == null) return;

        shapeTransform.concatenate(g.getTransform());

        double x0 = elementTransform != null ? elementTransform.getTranslateX() : 0;
        double y0 = elementTransform != null ? elementTransform.getTranslateY() : 0;

        updateTransformForFlags(flags, shapeTransform, x0, y0);

        g.setTransform(baseTransform);
        g.transform(shapeTransform);
    }

    public static @NotNull Shape applyNonScalingStroke(@NotNull Graphics2D g, @NotNull RenderContext context,
            @NotNull Shape shape) {
        // For the stroke not to be scaled we have to pre-multiply the shape by the transform and then paint
        // in the non-transformed coordinate system.
        AffineTransform baseTransform = context.rootTransform();
        AffineTransform shapeTransform = GraphicsUtil.tryCreateInverse(baseTransform);
        if (shapeTransform == null) return shape;

        shapeTransform.concatenate(g.getTransform());
        g.setTransform(baseTransform);
        return shapeTransform.createTransformedShape(shape);
    }

    private static void updateTransformForFlags(int flags, @NotNull AffineTransform transform, double x0, double y0) {
        switch (flags) {
            case Flags.NON_SCALING_SIZE: {
                double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
                if (detRoot == 0) return;
                double detRootInv = 1 / detRoot;
                transform.setTransform(
                        transform.getScaleX() * detRootInv, transform.getShearY() * detRootInv,
                        transform.getShearX() * detRootInv, transform.getScaleY() * detRootInv,
                        transform.getTranslateX(),
                        transform.getTranslateY());
                break;
            }
            case Flags.NON_ROTATING: {
                double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
                transform.setTransform(
                        detRoot, 0, 0, detRoot,
                        transform.getTranslateX(),
                        transform.getTranslateY());
                break;
            }
            case Flags.NON_SCALING_SIZE | Flags.NON_ROTATING: {
                transform.setTransform(
                        1, 0, 0, 1,
                        transform.getTranslateX(),
                        transform.getTranslateY());
                break;
            }
            case Flags.FIXED_POSITION: {
                transform.setTransform(
                        transform.getScaleX(), transform.getShearY(),
                        transform.getShearX(), transform.getScaleY(),
                        x0,
                        y0);
                break;
            }
            case Flags.FIXED_POSITION | Flags.NON_SCALING_SIZE: {
                double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
                if (detRoot == 0) return;
                double detRootInv = 1 / detRoot;
                transform.setTransform(
                        transform.getScaleX() * detRootInv, transform.getShearY() * detRootInv,
                        transform.getShearX() * detRootInv, transform.getScaleY() * detRootInv,
                        x0,
                        y0);
                break;
            }
            case Flags.FIXED_POSITION | Flags.NON_ROTATING: {
                double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
                transform.setTransform(
                        detRoot, 0, 0, detRoot,
                        x0,
                        y0);
                break;
            }
            case Flags.FIXED_POSITION | Flags.NON_ROTATING | Flags.NON_SCALING_SIZE: {
                transform.setTransform(
                        1, 0, 0, 1,
                        x0,
                        y0);
                break;
            }
            default:
                break;
        }
    }

    private static class Flags {
        private static final int NON_SCALING_SIZE = 1;
        private static final int NON_ROTATING = 1 << 1;
        private static final int FIXED_POSITION = 1 << 2;
    }
}
