/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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
package com.github.weisj.jsvg.util;

import java.awt.geom.Path2D;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.value.ConstantValue;
import com.github.weisj.jsvg.geometry.FillRuleAwareAWTSVGShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.path.BuildHistory;
import com.github.weisj.jsvg.geometry.path.PathCommand;
import com.github.weisj.jsvg.geometry.path.PathParser;

public final class PathUtil {

    private static final @Nullable MethodHandle trimPathHandle = lookupTrimPathMethod();

    // Only available in Java 10 or later
    private static @Nullable MethodHandle lookupTrimPathMethod() {
        try {
            MethodType methodType = MethodType.methodType(void.class);
            return MethodHandles.lookup().findVirtual(Path2D.class, "trimToSize", methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    private PathUtil() {}

    public static @NotNull SVGShape parseFromPathData(@NotNull String data, FillRule fillRule) {
        PathCommand[] pathCommands = new PathParser(data).parsePathCommand();

        int nodeCount = 2;
        for (PathCommand pathCommand : pathCommands) {
            nodeCount += pathCommand.nodeCount() - 1;
        }

        Path2D path = new Path2D.Float(fillRule.awtWindingRule, nodeCount);
        BuildHistory hist = new BuildHistory();

        for (PathCommand pathCommand : pathCommands) {
            pathCommand.appendPath(path, hist);
        }

        trimPathToSize(path);

        return new FillRuleAwareAWTSVGShape(new ConstantValue<>(path));
    }

    public static @NotNull Path2D setPolyLine(@Nullable Path2D path, float @NotNull [] points, boolean closed) {
        Path2D p;
        if (path == null) {
            p = new Path2D.Float(Path2D.WIND_EVEN_ODD, points.length / 2);
        } else {
            p = path;
            p.reset();
        }

        p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length - 1; i += 2) {
            p.lineTo(points[i], points[i + 1]);
        }
        if (closed) p.closePath();
        return p;
    }

    public static void trimPathToSize(@NotNull Path2D path) {
        if (trimPathHandle != null) {
            try {
                trimPathHandle.invokeExact(path);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
