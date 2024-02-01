/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jannis Weis
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

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.ImageObserver;
import java.util.concurrent.CountDownLatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.renderer.awt.PlatformSupport;

class UIFutureTest {

    @Test
    void testValueUiFuture() {
        Object o = new Object();
        UIFuture<Object> future = new ValueUIFuture<>(o);
        assertTrue(future.checkIfReady(null));
        assertEquals(o, future.get());
    }

    @Test
    void testSwingUIFuture() {
        Object o = new Object();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch repaintLatch = new CountDownLatch(1);
        PlatformSupport platformSupport = new PlatformSupport() {
            @Override
            public @Nullable ImageObserver imageObserver() {
                return null;
            }

            @Override
            public @NotNull TargetSurface targetSurface() {
                return repaintLatch::countDown;
            }
        };

        UIFuture<Object> future = new SwingUIFuture<>(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                return e;
            }
            return o;
        });
        assertFalse(future.checkIfReady(platformSupport));
        startLatch.countDown();
        assertDoesNotThrow(() -> repaintLatch.await());
        assertTrue(future.checkIfReady(platformSupport));
        assertEquals(o, future.get());
    }
}
