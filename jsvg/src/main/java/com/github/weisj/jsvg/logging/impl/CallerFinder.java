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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CallerFinder {
    private static final boolean HAS_STACK_WALKER;
    private static final MethodHandle STACK_WALKER_WALK;
    private static final MethodHandle STACK_FRAME_GET_CLASS_NAME;
    private static final MethodHandle STACK_FRAME_GET_METHOD_NAME;

    static {
        MethodHandle walk = null;
        MethodHandle getClassName = null;
        MethodHandle getMethodName = null;
        boolean hasStackWalker = false;
        try {
            Class<?> stackWalkerClass = Class.forName("java.lang.StackWalker");
            Class<?> optionClass = Class.forName("java.lang.StackWalker$Option");
            @SuppressWarnings({"rawtypes", "unchecked"}) Object retainClassRef =
                    Enum.valueOf((Class<Enum>) optionClass, "RETAIN_CLASS_REFERENCE");
            MethodHandle getInstance = MethodHandles.publicLookup().findStatic(
                    stackWalkerClass, "getInstance",
                    MethodType.methodType(stackWalkerClass, optionClass));
            Object stackWalkerInstance = getInstance.invoke(retainClassRef);
            walk = MethodHandles.lookup().findVirtual(
                    stackWalkerClass, "walk",
                    MethodType.methodType(Object.class, java.util.function.Function.class));
            walk = walk.bindTo(stackWalkerInstance);

            Class<?> stackFrameClass = Class.forName("java.lang.StackWalker$StackFrame");
            getClassName = MethodHandles.publicLookup().findVirtual(
                    stackFrameClass, "getClassName", MethodType.methodType(String.class));
            getMethodName = MethodHandles.publicLookup().findVirtual(
                    stackFrameClass, "getMethodName", MethodType.methodType(String.class));

            hasStackWalker = true;
        } catch (Throwable ignored) {
            // If any of the above fails, we will not use the stack walker.
        }
        HAS_STACK_WALKER = hasStackWalker;
        STACK_WALKER_WALK = walk;
        STACK_FRAME_GET_CLASS_NAME = getClassName;
        STACK_FRAME_GET_METHOD_NAME = getMethodName;
    }

    private final String dispatcherClassName;
    private boolean lookingForCaller = false;

    public CallerFinder(@NotNull String dispatcherClassName) {
        this.dispatcherClassName = dispatcherClassName;
    }

    @SuppressWarnings("unchecked")
    public Optional<StackTraceElement> findCaller() {
        if (HAS_STACK_WALKER) {
            try {
                return (Optional<StackTraceElement>) STACK_WALKER_WALK.invoke(
                        (Function<Stream<?>, Optional<StackTraceElement>>) stream -> stream
                                .map(f -> {
                                    try {
                                        String className = (String) STACK_FRAME_GET_CLASS_NAME.invoke(f);
                                        String methodName = (String) STACK_FRAME_GET_METHOD_NAME.invoke(f);
                                        return new StackTraceElement(className, methodName, null, -1);
                                    } catch (Throwable t) {
                                        return null;
                                    }
                                })
                                .filter(this::isCallerFrame)
                                .findFirst());
            } catch (Throwable ignored) {
                // If the stack walker fails, we will fall back to the traditional method.
            }
        }
        // Fallback for JDK 8
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement element : stack) {
            if (isCallerFrame(element)) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    private boolean isCallerFrame(@Nullable StackTraceElement frame) {
        if (frame == null) return false;
        String cname = frame.getClassName();
        if (!lookingForCaller) {
            lookingForCaller = cname.equals(dispatcherClassName);
            return false;
        }
        return true;
    }
}
