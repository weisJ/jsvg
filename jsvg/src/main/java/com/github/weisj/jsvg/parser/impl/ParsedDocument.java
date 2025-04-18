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
package com.github.weisj.jsvg.parser.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
import com.github.weisj.jsvg.parser.DomDocument;
import com.github.weisj.jsvg.parser.LoaderContext;

public class ParsedDocument implements DomDocument {
    private final Map<String, Object> namedElements = new HashMap<>();
    private final @Nullable URI rootURI;
    private final @NotNull LoaderContext loaderContext;
    private final @NotNull LoadHelper loadHelper;
    private int currentDepth;

    private @NotNull AnimationPeriod animationPeriod = new AnimationPeriod(0, 0, false);

    public ParsedDocument(@Nullable URI rootURI, @NotNull LoaderContext loaderContext,
            @NotNull LoadHelper loadHelper) {
        this.rootURI = rootURI;
        this.loaderContext = loaderContext;
        this.loadHelper = loadHelper;
    }

    @Override
    public @NotNull LoaderContext loaderContext() {
        return loaderContext;
    }

    public @NotNull LoadHelper loadHelper() {
        return loadHelper;
    }

    @Override
    public void registerNamedElement(@NotNull String name, @Nullable Object element) {
        namedElements.put(name, element);
    }

    @ApiStatus.Internal
    int currentNestingDepth() {
        return currentDepth;
    }

    @ApiStatus.Internal
    void setCurrentNestingDepth(int depth) {
        this.currentDepth = depth;
    }

    @Override
    public <T> @Nullable T getElementById(@NotNull Class<T> type, @Nullable String id) {
        if (id == null) return null;
        Object node = namedElements.get(id);
        if (!type.equals(ParsedElement.class) && node instanceof ParsedElement) {
            node = ((ParsedElement) node).nodeEnsuringBuildStatus(currentNestingDepth());
        }
        return type.isInstance(node) ? type.cast(node) : null;
    }

    public boolean hasElementWithId(@NotNull String id) {
        return namedElements.containsKey(id);
    }

    @Override
    public @Nullable URI rootURI() {
        return rootURI;
    }

    public @NotNull AnimationPeriod animationPeriod() {
        return animationPeriod;
    }

    public void registerAnimatedElement(@NotNull BaseAnimationNode animate) {
        animationPeriod = animationPeriod.derive(animate.track());
    }
}
