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

import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.View;

public interface ViewImpl extends View {
    @NotNull
    ResolvedView resolve(@NotNull Map<String, com.github.weisj.jsvg.nodes.View> views,
            @NotNull FloatSize documentSize);

    static @NotNull PreserveAspectRatio toInternal(
            @NotNull com.github.weisj.jsvg.view.PreserveAspectRatio preserveAspectRatio) {
        Objects.requireNonNull(preserveAspectRatio);
        PreserveAspectRatio.Align align;
        switch (preserveAspectRatio.align) {
            case None:
                align = PreserveAspectRatio.Align.None;
                break;
            case xMinYMin:
                align = PreserveAspectRatio.Align.xMinYMin;
                break;
            case xMidYMin:
                align = PreserveAspectRatio.Align.xMidYMin;
                break;
            case xMaxYMin:
                align = PreserveAspectRatio.Align.xMaxYMin;
                break;
            case xMinYMid:
                align = PreserveAspectRatio.Align.xMinYMid;
                break;
            case xMidYMid:
                align = PreserveAspectRatio.Align.xMidYMid;
                break;
            case xMaxYMid:
                align = PreserveAspectRatio.Align.xMaxYMid;
                break;
            case xMinYMax:
                align = PreserveAspectRatio.Align.xMinYMax;
                break;
            case xMidYMax:
                align = PreserveAspectRatio.Align.xMidYMax;
                break;
            case xMaxYMax:
                align = PreserveAspectRatio.Align.xMaxYMax;
                break;
            default:
                throw new IllegalArgumentException("Unknown align: " + preserveAspectRatio.align);
        }
        PreserveAspectRatio.MeetOrSlice meetOrSlice;
        switch (preserveAspectRatio.meetOrSlice) {
            case Meet:
                meetOrSlice = PreserveAspectRatio.MeetOrSlice.Meet;
                break;
            case Slice:
                meetOrSlice = PreserveAspectRatio.MeetOrSlice.Slice;
                break;
            default:
                throw new IllegalArgumentException("Unknown meetOrSlice: " + preserveAspectRatio.meetOrSlice);
        }
        return PreserveAspectRatio.of(align, meetOrSlice);
    }
}
