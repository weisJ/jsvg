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
package com.github.weisj.jsvg.nodes.path;

import java.awt.geom.GeneralPath;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
class Quadratic extends PathCommand {

    private final float kx;
    private final float ky;
    private final float x;
    private final float y;

    @Override
    public String toString() {
        return "Q " + kx + " " + ky
                + " " + x + " " + y;
    }

    public Quadratic(boolean isRelative, float kx, float ky, float x, float y) {
        super(isRelative);
        this.kx = kx;
        this.ky = ky;
        this.x = x;
        this.y = y;
    }

    @Override
    public void appendPath(GeneralPath path, BuildHistory hist) {
        float xOff = isRelative ? hist.lastPoint.x : 0f;
        float yOff = isRelative ? hist.lastPoint.y : 0f;

        path.quadTo(kx + xOff, ky + yOff, x + xOff, y + yOff);
        hist.setLastPoint(x + xOff, y + yOff);
        hist.setLastKnot(kx + xOff, ky + yOff);
    }

    @Override
    public int getInnerNodes() {
        return 4;
    }
}
