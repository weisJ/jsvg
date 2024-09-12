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
package com.github.weisj.jsvg.nodes.text;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

class GlyphCursor {
    protected final Rectangle2D completeGlyphRunBounds;
    protected @NotNull GlyphAdvancement advancement = GlyphAdvancement.defaultAdvancement();

    protected float x;
    protected float y;
    protected int glyphOffset;
    protected final AffineTransform transform;

    protected Length[] xLocations;
    protected int xOff;

    protected Length[] xDeltas;
    protected int dxOff;

    protected Length[] yLocations;
    protected int yOff;

    protected Length[] yDeltas;
    protected int dyOff;

    protected float[] rotations;
    protected int rotOff;

    GlyphCursor(float x, float y, @NotNull AffineTransform transform) {
        this(x, y, transform, new Rectangle2D.Float(Length.UNSPECIFIED_RAW, Length.UNSPECIFIED_RAW, 0, 0));
    }

    private GlyphCursor(float x, float y, @NotNull AffineTransform transform, @NotNull Rectangle2D glyphBounds) {
        this.x = x;
        this.y = y;
        this.transform = transform;
        this.completeGlyphRunBounds = glyphBounds;
        this.glyphOffset = 0;
    }

    GlyphCursor(@NotNull GlyphCursor c) {
        this(c.x, c.y, c.transform, c.completeGlyphRunBounds);
        this.advancement = c.advancement;
        this.glyphOffset = 0;
        this.xLocations = c.xLocations;
        this.xOff = c.xOff;
        this.yLocations = c.yLocations;
        this.yOff = c.yOff;
        this.xDeltas = c.xDeltas;
        this.dxOff = c.dxOff;
        this.yDeltas = c.yDeltas;
        this.dyOff = c.dyOff;
        this.rotations = c.rotations;
        this.rotOff = c.rotOff;
    }

    GlyphCursor derive() {
        return new GlyphCursor(this);
    }

    void updateFrom(GlyphCursor local) {
        x = local.x;
        y = local.y;
    }

    public void setAdvancement(@NotNull GlyphAdvancement advancement) {
        this.advancement = advancement;
    }

    public GlyphAdvancement advancement() {
        return advancement;
    }

    /*
     * Return a null value indicates that the iteration should stop.
     */
    @Nullable
    AffineTransform advance(@NotNull MeasureContext measure, @NotNull Glyph glyph) {
        x = nextX(measure);
        x += nextDeltaX(measure);

        y = nextY(measure);
        y += nextDeltaY(measure);

        transform.setToTranslation(x, y);

        double rotation = nextRotation();
        if (rotation != 0) {
            transform.rotate(rotation);
        }

        glyphOffset++;

        // Todo: Also handle non-horizontal and bidi text
        // This assumes a horizontal baseline
        x += advancement.glyphAdvancement(glyph);

        return advancement.glyphTransform(transform);
    }

    void advanceSpacing(float letterSpacing) {
        x += advancement.spacingAdvancement(letterSpacing);
    }

    private boolean currentGlyphHasXOverride() {
        return xLocations != null && xOff < xLocations.length;
    }

    protected float nextX(@NotNull MeasureContext measure) {
        if (currentGlyphHasXOverride()) {
            x = xLocations[xOff].resolveWidth(measure);
            xOff++;
        }
        return x;
    }

    private boolean currentGlyphHasDeltaXOverride() {
        return xDeltas != null && dxOff < xDeltas.length;
    }

    protected float nextDeltaX(@NotNull MeasureContext measure) {
        if (currentGlyphHasDeltaXOverride()) {
            return xDeltas[dxOff++].resolveWidth(measure);
        }
        return 0;
    }

    private boolean currentGlyphHasYOverride() {
        return yLocations != null && yOff < yLocations.length;
    }

    protected float nextY(@NotNull MeasureContext measure) {
        if (currentGlyphHasYOverride()) {
            y = yLocations[yOff].resolveHeight(measure);
            yOff++;
        }
        return y;
    }

    private boolean currentGlyphHasDeltaYOverride() {
        return yDeltas != null && dyOff < yDeltas.length;
    }

    protected float nextDeltaY(@NotNull MeasureContext measure) {
        if (currentGlyphHasDeltaYOverride()) {
            return yDeltas[dyOff++].resolveHeight(measure);
        }
        return 0;
    }

    private boolean currentGlyphHasRotationOverride() {
        return rotations != null && rotations.length != 0;
    }

    protected double nextRotation() {
        if (currentGlyphHasRotationOverride()) {
            float rotation = rotations[rotOff];
            rotOff = Math.min(rotations.length - 1, rotOff + 1);
            return Math.toRadians(rotation);
        }
        return 0;
    }

    public boolean isCurrentGlyphAutoLayout() {
        return !currentGlyphHasXOverride()
                && !currentGlyphHasDeltaXOverride()
                && !currentGlyphHasYOverride()
                && !currentGlyphHasDeltaYOverride()
                && !(currentGlyphHasRotationOverride() && rotations[rotOff] % 360 != 0);
    }
}
