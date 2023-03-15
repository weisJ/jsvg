/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.parser.AttributeNode;

public class FilterPrimitiveBase {

    final @NotNull Length x;
    final @NotNull Length y;
    final @NotNull Length width;
    final @NotNull Length height;

    private final @NotNull Object inputChannel;
    private final @NotNull Object resultChannel;

    public FilterPrimitiveBase(@NotNull AttributeNode attributeNode) {
        x = attributeNode.getLength("x", Unit.PERCENTAGE.valueOf(0));
        y = attributeNode.getLength("y", Unit.PERCENTAGE.valueOf(0));
        width = attributeNode.getLength("width", Unit.PERCENTAGE.valueOf(100));
        height = attributeNode.getLength("height", Unit.PERCENTAGE.valueOf(100));

        Object in = attributeNode.getValue("in");
        if (in == null) in = DefaultFilterChannel.LastResult;
        inputChannel = in;

        Object result = attributeNode.getValue("result");
        if (result == null) result = DefaultFilterChannel.LastResult;
        resultChannel = result;
    }

    protected @NotNull Channel channel(@NotNull Object channelName, @NotNull FilterContext context) {
        Channel input = context.getChannel(channelName);
        if (input == null) throw new IllegalStateException("Input channel [" + channelName + "] doesn't exist.");
        return input;
    }

    protected @NotNull Channel inputChannel(@NotNull FilterContext context) {
        return channel(inputChannel, context);
    }

    protected void saveResult(@NotNull Channel output, @NotNull FilterContext filterContext) {
        filterContext.addResult(resultChannel, output);
        if (resultChannel != DefaultFilterChannel.LastResult) {
            filterContext.addResult(DefaultFilterChannel.LastResult, output);
        }
    }
}
