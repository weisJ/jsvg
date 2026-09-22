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
package com.github.weisj.jsvg.renderer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.ViewBox;

class RenderConfigTest {

    @Test
    void defaults() {
        RenderConfig config = RenderConfig.builder().build();

        assertSame(NullPlatformSupport.INSTANCE, config.platformSupport());
        assertNull(config.fontLoader());
        assertEquals(SVGFont.defaultFontSize(), config.fontSize());
        assertEquals(SVGFont.defaultFontFamily(), config.defaultFontFamily());
        assertNull(config.viewBox());
        assertSame(AnimationState.NO_ANIMATION, config.animationState());
    }

    @Test
    void configuredValues() {
        FontLoader fontLoader = (family, attributes) -> null;
        ViewBox viewBox = new ViewBox(1, 2, 3, 4);
        AnimationState animationState = new AnimationState(2, 10);

        RenderConfig config = RenderConfig.builder()
                .platformSupport(NullPlatformSupport.INSTANCE)
                .fontLoader(fontLoader)
                .fontSize(24f)
                .defaultFontFamily("Custom")
                .viewBox(viewBox)
                .animationState(animationState)
                .build();

        assertSame(fontLoader, config.fontLoader());
        assertEquals(24f, config.fontSize());
        assertEquals("Custom", config.defaultFontFamily());
        assertSame(viewBox, config.viewBox());
        assertSame(animationState, config.animationState());
    }

    @Test
    void nullableValuesUseDefaults() {
        RenderConfig config = RenderConfig.builder()
                .fontSize(null)
                .defaultFontFamily(null)
                .animationState(null)
                .build();

        assertEquals(SVGFont.defaultFontSize(), config.fontSize());
        assertEquals(SVGFont.defaultFontFamily(), config.defaultFontFamily());
        assertSame(AnimationState.NO_ANIMATION, config.animationState());
    }
}
