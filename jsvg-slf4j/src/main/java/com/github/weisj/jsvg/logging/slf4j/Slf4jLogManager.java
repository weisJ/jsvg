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
package com.github.weisj.jsvg.logging.slf4j;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.github.weisj.jsvg.logging.LogManager;
import com.github.weisj.jsvg.logging.Logger;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(LogManager.class)
public final class Slf4jLogManager implements LogManager {

    @Override
    public @NotNull Logger createLogger(@NotNull String name) {
        return new Slf4jLoggerWrapper(LoggerFactory.getLogger(name));
    }

    private static final class Slf4jLoggerWrapper implements Logger {
        private final @NotNull org.slf4j.Logger logger;

        private Slf4jLoggerWrapper(@NotNull org.slf4j.Logger logger) {
            this.logger = logger;
        }

        private @NotNull org.slf4j.event.Level toSlf4jLevel(@NotNull Level level) {
            switch (level) {
                case DEBUG:
                    return org.slf4j.event.Level.DEBUG;
                case INFO:
                    return org.slf4j.event.Level.INFO;
                case WARNING:
                    return org.slf4j.event.Level.WARN;
                case ERROR:
                    return org.slf4j.event.Level.ERROR;
                default:
                    throw new IllegalArgumentException("Unknown log level: " + level);
            }
        }

        @Override
        public void log(Level level, @Nullable String message) {
            logger.atLevel(toSlf4jLevel(level)).log(message);
        }

        @Override
        public void log(Level level, @Nullable String message, @NotNull Throwable e) {
            logger.atLevel(toSlf4jLevel(level)).log(message, e);
        }

        @Override
        public void log(Level level, @NotNull Supplier<@Nullable String> messageSupplier) {
            logger.atLevel(toSlf4jLevel(level)).log(messageSupplier);
        }
    }
}
