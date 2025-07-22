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
package com.github.weisj.jsvg.logging.systemlogger;

import java.util.ResourceBundle;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.logging.LogManager;
import com.github.weisj.jsvg.logging.Logger;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(LogManager.class)
public final class SystemLoggerLogManager implements LogManager {

    @Override
    public @NotNull Logger createLogger(@NotNull String name) {
        return new SystemLoggerWrapper(System.getLogger(name));
    }

    private static final class SystemLoggerWrapper extends SystemLoggerStub implements Logger {
        private final @NotNull System.Logger logger;

        private SystemLoggerWrapper(@NotNull System.Logger logger) {
            this.logger = logger;
        }

        private @NotNull System.Logger.Level toSystemLevel(@NotNull Logger.Level level) {
            switch (level) {
                case DEBUG:
                    return System.Logger.Level.DEBUG;
                case INFO:
                    return System.Logger.Level.INFO;
                case WARNING:
                    return System.Logger.Level.WARNING;
                case ERROR:
                    return System.Logger.Level.ERROR;
                default:
                    throw new IllegalArgumentException("Unknown log level: " + level);
            }
        }

        @Override
        public void log(Logger.Level level, @Nullable String message) {
            logger.log(toSystemLevel(level), message);
        }

        @Override
        public void log(Logger.Level level, @Nullable String message, @NotNull Throwable e) {
            logger.log(toSystemLevel(level), message, e);
        }

        @Override
        public void log(Logger.Level level, @NotNull Supplier<@Nullable String> messageSupplier) {
            logger.log(toSystemLevel(level), messageSupplier);
        }
    }

    // Inheriting from this makes the default JUL implementation skip our logger from the stacktrace.
    private static class SystemLoggerStub implements System.Logger {

        @Override
        public String getName() {
            return "";
        }

        @Override
        public boolean isLoggable(Level level) {
            return false;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            // Do nothing
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            // Do nothing
        }
    }
}
