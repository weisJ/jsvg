/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.view.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.view.ViewBox;

public final class ResolvedView {
    public static final ResolvedView DEFAULT = new ResolvedView(null, null);

    private final @Nullable ViewBox viewBox;
    private final @Nullable PreserveAspectRatio preserveAspectRatio;
    private final @Nullable TransformValue transformOverride;

    public ResolvedView(@Nullable ViewBox viewBox, @Nullable PreserveAspectRatio preserveAspectRatio) {
        this(viewBox, preserveAspectRatio, null);
    }

    public ResolvedView(@Nullable ViewBox viewBox, @Nullable PreserveAspectRatio preserveAspectRatio,
            @Nullable TransformValue transformOverride) {
        this.viewBox = viewBox;
        this.preserveAspectRatio = preserveAspectRatio;
        this.transformOverride = transformOverride;
    }

    public @Nullable ViewBox viewBox(@Nullable ViewBox fallback) {
        return viewBox != null ? viewBox : fallback;
    }

    public @NotNull PreserveAspectRatio preserveAspectRatio(@NotNull PreserveAspectRatio fallback) {
        return preserveAspectRatio != null ? preserveAspectRatio : fallback;
    }

    public @Nullable TransformValue transformOverride() {
        return transformOverride;
    }
}
