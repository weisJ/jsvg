/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
import java.util.Objects;
import java.util.function.Supplier;

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.util.supplier.ConstantSupplier;
import com.github.weisj.jsvg.util.supplier.LazySupplier;

public final class ChannelStorage<T> {
    private final @NotNull Map<@NotNull Object, @NotNull Supplier<T>> storage = new HashMap<>();
    private final @NotNull Map<@NotNull Object, @NotNull FilterChannelKey> aliases = new HashMap<>();

    public void addResult(@NotNull FilterChannelKey key, @NotNull T value) {
        storage.put(key.key(), new ConstantSupplier<>(value));
    }

    public void addResult(@NotNull FilterChannelKey key, @NotNull Supplier<T> value) {
        storage.put(key.key(), new LazySupplier<>(value));
    }

    public void addAlias(@NotNull FilterChannelKey alias, @NotNull FilterChannelKey key) {
        // Capture the output, so reassigning a name or LastResult cannot redirect existing aliases.
        aliases.put(alias.key(), resolveKey(key));
    }

    public @NotNull FilterChannelKey resolveKey(@NotNull FilterChannelKey key) {
        return aliases.getOrDefault(key.key(), key);
    }

    public @NotNull T get(@NotNull FilterChannelKey key) {
        Supplier<T> provider = storage.get(resolveKey(key).key());

        // Fall back to last result if channel is not found.
        // E.g. this might be the case if the filter primtive is invalid.
        if (provider == null) provider = storage.get(DefaultFilterChannel.LastResult.key());
        Objects.requireNonNull(provider, "Channel " + key + " not found.");
        return provider.get();
    }
}
