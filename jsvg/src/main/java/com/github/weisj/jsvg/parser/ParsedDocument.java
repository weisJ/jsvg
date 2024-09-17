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
package com.github.weisj.jsvg.parser;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.nodes.animation.Animate;

public class ParsedDocument {
    private final Map<String, Object> namedElements = new HashMap<>();
    private final @Nullable URI rootURI;
    private final @NotNull LoaderContext loaderContext;

    private long animationStartTime;
    private long animationEndTime;

    public ParsedDocument(@Nullable URI rootURI, @NotNull LoaderContext loaderContext) {
        this.rootURI = rootURI;
        this.loaderContext = loaderContext;
    }

    public @NotNull LoaderContext loaderContext() {
        return loaderContext;
    }

    public void registerNamedElement(@NotNull String name, @Nullable Object element) {
        namedElements.put(name, element);
    }

    public <T> @Nullable T getElementById(@NotNull Class<T> type, @Nullable String id) {
        if (id == null) return null;
        Object node = namedElements.get(id);
        if (node instanceof ParsedElement) {
            node = ((ParsedElement) node).nodeEnsuringBuildStatus();
            // Ensure we aren't holding ParsedElement longer than needed.
            namedElements.put(id, node);
        }
        return type.isInstance(node) ? type.cast(node) : null;
    }

    public boolean hasElementWithId(@NotNull String id) {
        return namedElements.containsKey(id);
    }

    public @Nullable URI rootURI() {
        return rootURI;
    }

    public @NotNull AnimationPeriod animationPeriod() {
        return new AnimationPeriod(animationStartTime, animationEndTime);
    }

    public void registerAnimatedElement(@NotNull Animate animate) {
        Track track = animate.track();
        if (track == null) return;
        long begin = track.begin().milliseconds();
        long duration = track.duration().milliseconds();
        float repeatCount = track.repeatCount();

        animationStartTime = Math.min(animationStartTime, begin);

        if (Float.isFinite(repeatCount)) {
            animationEndTime = Math.max(animationEndTime, (long) (begin + duration * repeatCount));
        } else {
            animationEndTime = Math.max(animationEndTime, begin + duration);
        }
    }
}
