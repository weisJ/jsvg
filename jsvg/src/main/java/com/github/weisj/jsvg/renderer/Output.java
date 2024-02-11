/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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
package com.github.weisj.jsvg.renderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.util.Provider;

public interface Output {

    void fillShape(@NotNull Shape shape);

    void drawShape(@NotNull Shape shape);

    void drawImage(@NotNull BufferedImage image);

    void drawImage(@NotNull Image image, @Nullable ImageObserver observer);

    void drawImage(@NotNull Image image, @NotNull AffineTransform at, @Nullable ImageObserver observer);

    void setPaint(@NotNull Paint paint);

    /**
     * Set the paint used for the output. Use this version if computing the paint is expensive.
     * Outputs which don't support paints can avoid the computation.
     *
     * @param paintProvider The paint provider.
     */
    void setPaint(@NotNull Provider<Paint> paintProvider);

    void setStroke(@NotNull Stroke stroke);

    @NotNull
    Stroke stroke();

    void applyClip(@NotNull Shape clipShape);

    void setClip(@Nullable Shape shape);

    Optional<Float> contextFontSize();

    @NotNull
    Output createChild();

    void dispose();

    void debugPaint(@NotNull Consumer<Graphics2D> painter);

    @NotNull
    Rectangle2D clipBounds();

    @Nullable
    RenderingHints renderingHints();

    @Nullable
    Object renderingHint(@NotNull RenderingHints.Key key);

    void setRenderingHint(@NotNull RenderingHints.Key key, @Nullable Object value);

    @NotNull
    AffineTransform transform();

    void setTransform(@NotNull AffineTransform affineTransform);

    void applyTransform(@NotNull AffineTransform transform);

    void rotate(double angle);

    void scale(double sx, double sy);

    void translate(double dx, double dy);

    void applyOpacity(float opacity);

    @NotNull
    SafeState safeState();

    boolean supportsFilters();

    boolean supportsColors();

    default boolean isSoftClippingEnabled() {
        return renderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING) == SVGRenderingHints.VALUE_SOFT_CLIPPING_ON;
    }

    interface SafeState {
        void restore();
    }
}
