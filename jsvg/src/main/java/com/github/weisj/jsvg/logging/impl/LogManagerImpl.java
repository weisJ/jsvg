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
package com.github.weisj.jsvg.logging.impl;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.logging.LogManager;
import com.github.weisj.jsvg.logging.Logger;

import aQute.bnd.annotation.spi.ServiceConsumer;

@ServiceConsumer(value = LogManager.class)
public final class LogManagerImpl {
    private static final @NotNull LogManager logManager;

    private LogManagerImpl() {}

    static {
        Iterator<LogManager> managers = ServiceLoader.load(LogManager.class).iterator();
        if (managers.hasNext()) {
            logManager = managers.next();
        } else {
            logManager = new DefaultLogManager();
        }
    }

    public static @NotNull LogManager logManager() {
        return logManager;
    }

    private static class DefaultLogManager implements LogManager {
        @Override
        public @NotNull Logger createLogger(@NotNull String name) {
            return new JavaUtilLoggerWrapper(java.util.logging.Logger.getLogger(name));
        }
    }

    private static final class JavaUtilLoggerWrapper implements Logger {
        private final @NotNull java.util.logging.Logger logger;

        private JavaUtilLoggerWrapper(@NotNull java.util.logging.Logger logger) {
            this.logger = logger;
        }

        @Override
        public void log(Level level, @Nullable String message) {
            logger.log(new WrapperLogRecord(toJavaUtilLevel(level), message));
        }

        @Override
        public void log(Level level, @Nullable String message, @NotNull Throwable e) {
            WrapperLogRecord logRecord = new WrapperLogRecord(toJavaUtilLevel(level), message);
            logRecord.setThrown(e);
            logger.log(logRecord);
        }

        @Override
        public void log(Level level, @NotNull Supplier<@Nullable String> messageSupplier) {
            java.util.logging.Level javaUtilLevel = toJavaUtilLevel(level);
            if (!logger.isLoggable(javaUtilLevel)) {
                return;
            }
            logger.log(new WrapperLogRecord(javaUtilLevel, messageSupplier.get()));
        }

        static java.util.logging.Level toJavaUtilLevel(@NotNull Logger.Level level) {
            switch (level) {
                case DEBUG:
                    return java.util.logging.Level.FINE;
                case INFO:
                    return java.util.logging.Level.INFO;
                case WARNING:
                    return java.util.logging.Level.WARNING;
                case ERROR:
                    return java.util.logging.Level.SEVERE;
                default:
                    throw new IllegalArgumentException("Unknown log level: " + level);
            }
        }
    }

    private static final class WrapperLogRecord extends java.util.logging.LogRecord {
        private static final String DISPATCHER_CLASS_NAME = JavaUtilLoggerWrapper.class.getName();
        private boolean needToInferCaller = true;

        public WrapperLogRecord(Level level, String msg) {
            super(level, msg);
        }

        @Override
        public String getSourceClassName() {
            if (needToInferCaller) {
                customInferCaller();
            }
            return super.getSourceClassName();
        }

        @Override
        public String getSourceMethodName() {
            if (needToInferCaller) {
                customInferCaller();
            }
            return super.getSourceMethodName();
        }

        private void customInferCaller() {
            needToInferCaller = false;
            new CallerFinder(DISPATCHER_CLASS_NAME)
                    .findCaller()
                    .ifPresent(st -> {
                        setSourceClassName(st.getClassName());
                        setSourceMethodName(st.getMethodName());
                    });
        }
    }
}
