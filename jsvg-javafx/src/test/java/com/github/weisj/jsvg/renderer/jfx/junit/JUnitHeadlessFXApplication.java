/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.junit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.stage.Stage;

// Based on: http://awhite.blogspot.com/2013/04/javafx-junit-testing.html
public class JUnitHeadlessFXApplication extends Application {

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final AtomicBoolean started = new AtomicBoolean();

    public static void checkJavaFXThread() {
        LOCK.lock();
        try {
            if (!started.get()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(Application::launch);
                while (!started.get()) {
                    Thread.yield();
                }
            }
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void start(final Stage stage) {
        started.set(Boolean.TRUE);
    }
}
