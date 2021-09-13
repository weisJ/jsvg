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
package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
class Cubic extends PathCommand {

    private final float k1x;
    private final float k1y;
    private final float k2x;
    private final float k2y;
    private final float x;
    private final float y;

    public Cubic(boolean isRelative, float k1x, float k1y, float k2x, float k2y, float x, float y) {
        super(isRelative);
        this.k1x = k1x;
        this.k1y = k1y;
        this.k2x = k2x;
        this.k2y = k2y;
        this.x = x;
        this.y = y;
    }

    @Override
    public void appendPath(Path2D path, BuildHistory hist) {
        float xOff = isRelative ? hist.lastPoint.x : 0f;
        float yOff = isRelative ? hist.lastPoint.y : 0f;

        path.curveTo(k1x + xOff, k1y + yOff,
                k2x + xOff, k2y + yOff,
                x + xOff, y + yOff);
        hist.setLastPoint(x + xOff, y + yOff);
        hist.setLastKnot(k2x + xOff, k2y + yOff);
    }

    @Override
    public int getInnerNodes() {
        return 6;
    }

    @Override
    public String toString() {
        return "C " + k1x + " " + k1y
                + " " + k2x + " " + k2y
                + " " + x + " " + y;
    }
}
