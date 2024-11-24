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
package com.github.weisj.jsvg.ui;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.renderer.AnimationState;

public class AnimationPlayer {
    private static final AnimationPeriod NO_ANIMATION = new AnimationPeriod(0, 0, false);

    @FunctionalInterface
    public interface FrameAction {
        void runFrame(long elapsedTime);
    }

    private final Timer animationTimer = new Timer(1 / 60, e -> tick());
    private final @NotNull FrameAction action;
    private @NotNull AnimationPeriod animationPeriod;
    private long startTime;
    private long elapsedTime;

    public AnimationPlayer(@NotNull FrameAction action) {
        this.animationPeriod = NO_ANIMATION;
        this.action = action;

        animationTimer.setCoalesce(true);
        animationTimer.setRepeats(true);
    }

    public void setAnimationPeriod(@Nullable AnimationPeriod animationPeriod) {
        this.animationPeriod = animationPeriod != null
                ? animationPeriod
                : NO_ANIMATION;
        animationTimer.setInitialDelay((int) this.animationPeriod.startTime());
    }

    public @NotNull AnimationState animationState() {
        return new AnimationState(0, elapsedTime());
    }


    public boolean isRunning() {
        return animationTimer.isRunning();
    }

    public void start() {
        elapsedTime = 0;
        resume();
    }

    public void stop() {
        pause();
        elapsedTime = 0;

    }

    public void pause() {
        animationTimer.stop();
        elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
        action.runFrame(elapsedTime());
    }

    public void resume() {
        if (elapsedTime() >= animationPeriod.duration()) return;
        startTime = System.currentTimeMillis();
        animationTimer.start();
    }

    private long elapsedTime() {
        if (!animationTimer.isRunning()) return elapsedTime;
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) + elapsedTime;
    }

    private void tick() {
        long time = elapsedTime();
        long maxTime = animationPeriod.endTime();
        if (time >= maxTime) {
            animationTimer.stop();
            time = maxTime;
        }
        action.runFrame(time);
    }
}
