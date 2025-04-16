/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jannis Weis
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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.UIFuture;
import com.github.weisj.jsvg.renderer.PlatformSupport;

public final class SwingUIFuture<T> implements UIFuture<T> {

    private final @NotNull AtomicReference<SwingWorker<Void, Void>> swingWorker;
    private @Nullable T value;

    public SwingUIFuture(@NotNull Supplier<T> supplier) {
        swingWorker = new AtomicReference<>(new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                value = supplier.get();
                synchronized (SwingUIFuture.this) {
                    swingWorker.set(null);
                }
                return null;
            }
        });
        swingWorker.get().execute();
    }

    @Override
    public boolean checkIfReady(@NotNull PlatformSupport platformSupport) {
        SwingWorker<?, ?> worker = swingWorker.get();
        if (worker == null || worker.isDone()) return true;
        PlatformSupport.TargetSurface targetSurface = platformSupport.targetSurface();
        if (targetSurface != null) {
            synchronized (SwingUIFuture.this) {
                worker.addPropertyChangeListener(e -> {
                    if (worker.isDone()) targetSurface.repaint();
                });
            }
        }
        return false;
    }

    @Override
    public T get() {
        return value;
    }
}
