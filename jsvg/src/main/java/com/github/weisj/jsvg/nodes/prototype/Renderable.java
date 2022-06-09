/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.nodes.prototype;

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;

public interface Renderable {

    /**
     * Indicated whether the element can only be rendered through means of instantiation e.g. being referenced in
     * a use tag. Instantiation doesn't create a new element it only controls, when an element can be rendered.
     *
     * @return true if only rendered if instantiated.
     */
    default boolean requiresInstantiation() {
        return false;
    }

    boolean isVisible(@NotNull RenderContext context);

    void render(@NotNull RenderContext context, @NotNull Graphics2D g);

    default boolean parseIsVisible(@NotNull AttributeNode node) {
        return !"none".equals(node.getValue("display"))
                && !"hidden".equals(node.getValue("visibility"))
                && !"collapse".equals(node.getValue("visibility"));
    }
}
