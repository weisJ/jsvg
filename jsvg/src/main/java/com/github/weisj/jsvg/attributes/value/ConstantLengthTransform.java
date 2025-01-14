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
package com.github.weisj.jsvg.attributes.value;

import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

public final class ConstantLengthTransform implements TransformValue {
    public static final ConstantLengthTransform IDENTITY = new ConstantLengthTransform(Collections.emptyList());
    public static final ConstantLengthTransform INHERITED = new ConstantLengthTransform(Collections.emptyList());

    private final @NotNull List<@NotNull TransformPart> parts;

    public ConstantLengthTransform(@NotNull List<@NotNull TransformPart> parts) {
        this.parts = parts;
    }

    @Override
    public @NotNull AffineTransform get(@NotNull MeasureContext context) {
        AffineTransform transform = new AffineTransform();
        for (TransformPart part : parts) {
            part.applyToTransform(transform, context);
        }
        return transform;
    }
}
