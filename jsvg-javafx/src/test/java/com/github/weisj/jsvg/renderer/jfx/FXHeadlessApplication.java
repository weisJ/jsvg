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
package com.github.weisj.jsvg.renderer.jfx;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.stage.Stage;

// Based on: http://awhite.blogspot.com/2013/04/javafx-junit-testing.html
public class FXHeadlessApplication extends Application {

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final CountDownLatch LATCH = new CountDownLatch(1);

    private static final AtomicBoolean init = new AtomicBoolean(false);
    private static final AtomicBoolean started = new AtomicBoolean(false);

    private static final String THREAD_NAME = "JavaFX Init Thread";
    private static final int LAUNCH_TIMEOUT_SECONDS = 10;

    public static boolean checkJavaFXThread() {
        LOCK.lock();
        try {
            if (!init.get()) {
                init.set(true);

                try (ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName(THREAD_NAME);
                    return thread;
                })) {
                    executor.execute(Application::launch);

                    try {
                        if (!LATCH.await(LAUNCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            started.set(false);
                        }
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        started.set(false);
                    }
                }
            }
        } finally {
            LOCK.unlock();
        }
        return started.get();
    }

    public void launch() {
        try {
            Application.launch();
        } catch (Exception e) {
            started.set(false);
            LATCH.countDown();
        }
    }

    @Override
    public void start(final Stage stage) {
        started.set(true);
        LATCH.countDown();
    }
}
