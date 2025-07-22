/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx.impl;

/**
 * We don't have direct access to set the clip required for the setClip() method, so we must track the number of save/restore calls.
 * Then we can pop the stack back to the correct clip.
 */
public final class FXOutputSaveCounter {

    private final FXOutput fxOutput;
    private int saveCount = 0;

    FXOutputSaveCounter(FXOutput fxOutput) {
        super();
        this.fxOutput = fxOutput;
    }

    public int save() {
        fxOutput.ctx.save();
        return saveCount++;
    }

    public void restoreTo(int count) {
        while (saveCount > count) {
            fxOutput.ctx.restore();
            saveCount--;
        }
    }

}
