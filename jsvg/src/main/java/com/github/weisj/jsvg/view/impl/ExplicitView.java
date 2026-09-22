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

import java.awt.geom.AffineTransform;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.value.ConstantTransform;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.PreserveAspectRatio;
import com.github.weisj.jsvg.view.ViewBox;

public final class ExplicitView implements ViewImpl {
    private final @NotNull ResolvedView view;

    public ExplicitView(@NotNull ViewBox viewBox, @NotNull PreserveAspectRatio preserveAspectRatio) {
        view = new ResolvedView(viewBox, ViewImpl.toInternal(preserveAspectRatio));
    }

    public ExplicitView(@NotNull ViewBox viewBox, @NotNull PreserveAspectRatio preserveAspectRatio,
            @NotNull AffineTransform transform) {
        view = new ResolvedView(
                viewBox, ViewImpl.toInternal(preserveAspectRatio),
                new ConstantTransform(new AffineTransform(transform)));
    }

    public ExplicitView(@NotNull AffineTransform transform) {
        view = new ResolvedView(null, null, new ConstantTransform(new AffineTransform(transform)));
    }

    @Override
    public @NotNull ResolvedView resolve(@NotNull Map<String, com.github.weisj.jsvg.nodes.View> views,
            @NotNull FloatSize documentSize) {
        return view;
    }
}
