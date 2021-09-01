/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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

import java.awt.font.GlyphMetrics;
import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

class GlyphCursor {
    float x;
    float y;
    int glyphOffset;
    final AffineTransform transform;

    Length[] xLocations;
    int xOff;

    Length[] xDeltas;
    int dxOff;

    Length[] yLocations;
    int yOff;

    Length[] yDeltas;
    int dyOff;

    float[] rotations;
    int rotOff;

    GlyphCursor(float x, float y, @NotNull AffineTransform transform) {
        this.x = x;
        this.y = y;
        this.transform = transform;
        this.glyphOffset = 0;
    }

    GlyphCursor(@NotNull GlyphCursor c) {
        this(c.x, c.y, c.transform);
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

    @NotNull
    AffineTransform advance(@NotNull MeasureContext measure) {
        if (xLocations != null && xOff < xLocations.length) {
            x = xLocations[xOff].resolveWidth(measure);
            xOff++;
        }
        if (xDeltas != null && dxOff < xDeltas.length) {
            x += xDeltas[dxOff].resolveWidth(measure);
            dxOff++;
        }

        if (yLocations != null && yOff < yLocations.length) {
            y = yLocations[yOff].resolveHeight(measure);
            yOff++;
        }
        if (yDeltas != null && dyOff < yDeltas.length) {
            y += yDeltas[dyOff].resolveHeight(measure);
            dyOff++;
        }

        transform.setToTranslation(x, y);

        if (rotations != null && rotations.length != 0) {
            float rotation = rotations[rotOff];
            rotOff = Math.min(rotations.length - 1, rotOff + 1);
            transform.rotate(Math.toRadians(rotation));
        }

        glyphOffset++;
        return transform;
    }

    void consumeGlyph(@NotNull GlyphMetrics gm, float letterSpacing) {
        // This assumes a horizontal baseline
        x += gm.getAdvanceX() + letterSpacing;
    }
}
