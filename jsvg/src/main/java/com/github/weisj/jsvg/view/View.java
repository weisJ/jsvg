/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.view;

import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import com.github.weisj.jsvg.view.impl.ExplicitView;
import com.github.weisj.jsvg.view.impl.NamedView;

/**
 * A source view of an SVG document selected for rendering.
 * <p>
 * Instances must be created through the factory methods on this interface.
 *
 * @see <a href="https://www.w3.org/TR/SVG2/linking.html#SVGFragmentIdentifiersDefinitions">SVG 2 fragment
 *      identifiers</a>
 */
@ProviderType
public interface View {

    /** Selects the {@code <view>} element with the given id. */
    static @NotNull View named(@NotNull String name) {
        return new NamedView(name);
    }

    /** Selects an explicit source view box and aspect-ratio mapping. */
    static @NotNull View of(@NotNull ViewBox viewBox, @NotNull PreserveAspectRatio preserveAspectRatio) {
        return new ExplicitView(viewBox, preserveAspectRatio);
    }

    /**
     * Selects an explicit source view box and aspect-ratio mapping and replaces the root SVG transform.
     * The supplied transform is defensively copied.
     */
    static @NotNull View of(@NotNull ViewBox viewBox, @NotNull PreserveAspectRatio preserveAspectRatio,
            @NotNull AffineTransform transform) {
        return new ExplicitView(viewBox, preserveAspectRatio, transform);
    }

    /**
     * Selects the root view and replaces the root SVG transform. The supplied transform is defensively copied.
     */
    static @NotNull View of(@NotNull AffineTransform transform) {
        return new ExplicitView(transform);
    }
}
