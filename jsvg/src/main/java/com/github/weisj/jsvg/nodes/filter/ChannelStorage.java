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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.util.ConstantProvider;
import com.github.weisj.jsvg.util.LazyProvider;
import com.github.weisj.jsvg.util.Provider;

public final class ChannelStorage<T> {
    private final @NotNull Map<@NotNull String, @NotNull Provider<T>> storage = new HashMap<>();

    public void addResult(@NotNull Object key, @NotNull T value) {
        storage.put(key.toString(), new ConstantProvider<>(value));
    }

    public void addResult(@NotNull DefaultFilterChannel key, @NotNull Supplier<T> value) {
        storage.put(key.toString(), new LazyProvider<>(value));
    }

    public void addResult(@NotNull DefaultFilterChannel key, @NotNull T value) {
        storage.put(key.toString(), new ConstantProvider<>(value));
    }

    public @NotNull T get(@NotNull Object key) {
        Provider<T> provider = storage.get(key.toString());
        if (provider == null) throw new IllegalFilterStateException();
        return provider.get();
    }
}
