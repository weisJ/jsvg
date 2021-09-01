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

import java.awt.geom.AffineTransform;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.MeasureContext;

class Cursor {
    AffineTransform transform = new AffineTransform();

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

    float x;
    float y;
    int glyphOffset;

    Cursor(float x, float y) {
        this.x = x;
        this.y = y;
    }

    void advance(@NotNull MeasureContext measure) {
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
    }

    Cursor createLocalCursor(@NotNull LinearTextContainer txt) {
        Cursor c = new Cursor(x, y);
        c.transform = transform;
        c.glyphOffset = 0;
        if (txt.x.length != 0) {
            c.xLocations = txt.x;
            c.xOff = 0;
        } else {
            c.xLocations = xLocations;
            c.xOff = xOff;
        }
        if (txt.y.length != 0) {
            c.yLocations = txt.y;
            c.yOff = 0;
        } else {
            c.yLocations = yLocations;
            c.yOff = yOff;
        }
        if (txt.dx.length != 0) {
            c.xDeltas = txt.dx;
            c.dyOff = 0;
        } else {
            c.xDeltas = xDeltas;
            c.dxOff = dxOff;
        }
        if (txt.dy.length != 0) {
            c.yDeltas = txt.dy;
            c.dyOff = 0;
        } else {
            c.yDeltas = yDeltas;
            c.dyOff = dyOff;
        }
        if (txt.rotate.length != 0) {
            c.rotations = txt.rotate;
            c.rotOff = 0;
        } else {
            c.rotations = rotations;
            c.rotOff = rotOff;
        }
        return c;
    }

    void restoreFromLocalCursor(@NotNull Cursor localCursor, @NotNull LinearTextContainer txt) {
        x = localCursor.x;
        y = localCursor.y;
        if (txt.x.length == 0) xOff = localCursor.xOff;
        if (txt.y.length == 0) yOff = localCursor.yOff;
        if (txt.dx.length == 0) dxOff = localCursor.dxOff;
        if (txt.dy.length == 0) dyOff = localCursor.dyOff;
        if (txt.rotate.length == 0) rotOff = localCursor.rotOff;
    }

    @Override
    public String toString() {
        return "Cursor{" +
                "x=" + x +
                ", y=" + y +
                ", glyphOffset=" + glyphOffset +
                '}';
    }
}
