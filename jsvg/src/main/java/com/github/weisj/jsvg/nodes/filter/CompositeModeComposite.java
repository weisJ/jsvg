/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.CompositeMode;
import com.github.weisj.jsvg.parser.AttributeNode;

public final class CompositeModeComposite {

    private final @NotNull Composite composite;

    public CompositeModeComposite(@NotNull AttributeNode attributeNode) {
        this.composite = createComposite(attributeNode);
    }

    public @NotNull Composite composite() {
        return composite;
    }

    private static @NotNull Composite createComposite(@NotNull AttributeNode attributeNode) {
        CompositeMode compositeMode = attributeNode.getEnum("operator", CompositeMode.Over);
        switch (compositeMode) {
            case Over:
                return AlphaComposite.SrcOver;
            case In:
                return AlphaComposite.SrcIn;
            case Out:
                return AlphaComposite.SrcOut;
            case Atop:
                return AlphaComposite.SrcAtop;
            case Xor:
                return AlphaComposite.Xor;
            case Lighter:
                return new LighterComposite();
            case Arithmetic:
                return new ArithmeticComposite(
                        attributeNode.getInt("k1", 0),
                        attributeNode.getInt("k2", 0),
                        attributeNode.getInt("k3", 0),
                        attributeNode.getInt("k4", 0));
            default:
                throw new IllegalStateException();
        }
    }

    private static final class ArithmeticComposite extends AbstractBlendComposite
            implements AbstractBlendComposite.Blender {

        private final int k1;
        private final int k2;
        private final int k3;
        private final int k4;

        private ArithmeticComposite(int k1, int k2, int k3, int k4) {
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.k4 = k4;
        }

        @Override
        protected @NotNull Blender blender() {
            return this;
        }

        @Override
        public void blend(int[] src, int[] dst, int[] result) {
            result[0] = k1 * src[0] * dst[0] + k2 * src[0] + k3 * dst[0] + k4;
            result[1] = k1 * src[1] * dst[1] + k2 * src[1] + k3 * dst[1] + k4;
            result[2] = k1 * src[2] * dst[2] + k2 * src[2] + k3 * dst[2] + k4;
            result[3] = k1 * src[3] * dst[3] + k2 * src[3] + k3 * dst[3] + k4;
        }
    }

    private static final class LighterComposite extends AbstractBlendComposite
            implements AbstractBlendComposite.Blender {

        @Override
        protected @NotNull Blender blender() {
            return this;
        }

        @Override
        public void blend(int[] src, int[] dst, int[] result) {
            result[0] = Math.min(255, src[0] + dst[0]);
            result[1] = Math.min(255, src[1] + dst[1]);
            result[2] = Math.min(255, src[2] + dst[2]);
            result[3] = Math.min(255, src[3] + dst[3]);
        }
    }

}
